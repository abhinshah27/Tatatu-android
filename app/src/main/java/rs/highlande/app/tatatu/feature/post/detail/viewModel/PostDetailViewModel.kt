package rs.highlande.app.tatatu.feature.post.detail.viewModel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CREATE_COMMENT
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_DO_COMMENT_LIKE_UNLIKE
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_COMMENTS
import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.core.ui.recyclerView.EndlessScrollListener
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.core.util.nowToDBDate
import rs.highlande.app.tatatu.feature.commonRepository.PostRepository
import rs.highlande.app.tatatu.feature.commonRepository.UsersRepository
import rs.highlande.app.tatatu.feature.post.common.BasePostViewModel
import rs.highlande.app.tatatu.model.Post
import rs.highlande.app.tatatu.model.PostComment
import rs.highlande.app.tatatu.model.User
import java.util.*

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class PostDetailViewModel(
    application: Application,
    postRepository: PostRepository,
    usersRepository: UsersRepository,
    relationshipManager: RelationshipManager
): BasePostViewModel(application, postRepository, usersRepository, relationshipManager) {

    val postCommentsLiveData = MutableLiveData<Pair<MutableList<PostComment>, Boolean>>()
    val newCommentSavedLiveData = MutableLiveData<Boolean>()
    val commentLikeLiveData = MutableLiveData<Boolean>()

    var parentCommentID: String? = null
    var newComment: PostComment? = null
    var currentComment: PostComment? = null

    private var currentCommentPage = 0

    private var loadingMore = false


    var videoCache: Long? = null


    val scrollListener = object : EndlessScrollListener() {
        override fun onLoadMore() {
            loadingMore = true
            getPostComments(true)
        }
    }


    fun getPostComments(loadMore: Boolean = false) {
        addDisposable(postRepository.fetchCurrentPost()
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(
                {
                    it?.let {
                        if (loadMore) currentCommentPage++
                        postRepository.fetchPostComments(this, it, currentCommentPage)
                    }
                },
                { thr -> thr.printStackTrace() }
            )
        )
    }

    fun handleLikeUnlikeComment(comment: PostComment) {
        if (comment.liked) {
            comment.likesCount --
            comment.liked = false
        } else {
            comment.likesCount ++
            comment.liked = true
        }
        currentComment = comment
        addDisposable(
            Observable.just(postRepository.likeUnlikeComment(this, comment.id, comment.liked))
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {},
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    fun saveComment(user: User, commentText: String) {
        addDisposable(
            postRepository.fetchCurrentPost()
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {
                        val level = if (!parentCommentID.isNullOrEmpty()) { 1 } else { 0 }

                        newComment = PostComment().apply {
                            text = commentText
                            date = nowToDBDate()
                            this.level = level
                            userData = user
                            parentCommentID = this@PostDetailViewModel.parentCommentID
                        }

                        postRepository.saveComment(this, it!!.uid, newComment!!)

                        parentCommentID = ""
                    },
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        when(callCode) {
            SERVER_OP_GET_COMMENTS -> {
                addDisposable(
                    Observable.just(response)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe(
                            {
                                val tmpList = mutableListOf<PostComment>()
                                for (i in 0 until it!!.length()) {
                                    PostComment.get(it.optJSONObject(i))?.let { data ->
                                        tmpList.add(data)
                                    }
                                }

                                // update scroll listener canFetch property
                                scrollListener.canFetch = (tmpList.size >= PAGINATION_SIZE && tmpList[19].level == 0)

                                postCommentsLiveData.postValue(orderComments(tmpList) to loadingMore)

                                loadingMore = false
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
            SERVER_OP_CREATE_COMMENT -> {
                addDisposable(
                    Observable.just(response)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe(
                            {
                                val result = PostComment.getIds(it)
                                if (!result.isNullOrEmpty()) {
                                    if (currentPostLiveData.value != null) {
                                        currentPostLiveData.value!!.commentsCount ++
                                    }
                                    newComment!!.id = result[0]
                                    newCommentSavedLiveData.postValue(true)
                                } else {
                                    newCommentSavedLiveData.postValue(false)
                                }
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
            SERVER_OP_DO_COMMENT_LIKE_UNLIKE -> {
                addDisposable(
                    Observable.just(response)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        .subscribe(
                            {
                                if (it!!.length() > 0) {
                                    commentLikeLiveData.postValue(true)
                                } else {
                                    commentLikeLiveData.postValue(false)
                                }
                            },
                            { thr -> thr.printStackTrace() }
                        )
                )
            }
        }
    }

    private fun orderComments(comments: List<PostComment>): MutableList<PostComment> {
        return ArrayList<PostComment>().apply {

            comments.forEach {

                //                var parentCommentId = ""

                if (!it.isSubComment()) {
//                    parentCommentId = it.id
                    add(it)
                }
                else {
                    if (!contains(it) && !it.parentCommentID.isNullOrBlank()) {
                        val tmpParent = PostComment(it.parentCommentID!!)
                        val tmp = comments.filter { lev1 -> lev1.parentCommentID == tmpParent.id }
                        addAll(indexOf(tmpParent) + 1, tmp)
                    }
                }
            }
        }



//        for(comment in comments) {
//            if (!comment.isSubComment()) {
//                orderedList.add(comment)
//            } else {
//
//                // TODO: 2019-08-06    fix sorting: use indexOfLast???
//
//                val parentIndex = comments.indexOfLast {
//                    comment != it && (
//                                    (it.isSubComment() && comment.parentCommentID == it.parentCommentID) ||
//                                            (!it.isSubComment() && comment.parentCommentID == it.id)
//                            )
//                }
//                orderedList.add(parentIndex+1, comment)
//            }
//        }
//        return orderedList
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return super.getAllObservedData().plus(arrayOf<MutableLiveData<*>>(postCommentsLiveData, newCommentSavedLiveData,
            postDeletedLiveData, postUnFollowedLiveData, commentLikeLiveData))
    }

    fun getCachedCurrentPost(): Post? {
        return if (currentPostLiveData.value != null) {
            return currentPostLiveData.value!!
        } else {
            null
        }
    }


}