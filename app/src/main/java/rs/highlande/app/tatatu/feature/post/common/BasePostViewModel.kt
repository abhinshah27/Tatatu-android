package rs.highlande.app.tatatu.feature.post.common

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import rs.highlande.app.tatatu.connection.webSocket.*
import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.core.ui.BaseAndroidViewModel
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.commonApi.CommonApi
import rs.highlande.app.tatatu.feature.commonRepository.PostRepository
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.commonView.PostSheetActionsImpl
import rs.highlande.app.tatatu.model.Post
import rs.highlande.app.tatatu.model.User

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
abstract class BasePostViewModel(
    application: Application,
    val postRepository: PostRepository,
    private val usersRepository: UsersRepository,
    private val relationshipManager: RelationshipManager
): BaseAndroidViewModel(application), PostSheetActionsImpl {

    val postLikeLiveData = MutableLiveData<Pair<Boolean, Post>>()
    val postDeletedLiveData = MutableLiveData<Pair<Boolean, Post?>>()
    val postUnFollowedLiveData = MutableLiveData<Boolean>()
    val currentPostLiveData = MutableLiveData<Post>()
    val postLikeConnected = MutableLiveData<Pair<Boolean, Post>>()
    val postReportedLiveData = MutableLiveData<Boolean>()
    var postDeeplinkLiveData = MutableLiveData<Pair<String?, String?>>()

    val postAuthorUpdated = MutableLiveData<Triple<Boolean, User?, Post?>>()

    private var currentRelationshipID: String? = null

    var currentPostId = ""

    fun handleLikeUnlikePost(post: Post) {
        if (post.liked) {
            post.likes --
            post.liked = false
        } else {
            post.likes ++
            post.liked = true
        }
        setCurrentPost(post)
        addDisposable(postRepository.likeUnlikePost(this, post, post.liked)
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {
                    if (it == RequestStatus.SENT)
                        postLikeConnected.postValue(true to post)
                },
                { thr -> thr.printStackTrace() }
            )
        )
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)

        when ( callCode) {
            SERVER_OP_DO_LIKE_UNLIKE -> {
                addDisposable(
                    Observable.combineLatest(
                        Observable.just(response), postRepository.fetchCurrentPost(),
                        BiFunction<JSONArray?, Post?, Pair<JSONArray?, Post?>> { t1, t2 ->
                            Pair(t1, t2)
                        })
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe(
                            {
                                postLikeLiveData.postValue(Pair(!Post.getIds(it.first).isNullOrEmpty(), it.second!!))
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
            SERVER_OP_DELETE_POST -> {
                addDisposable(
                    Observable.just(response)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe(
                            {
                                if (!Post.getIds(it).isNullOrEmpty()) {
                                    addDisposable(postRepository.fetchCurrentPost()
                                        .subscribeOn(Schedulers.computation())
                                        .observeOn(Schedulers.computation())
                                        .subscribe(
                                            { post ->
                                                post?.let { post1 ->
                                                    postDeletedLiveData.postValue(true to post1)
                                                }
                                            },
                                            { thr -> thr.printStackTrace() }
                                        )
                                    )
                                } else {
                                    postRepository.clearPostsToRemove()
                                    postDeletedLiveData.postValue(false to null)
                                }
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
            SERVER_OP_MANAGE_FOLLOWER_REQUEST -> {
                addDisposable(
                    Observable.just(response)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe(
                            {
                                if (it!!.length() > 0) {
                                    //TODO 31/07: Remove posts from timeline
                                    postUnFollowedLiveData.postValue(true)
                                } else {
                                    postUnFollowedLiveData.postValue(false)
                                }
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
            SERVER_OP_GET_USER -> {
                addDisposable(
                    Observable.combineLatest(Observable.just(response), postRepository.fetchCurrentPost(),
                        BiFunction<JSONArray?, Post?, Pair<JSONArray?, Post?>> { t1, t2 ->
                            Pair(t1, t2)
                        })
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe(
                            {

                                it.first?.let { response ->
                                    User.get(response.optJSONObject(0))?.let { user ->
                                        it.second?.let { post ->
                                            currentRelationshipID = user.detailsInfo.requestID
                                            val triple = Triple(true, user, post)
                                            LogUtils.d("TEST_BSHEET", "User updated --> $triple")

                                            postAuthorUpdated.postValue(triple)

                                        } ?: postAuthorUpdated.postValue(Triple(false, null, null))
                                    }
                                } ?: postAuthorUpdated.postValue(Triple(false, null, null))
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
            SERVER_OP_DO_REPORT_POST -> {
                addDisposable(
                    Observable.just(response)
                        .observeOn(Schedulers.computation())
                        .subscribeOn(Schedulers.computation())
                        .subscribe(
                            {
                                if (it!!.length() > 0) {
                                    postReportedLiveData.postValue(true)
                                }
                                LogUtils.d("post reported", "post reported")
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
            SERVER_OP_DO_SHARE_POST -> {
                addDisposable(
                    Observable.just(response)
                        .observeOn(Schedulers.computation())
                        .subscribeOn(Schedulers.computation())
                        .subscribe(
                            {
                                if (it!!.length() > 0) {
                                    postDeeplinkLiveData.postValue(
                                        it.getJSONObject(0).getString("deepLink") to
                                                it.getJSONObject(0).getString("sharesCount"))
                                } else {
                                    postDeeplinkLiveData.postValue(null to null)
                                }
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
            SERVER_OP_GET_POST -> {
                addDisposable(
                    Observable.just(response)
                        .observeOn(Schedulers.computation())
                        .subscribeOn(Schedulers.computation())
                        .subscribe(
                            {
                                if (it!!.length() > 0) {
                                    Post.get(it.getJSONObject(0))?.let { data ->
                                        currentPostLiveData.postValue(data)
                                    }
                                } else {
                                    currentPostLiveData.postValue(null)
                                }
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)

        when ( callCode) {
            SERVER_OP_DO_LIKE_UNLIKE -> LogUtils.e(logTag, "Error LIKE/UNLIKE post")
            SERVER_OP_DELETE_POST -> LogUtils.e(logTag, "Error DELETE post")
            SERVER_OP_MANAGE_FOLLOWER_REQUEST -> LogUtils.e(logTag, "Error UNFOLLOW")
            SERVER_OP_GET_USER -> {
                LogUtils.e(logTag, "Error GET USER")
                postAuthorUpdated.postValue(Triple(false, null , null))
            }
            SERVER_OP_DO_SHARE_POST -> {
                LogUtils.e(logTag, "Error GET POST SHARE")
                postDeeplinkLiveData.postValue(null to null)
            }
            SERVER_OP_GET_POST -> {
                LogUtils.e(logTag, "Error GET POST")
                currentPostLiveData.postValue(null)
            }
        }
    }

    fun getPost(postID: String) {
        addDisposable(
            postRepository.getPost(this, postID)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {},
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    fun getCurrentPost() {
        addDisposable(
            postRepository.fetchCurrentPost()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {
                        currentPostLiveData.postValue(it)
                    },
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    fun setCurrentPost(post: Post) {
        currentPostId = post.uid
        postRepository.cacheCurrentPost(post)
    }


    fun refreshPostAuthor(authorID: String) {
        usersRepository.fetchUser(this, authorID, arrayOf(CommonApi.UserInfo.GENERIC, CommonApi.UserInfo.DETAIL))
    }

    fun resetBottomSheetRequest() = postAuthorUpdated.postValue(Triple(false, null , null))


    override fun getAllObservedData(): Array<MutableLiveData<*>> = arrayOf(postLikeLiveData, currentPostLiveData)

    override fun deletePost() {
        addDisposable(
            postRepository.fetchCurrentPost()
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {
                        postRepository.deletePost(this, it!!)
                        postRepository.cachePostsToRemove(listOf(it))
                    },
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    override fun unfollow() {
        if (!currentRelationshipID.isNullOrBlank())
            relationshipManager.manageUnFollowRequest(this, currentRelationshipID!!)
    }

    override fun share() {
        addDisposable(postRepository.fetchCurrentPost()
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {
                    postRepository.sharePost(this, it!!)
                },
                { thr -> thr.printStackTrace() }
            )
        )
    }

    override fun flagInappropriate() {
        addDisposable(postRepository.fetchCurrentPost()
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {
                    postRepository.reportPost(this, it!!)
                },
                { thr -> thr.printStackTrace() }
            )
        )
    }

    override fun openEditPost() {

        // they are view-related ops. logic is in view layer.

    }


    companion object {
        val logTag = BasePostViewModel::class.java.simpleName
    }

}