package rs.highlande.app.tatatu.feature.post.api

import android.os.Bundle
import io.reactivex.Observable
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.*
import rs.highlande.app.tatatu.core.api.BaseApi
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.model.PostComment

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class PostApi: BaseApi() {

    /**
     * Send a like/unlike post request to server.
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param postID the post id
     * @param action can be 1 for like and 2 for unlike
     */
    fun doLikeUnlikePost(
        caller: OnServerMessageReceivedListener,
        postID: String,
        action: Int
    ): Observable<RequestStatus> {

        return if (postID.isNotBlank()) tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putString("postID", postID)
                    putInt("action", action)
                },
                callCode = SERVER_OP_DO_LIKE_UNLIKE,
                logTag = "DO POST LIKE UNLIKE call",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)
    }

    /**
     * Deletes the sppecified post.
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param postID the post id
     */
    fun doDeletePost(
        caller: OnServerMessageReceivedListener,
        postID: String
    ): Observable<RequestStatus> {

        return if (postID.isNotBlank()) tracker.callServer(
            SocketRequest(
                Bundle().apply { putString("postID", postID) },
                callCode = SERVER_OP_DELETE_POST,
                logTag = "DELETE POST",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)
    }

    /**
     * Requests a post comments list.
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param postID the post id
     * @param skip the number of comments to skip, 0 by default
     * @param limit the number of comments to fetch [PAGINATION_SIZE] by default
     */
    fun doGetComments(
        caller: OnServerMessageReceivedListener,
        postID: String,
        skip: Int = 0,
        limit: Int = PAGINATION_SIZE): Observable<RequestStatus> {

        return if (postID.isNotBlank()) tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    if (!postID.isNullOrBlank()) putString("postID", postID)
                    putInt(
                        "skip",
                        if (skip > -1) skip else 0
                    )
                    putInt(
                        "limit",
                        if (limit <= 0) skip
                        else {
                            PAGINATION_SIZE
                        }
                    )
                },
                callCode = SERVER_OP_GET_COMMENTS,
                logTag = "GET COMMENTS call",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)
    }

    /**
     * Requests a single post.
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param postID the post id
     */
    fun doGetPost(
        caller: OnServerMessageReceivedListener,
        postID: String): Observable<RequestStatus> {

        return if (postID.isNotBlank()) tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putString("postID", postID)
                },
                callCode = SERVER_OP_GET_POST,
                logTag = "GET POST call",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)
    }

    /**
     * Creates a comment.
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param postID the post id
     * @param text the comment text
     * @param level can be 1 or 0 (default value). If 1, [parentCommentID] must be set
     * @param parentCommentID the parentCommentID
     */
    fun doCreateComments(
        caller: OnServerMessageReceivedListener,
        postID: String,
        comment: PostComment
    ): Observable<RequestStatus> {


        return if (postID.isNotBlank()) tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putString("postID", postID)
                    putString("date", comment.date)
                    putString("text", comment.text)
                    putInt("level", comment.level)
                    if (comment.level == 1) {
                        if (!comment.parentCommentID.isNullOrBlank()) {
                            putString("parentCommentID", comment.parentCommentID)
                        } else {
                            //TODO 26/07: if parentCommentID is null and level is 1, check best approach.
                            throw Exception("child comment without parent")
                        }
                    } else putString("parentCommentID", "")

                },
                callCode = SERVER_OP_CREATE_COMMENT,
                logTag = "CREATE COMMENTS call",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)
    }

    /**
     * Send a like/unlike comment request to server.
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param commentID the comment id
     * @param action can be 1 for like and 2 for unlike
     */
    fun doLikeUnlikeComment(
        caller: OnServerMessageReceivedListener,
        commentID: String,
        action: Int
    ): Observable<RequestStatus> {

        return if (commentID.isNotBlank()) tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putString("commentID", commentID)
                    putInt("action", action)
                },
                callCode = SERVER_OP_DO_COMMENT_LIKE_UNLIKE,
                logTag = "DO COMMENT LIKE UNLIKE call",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)
    }

    /**
     * Requests a post likes list.
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param postID the post id
     * @param skip the number of likes to skip, 0 by default
     * @param limit the number of likes to fetch [PAGINATION_SIZE] by default
     */
    fun doGetLikes(
        caller: OnServerMessageReceivedListener,
        postID: String,
        skip: Int = 0,
        limit: Int = PAGINATION_SIZE): Observable<RequestStatus> {

        return  if (postID.isNotBlank()) {
            tracker.callServer(
                SocketRequest(
                    Bundle().apply {
                        putString("postID", postID)
                        putInt(
                            "skip",
                            if (skip > -1) skip else 0
                        )
                        putInt(
                            "limit",
                            if (limit <= 0) skip
                            else {
                                PAGINATION_SIZE
                            }
                        )
                    },
                    callCode = SERVER_OP_GET_LIKES,
                    logTag = "GET LIKES call",
                    caller = caller
                )
            )
        } else Observable.just(RequestStatus.ERROR)
    }


    /**
     * Reports a post
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param postID the post id
     */
    fun doReportPost(
        caller: OnServerMessageReceivedListener,
        postID: String
    ): Observable<RequestStatus> {

        return  if (postID.isNotBlank()) {
            tracker.callServer(
                SocketRequest(
                    Bundle().apply {
                        putString("postID", postID)
                    },
                    callCode = SERVER_OP_DO_REPORT_POST,
                    logTag = "GET LIKES call",
                    caller = caller
                )
            )
        } else Observable.just(RequestStatus.ERROR)
    }

    /**
     * Shares the specified user profile
     * @param caller the [OnServerMessageReceivedListener] listening for the response
     * @param userID of the user to be shared
     */
    fun doSharePost(
        caller: OnServerMessageReceivedListener,
        postID: String): Observable<RequestStatus> {

        return if (postID.isNotEmpty()) tracker.callServer(
            SocketRequest(
                Bundle().apply {
                    putString("postID", postID)
                },
                callCode = SERVER_OP_DO_SHARE_POST,
                logTag = "SHARE POST call",
                caller = caller
            )
        ) else Observable.just(RequestStatus.ERROR)

    }
}