package rs.highlande.app.tatatu.feature.post.timeline.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_TIMELINE
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_TIMELINE_FOR_PROFILE
import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.core.ui.recyclerView.EndlessScrollListener
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.feature.commonApi.CommonApi
import rs.highlande.app.tatatu.feature.commonRepository.PostRepository
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.post.common.BasePostViewModel
import rs.highlande.app.tatatu.model.Post
import java.util.concurrent.ConcurrentHashMap

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class PostTimelineViewModel(
    application: Application,
    postRepository: PostRepository,
    val usersRepository: UsersRepository,
    relationshipManager: RelationshipManager
): BasePostViewModel(application, postRepository, usersRepository, relationshipManager) {

    enum class Action { NONE, REFRESH, LOAD_MORE }

    var timelineType: CommonApi.TimelineType = CommonApi.TimelineType.TIMELINE
    val postTimelineLiveData = MutableLiveData<Triple<MutableList<Post>, Action, Int>>()
    val hasPostsToRemoveLiveData = MutableLiveData<Set<Post>>()

    var playingVideos = ConcurrentHashMap<String, Long>()

    private var currentAction = Action.NONE
    private var loadingMore = false

    var userID: String = ""

    val scrollListener = object : EndlessScrollListener() {
        override fun onLoadMore() {
            loadingMore = true

            getTimeline(true)
        }
    }

    private var currentPostPage = 0

    fun getTimeline(loadMore: Boolean = false) {
        if (userID.isNotBlank()) {
            if (loadMore) {
                currentAction = Action.LOAD_MORE
                currentPostPage++
            }
            if (timelineType != CommonApi.TimelineType.PROFILE)
                postRepository.getTimelinePosts(this, currentPostPage, userID, timelineType, true)
            else {
                if (!loadMore && postRepository.cachedProfilePosts != null) {
                    with(postRepository.cachedProfilePosts) {
                        currentPostPage = this!!.second
                        handlePostResponse(first)
                    }
                } else postRepository.getProfileTimelinePosts(this, currentPostPage, userID, true)
            }
        }
    }

    fun refreshTimeline() {
        setCurrentPost(Post())
        currentAction = Action.REFRESH
        currentPostPage = 0
        getTimeline()
    }


    fun fetchPostsToRemove() {
        val obs = postRepository.fetchPostsToRemove()
        addDisposable(
            obs
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {
                        if (it != null) {
                            hasPostsToRemoveLiveData.postValue(it)
                            postRepository.clearPostsToRemove()
                        }
                    },
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    private fun handlePostResponse(postList: MutableList<Post>) {
        // update scroll listener canFetch property
        scrollListener.canFetch = (postList.size % PAGINATION_SIZE == 0)
        postTimelineLiveData.postValue(Triple(postList, currentAction, currentPostPage))

        loadingMore = false
        currentAction = Action.NONE

    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        when (callCode) {
            SERVER_OP_GET_TIMELINE, SERVER_OP_GET_TIMELINE_FOR_PROFILE -> {
                addDisposable(
                    Observable.just(response).subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe(
                            {

                                LogUtils.d(logTag, "testTIMELINE elements: size=${it?.length()}")

                                val tmpList = mutableListOf<Post>()
                                for (i in 0 until it!!.length()) {
                                    Post.get(it.optJSONObject(i))?.let { data ->
                                        tmpList.add(data)
                                    }
                                }
                                handlePostResponse(tmpList)
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        LogUtils.e("", "Err Get post timeline")
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return super.getAllObservedData().plus(arrayOf<MutableLiveData<*>>(postTimelineLiveData, hasPostsToRemoveLiveData))
    }

    fun getCachedUserID(): String {
        return usersRepository.fetchCachedMyUserId()
    }
}