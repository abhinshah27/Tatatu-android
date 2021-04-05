package rs.highlande.app.tatatu.feature.account.profile.repository

import android.os.Bundle
import io.reactivex.Observable
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.core.util.JsonHelper
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.core.util.PreferenceHelper
import rs.highlande.app.tatatu.feature.account.profile.api.ProfileApi
import rs.highlande.app.tatatu.feature.commonRepository.CreatePostRepository
import rs.highlande.app.tatatu.model.*
import kotlin.random.Random

class ProfileRepository : BaseRepository(), CreatePostRepository, OnServerMessageReceivedListener {


    // TODO: 2019-07-09    REMOVE -> TEST
    private val testCachedPosts = mutableListOf<Post>()
    private val profileApi: ProfileApi by inject()
    private val sharedPreferences: PreferenceHelper by inject()


    fun fetchUserProfile(userId: String): Observable<User> {
        return Observable.create { emitter ->
            val profile = User.get(getMockProfile(userId) ?: JSONObject())
            if (profile == null) emitter.onError(Throwable(IllegalStateException("No user for provided ID")))
            else {
                emitter.onNext(profile)
                emitter.onComplete()
            }
        }
    }

    fun fetchUserPosts(userId: String, page: Int): Observable<MutableList<ProfileListItem>> {
        return Observable.create { emitter ->

            val arr = getDynamicPosts(userId)

            if (arr.length() == 0) {
                emitter.onNext(mutableListOf())
            } else {
                val startIndex = page * PAGINATION_SIZE
                val endIndex = startIndex + PAGINATION_SIZE

                val end = if (endIndex > arr.length()) arr.length() else endIndex
                val list = mutableListOf<ProfileListItem>()

                // TODO: 2019-07-09    REMOVE -> TEST
                if (startIndex == 0) list.addAll(testCachedPosts)

                for (it in startIndex until end) list.add(Post.get(arr.optJSONObject(it)) ?: Post())
                emitter.onNext(list)
            }
            emitter.onComplete()
        }
    }

    fun savePost(post: Post) {
        savePost(post) {
            testCachedPosts.add(0, it)
        }
    }

    private fun getMockProfile(userId: String): JSONObject? = mockProfileModelsJson[userId]


    private val mockProfileModelsJson = mapOf("0" to JSONObject().put("uid", "0").put("username", "usernametest1").put("name", "Test None").put("picture", "https://avatarfiles.alphacoders.com/812/81222.jpg").put("verified", true).put("isPublic", true).put("accountType", "PublicFigure").put("followersCount", 123).put("followingCount", 32121).put("donatingUid", "0").put("donatingInfo", "donation1\ndonation donation\ntest donation").put("details", JSONObject().put("bio", "this is my description").put("website", "www.example.com")).put("friendsCount", 54).put("balance", 213123.00).put("postsCount", 139).put("relationship", "none"), "1" to JSONObject().put("uid", "1").put("username", "usernametest2").put("name", "Test Requested").put("picture", "https://avatarfiles.alphacoders.com/812/81222.jpg").put("verified", true).put("isPublic", true).put("accountType", "Normal").put("followersCount", 123).put("followingCount", 32121).put("donatingUid", "1").put("donatingInfo", "donation1\ndonation donation\ntest donation").put("details", JSONObject().put("bio", "this is my description").put("website", "www.example.com")).put("postsCount", 15).put("relationship", "requested"), "2" to JSONObject().put("uid", "2").put("username", "usernametest3").put("name", "Test Follower").put("picture", "https://avatarfiles.alphacoders.com/812/81222.jpg").put("verified", true).put("isPublic", true).put("accountType", "Normal").put("followersCount", 123).put("followingCount", 32121).put("donatingUid", "2").put("donatingInfo", "donation1\ndonation donation\ntest donation").put("details", JSONObject().put("bio", "this is my description").put("website", "www.example.com")).put("postsCount", 15).put("relationship", "follower"), "3" to JSONObject().put("uid", "3").put("username", "usernametest5").put("name", "Test Follow Each Other").put("picture", "https://avatarfiles.alphacoders.com/812/81222.jpg").put("verified", true).put("isPublic", true).put("accountType", "Normal").put("followersCount", 123).put("followingCount", 32121).put("donatingUid", "3").put("donatingInfo", "donation1\ndonation donation\ntest donation").put("details", JSONObject().put("bio", "this is my description").put("website", "www.example.com")).put("friendsCount", 54).put("balance", 213123.00).put("postsCount", 15).put("relationship", "friends"), "4" to JSONObject().put("uid", "4").put("username", "usernametest1").put("name", "Test Following").put("picture", "https://avatarfiles.alphacoders.com/812/81222.jpg").put("verified", false).put("isPublic", true).put("accountType", "Normal").put("followersCount", 123).put("followingCount", 32121).put("details", JSONObject().put("bio", "this is my description")).put("postsCount", 15).put("relationship", "following"), "5" to JSONObject().put("uid", "5").put("username", "usernametest4").put("name", "Test None").put("picture", "https://avatarfiles.alphacoders.com/812/81222.jpg").put("verified", true).put("isPublic", true).put("accountType", "Celebrity").put("followersCount", 123).put("followingCount", 32121).put("donatingUid", "5").put("donatingInfo", "donation1\ndonation donation\ntest donation").put("details", JSONObject().put("bio", "this is my description").put("website", "www.example.com")).put("postsCount", 15).put("relationship", "following")

    )


    val types = arrayOf(PostType.IMAGE, PostType.VIDEO, PostType.MULTIMEDIA)
    val mediaTypes = arrayOf(MediaType.IMAGE, MediaType.VIDEO)
    val medias = arrayOf("https://images.pexels.com/photos/1232433/pexels-photo-1232433.jpeg", "https://images.pexels.com/photos/1235706/pexels-photo-1235706.jpeg", "https://images.pexels.com/photos/2316543/pexels-photo-2316543.jpeg", "https://images.pexels.com/photos/1233528/pexels-photo-1233528.jpeg")
    private fun getDynamicPosts(userId: String): JSONArray {
        val tmp = mutableListOf<JSONObject>()

        val count = when (userId) {
            "0" -> 140
            "1" -> 0
            "3" -> 17
            else -> 30
        }

        for (it in 0 until count) {

            val p = Post().apply {
                uid = it.toString()
                date = "2019-07-08"
                userData = User().apply { uid = Random.nextInt(6).toString() }
                type = types[Random.nextInt(types.size)]
                mediaItems = mutableListOf(PostMediaItem().apply {
                    mediaEndpoint = medias[Random.nextInt(medias.size)]
                    mediaType = mediaTypes[Random.nextInt(mediaTypes.size)]
                })
            }
            tmp.add(JSONObject(JsonHelper.serializeToString(p)))
        }

        return JSONArray(tmp)
    }

    //Edit Profile
    fun getEditProfile(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return profileApi.getEditProfile(caller, bundle)
    }

    //Delete Profile
    fun getDeleteProfile(caller: OnServerMessageReceivedListener, bundle: Bundle): Observable<RequestStatus> {
        return profileApi.getDeleteProfile(caller, bundle)
    }

    fun removeUser() {
        sharedPreferences.removeUser()
    }

    fun updateCachesUser(user: User) {
        sharedPreferences.storeUser(user)
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {

    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {

    }
}