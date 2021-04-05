package rs.highlande.app.tatatu.feature.account.profile.view

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.realm.RealmList
import org.json.JSONArray
import rs.highlande.app.tatatu.connection.webSocket.*
import rs.highlande.app.tatatu.core.manager.RelationshipAction
import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.core.ui.recyclerView.EndlessScrollListener
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.core.util.RealmUtils
import rs.highlande.app.tatatu.feature.account.common.BaseAccountViewModel
import rs.highlande.app.tatatu.feature.chat.repository.ChatRepository
import rs.highlande.app.tatatu.feature.commonRepository.PostRepository
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.model.Post
import rs.highlande.app.tatatu.model.Relationship
import rs.highlande.app.tatatu.model.User
import rs.highlande.app.tatatu.model.chat.ChatRoom
import rs.highlande.app.tatatu.model.chat.Participant

@SuppressLint("CheckResult")
class ProfileViewModel(usersRepository: UsersRepository,
                       relationshipManager: RelationshipManager,
                       private val postRepository: PostRepository,
                       private val chatRepository: ChatRepository): BaseAccountViewModel(usersRepository, relationshipManager) {

    private lateinit var relationship: Relationship
    lateinit var profileImage: String

    val profileActionLiveData = MutableLiveData<RelationshipAction>()
    val profileStartChatLiveData = MutableLiveData<Pair<Boolean, String>>()
    val profilePostsLiveData = MutableLiveData<MutableList<Post>>()

    var showCreateItem: Boolean = false

    var postScrollListener: EndlessScrollListener? = null

    private var postsSkip = 0
    private var isFetchingPosts = false


    fun setNewPostsScrollListener(scrollListener: EndlessScrollListener): RecyclerView.OnScrollListener {
        postScrollListener = null

        return scrollListener.let { listener ->
            postScrollListener = listener
            listener
        }
    }



    fun fetchPosts(userId: String, replaceItemsOnNextUpdate: Boolean = false) {

        if (!isFetchingPosts) {
            isFetchingPosts = true

            this.replaceItemsOnNextUpdate = replaceItemsOnNextUpdate

            if (replaceItemsOnNextUpdate) {
                postsSkip = 0
            }

            addDisposable(Observable.just(
                postRepository.getProfileTimelinePosts(
                    this,
                    postsSkip,
                    userId

                    // INFO: 2019-11-26    restores default pagination size
                    /*, limit = POST_GRID_PAGINATION_SIZE*/
                )
            )
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {},
                    { it.printStackTrace() }
                )
            )
        }
    }

    fun getUserFollowStatus(): Relationship {
        return if (::relationship.isInitialized) relationship else Relationship.NA
    }

    fun onProfileActionButtonClick() {
        when (relationship) {
            Relationship.FRIENDS -> handleChat()
            else -> profileActionLiveData.value = relationshipManager.getRelationshipAction(relationship)
        }
    }

    fun onProfileActionIconClick() {
        profileActionLiveData.value = relationshipManager.getRelationshipAction(relationship)
    }


    fun updateFollowStatus(cancelRequest: Boolean = true) {
        profileUserLiveData.value?.let {
            val obs = if (cancelRequest) {
                Observable.just(relationshipManager.manageRelationship(it, this))
            } else {
                Observable.just(relationshipManager.manageCancelOutgoingRequest(this, it))
            }
            addDisposable(obs
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {},
                    { thr -> thr.printStackTrace() }
                )
            )
        }
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        when (callCode) {
            SERVER_OP_GET_USER -> {
                addDisposable(Observable.just(response)
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {
                            User.get(it!!.optJSONObject(0))?.let {result ->
                                relationship = result.detailsInfo.relationship
                            }
                        },
                        { thr -> thr.printStackTrace() }
                    )
                )
            }
            SERVER_OP_GET_TIMELINE_FOR_PROFILE -> {
                addDisposable(Observable.just(response)
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {
                            val tmpList = mutableListOf<Post>()
                            for (i in 0 until it!!.length()) {
                                Post.get(it.optJSONObject(i))?.let { data ->
                                    tmpList.add(data)
                                }
                            }

                            postsSkip += tmpList.size
                            postScrollListener?.canFetch = (postsSkip % PAGINATION_SIZE == 0)

                            profilePostsLiveData.postValue(tmpList)

                            isFetchingPosts = false
                        },
                        { thr ->
                            thr.printStackTrace()
                            isFetchingPosts = false
                        }
                    )
                )
            }
            SERVER_OP_MANAGE_FOLLOWER_REQUEST,
            SERVER_OP_MAKE_NEW_FOLLOWER_REQUEST -> {
                addDisposable(Observable.just(response)
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {
                            profileUserLiveData.value?.let {
                                fetchUser(it.uid)
                            }
                        },
                        { thr -> thr.printStackTrace() }
                    )
                )
            }
            SERVER_OP_CHAT_INITIALIZE_ROOM -> {
                addDisposable(Observable.just(response)
                    .observeOn(Schedulers.computation())
                    .subscribeOn(Schedulers.computation())
                    .subscribe(
                        {
                            ChatRoom.getRoom(it!!.optJSONObject(0))?.apply {
                                RealmUtils.useTemporaryRealm { realm ->
                                    ownerID = usersRepository.fetchCachedMyUserId()
                                    chatRepository.updateChatRoomOwnerId(realm, this, ownerID!!)
                                    val participant = this.participants.first()
                                    if (this.isValid() || participant != null) {
                                        participant!!.chatRoomID = chatRoomID
                                        chatRepository.saveParticipant(realm, participant)
                                        profileStartChatLiveData.postValue(
                                            Pair(
                                                true,
                                                this.chatRoomID!!
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        { thr -> thr.printStackTrace()}
                    )
                )
            }
        }
    }

    override fun handleErrorResponse(
        idOp: String,
        callCode: Int,
        errorCode: Int,
        description: String?
    ) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)

        if (callCode == SERVER_OP_GET_TIMELINE_FOR_PROFILE)
            isFetchingPosts = false

    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return mutableListOf<MutableLiveData<*>>().apply {
            addAll(super.getAllObservedData())
            add(profileActionLiveData)
        }.toTypedArray()
    }

    override fun performSearch(query: String) {}

    override fun setUser(user: User, post: Boolean) {
        super.setUser(user, post)
        if (post) {
            profileUserLiveData.postValue(user)
        } else {
            profileUserLiveData.value = user
        }
    }

    fun cacheCurrentPost(post: Post) {
        postRepository.cacheCurrentPost(post)
    }

    fun isPrivate(it: User) = (it.isPrivate() && !relationshipManager.isFriendOrMyself(it))

    fun cachePostList(items: MutableList<Post>) {
        if (showCreateItem) items.removeAt(0)
        // INFO: 2019-11-28    real page to be used wants -1: first batch items was 0, not 1
        postRepository.cacheProfilePosts(items, (items.size / PAGINATION_SIZE) - 1)
    }

    fun handleChat() {

        if (profileUserLiveData.value == null) return

        RealmUtils.useTemporaryRealm { realm ->
            val participant = usersRepository.getParticipant(realm, profileUserLiveData.value!!, true)
            participant?.let {
                with(ChatRoom.checkRoomByParticipant(it.id, realm)) {
                    if (first && second?.isValid() == true)
                        profileStartChatLiveData.value = Pair(true, second!!.chatRoomID!!)
                    else {
                        initializeNewRoom(it, false)
                    }
                }
            }
        }
    }

    private fun initializeNewRoom(participant: Participant, hasID: Boolean = false) {
        val ownerID = usersRepository.fetchCachedMyUserId()
        if (ownerID.isNullOrEmpty()) return
        val chatRoom = ChatRoom(
            ownerID,
            RealmList(participant.id!!),
            if (hasID) participant.chatRoomID else null
        )
        addDisposable(
            chatRepository.initializeNewRoom(this, chatRoom)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {},
                    { thr -> thr.printStackTrace() }
                )
        )
    }
}