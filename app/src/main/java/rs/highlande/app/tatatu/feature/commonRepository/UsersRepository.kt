package rs.highlande.app.tatatu.feature.commonRepository

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.realm.Realm
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.core.util.PreferenceHelper
import rs.highlande.app.tatatu.core.util.RealmUtils
import rs.highlande.app.tatatu.feature.commonApi.CommonApi
import rs.highlande.app.tatatu.feature.commonApi.UsersApi
import rs.highlande.app.tatatu.model.User
import rs.highlande.app.tatatu.model.chat.Participant

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-07.
 */
class UsersRepository : BaseRepository() {

    private val sharedPreferences: PreferenceHelper by inject()
    
    private val api: UsersApi by inject()
    private val commonAPI: CommonApi by inject()

    var mUser: User? = null
    private set


    fun getMyUser(fromNetwork: Boolean = false, caller: OnServerMessageReceivedListener? = null): Observable<Pair<User?, RequestStatus?>> {
        return Observable.combineLatest(
            Observable.just(mUser ?: sharedPreferences.getUser() ?: User()),
            fetchMyUserFromNetwork(fromNetwork, caller),
            BiFunction { t1, t2 ->
                // if user is valid emit user, otherwise null
                (if (t1.isValid()) t1 else null) to t2
            }
        )
    }

    fun fetchCachedMyUserId() = sharedPreferences.getUserId()

    fun cacheUser(user: User) {
        mUser = null
        mUser = user
        sharedPreferences.storeUser(user)
    }

    fun clearCachedUser() {
        mUser = null
    }


    private fun fetchMyUserFromNetwork(fromNetwork: Boolean, caller: OnServerMessageReceivedListener?): Observable<RequestStatus> {
        return if (fromNetwork && caller != null) api.getMyUser(caller) else Observable.just(RequestStatus.ERROR)
    }

    fun fetchUser(caller: OnServerMessageReceivedListener, userID: String, userInfo: Array<CommonApi.UserInfo>) {
        commonAPI.getUser(caller, userID, userInfo)
    }

    fun manageFollowerRequest(caller: OnServerMessageReceivedListener, requestID: String, action: Int) {
        api.doManageFollowerRequest(caller, requestID, action)
    }

    fun manageNewFollowerRequest(caller: OnServerMessageReceivedListener, userID: String, date: String) {
        api.doMakeManageFollowerRequest(caller, userID, date)
    }

    fun fetchUserFollowers(caller: OnServerMessageReceivedListener,
                           user: User,
                           page: Int = 0):
            Observable<RequestStatus> = api.doGetFollowers(caller, user.uid, page * PAGINATION_SIZE)

    fun fetchUserFollowing(caller: OnServerMessageReceivedListener,
                           user: User,
                           page: Int = 0):
            Observable<RequestStatus> = api.doGetFollowing(caller, user.uid, page * PAGINATION_SIZE)

    fun fetchUserFollowRequests(caller: OnServerMessageReceivedListener,
                                type: Int,
                                page: Int = 0):
            Observable<RequestStatus> = api.doGetFollowRequest(caller, type, page * PAGINATION_SIZE)

    fun fetchUserTotalFollowRequests(caller: OnServerMessageReceivedListener,
                           type: Int):
            Observable<RequestStatus> = api.doGetTotalFollowRequests(caller, type)

    fun searchFollowingFollowers(caller: OnServerMessageReceivedListener,
                           text: String, user: User, page: Int = 0, action: Int = 0):
            Observable<RequestStatus> = api.doSearchFollowingFollowers(caller, text, user.uid,page * PAGINATION_SIZE, action = action)

    fun reportUser(caller: OnServerMessageReceivedListener, userId: String):
            Observable<RequestStatus> = api.doReportUser(caller, userId)

    fun fetchShareUserProfileDeeplink(caller: OnServerMessageReceivedListener, userId: String):
            Observable<RequestStatus> = api.doShareUserProfile(caller, userId)

    fun addRemoveBlacklist(caller: OnServerMessageReceivedListener, userId: String, action: Int):
            Observable<RequestStatus> = api.doAddRemoveBlacklist(caller, userId, action)

    fun getParticipant(realm: Realm, user: User, insertIfNotExists: Boolean = false): Participant? {
        return if (insertIfNotExists) {
            (RealmUtils.readFirstFromRealmWithId(realm, Participant::class.java, null, user.uid) as? Participant)?.let {
                it
            }?: run {
                val participant = Participant()
                    .apply {
                    id = user.uid
                    name = user.name
                    nickname = user.username
                    avatarURL = user.picture
                    canChat = true
                    canAudiocall = true
                    canVideocall = true
                }
                realm.executeTransaction {
                    it.insertOrUpdate(participant)
                }
                participant
            }
        } else {
            RealmUtils.readFirstFromRealmWithId(realm, Participant::class.java, null, user.uid) as? Participant
        }
    }

}