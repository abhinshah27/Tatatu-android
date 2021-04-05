package rs.highlande.app.tatatu.feature.commonRepository

import io.reactivex.Observable
import org.koin.core.inject
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.core.repository.BaseRepository
import rs.highlande.app.tatatu.core.util.PAGINATION_SIZE
import rs.highlande.app.tatatu.feature.commonApi.CommonApi
import rs.highlande.app.tatatu.feature.post.api.PostApi
import rs.highlande.app.tatatu.model.HomeDataObject
import rs.highlande.app.tatatu.model.Post
import rs.highlande.app.tatatu.model.PostComment
import rs.highlande.app.tatatu.model.PostType


const val POST_GRID_PAGINATION_SIZE = 50

/**
 * Repository handling all operations for fetching and saving for [Post].
 * @author mbaldrighi on 2019-06-27.
 */
class PostRepository : BaseRepository(), CreatePostRepository {

    private val commonApi: CommonApi by inject()
    private val postApi: PostApi by inject()

    var cachedHomePosts = mutableListOf<HomeDataObject>()

    var cachedProfilePosts: Pair<MutableList<Post>, Int>? = null

    var timelinePosts = mutableMapOf<String, Post>()

    private var cachedCurrentPost: Post? = null
    private var cachedPostsToRemove = mutableSetOf<Post>()

    fun getHomePosts(caller: OnServerMessageReceivedListener): Observable<RequestStatus> = getMyTimelinePosts(caller, CommonApi.TimelineType.HOME, isHomePage = true)

    fun getTimelinePosts(
        caller: OnServerMessageReceivedListener,
        page: Int = 0,
        userID: String? = null,
        type: CommonApi.TimelineType = CommonApi.TimelineType.TIMELINE,
        isHomePage: Boolean = true
    ): Observable<RequestStatus> {
        return if (userID.isNullOrEmpty()) {
            getMyTimelinePosts(caller, type, page * PAGINATION_SIZE, isHomePage = isHomePage)
        } else {
            commonApi.getTimeline(caller, type = type, skip = page * PAGINATION_SIZE, userID = userID, isHomePage = isHomePage)
        }
    }

    fun getProfileTimelinePosts(
        caller: OnServerMessageReceivedListener,
        pageOrSkip: Int = 0,
        userID: String? = null,
        isPage: Boolean = false,
        limit: Int = PAGINATION_SIZE
    ): Observable<RequestStatus> {
        return if (userID.isNullOrEmpty()) {
            commonApi.getTimelineForProfile(caller, skip = if (isPage) pageOrSkip * PAGINATION_SIZE else pageOrSkip, limit = limit)
        } else {
            commonApi.getTimelineForProfile(caller, skip = if (isPage) pageOrSkip * PAGINATION_SIZE else pageOrSkip, userID = userID, limit = limit)
        }
    }

    fun getMyTimelinePosts(
        caller: OnServerMessageReceivedListener,
        type: CommonApi.TimelineType = CommonApi.TimelineType.TIMELINE,
        page: Int = 0,
        limit: Int = PAGINATION_SIZE,
        isHomePage: Boolean = true
    ): Observable<RequestStatus> = commonApi.getTimeline(caller, type = type, skip = page, limit = limit, isHomePage = isHomePage)

    fun getDiaryPosts(caller: OnServerMessageReceivedListener, userID: String): Observable<RequestStatus> = commonApi.getTimeline(caller, userID)


    // TODO: 2019-07-28    return here for post persistence
    fun savePost(post: Post) {
        savePost(post) {
            if (cachedHomePosts.contains(it))
                (cachedHomePosts[cachedHomePosts.indexOf(it)] as Post).caption = it.caption
            else cachedHomePosts.add(0, it)

            if (timelinePosts.contains(it.uid))
                timelinePosts[it.uid]?.caption = it.caption
            else
                timelinePosts[it.uid] = it
        }
    }

    fun addNewItem(posts: List<Post>): Observable<List<Post>> {

        return Observable.just(posts.toMutableList()).map {
            it.add(
                0,
                Post().apply {
                    uid = "newPost"
                    type = PostType.NEW
                }
            )
            it
        }
    }

    fun cacheCurrentPost(post: Post) {
        this.cachedCurrentPost = post
    }

    fun fetchCurrentPost(): Observable<Post?> {
        return cachedCurrentPost?.let {
            Observable.just(it)
        } ?: run {
            Observable.empty<Post?>()
        }
    }

    //region + Remove section +

    fun cachePostsToRemove(posts: Collection<Post>) {
        this.cachedPostsToRemove.addAll(posts)
    }

    fun clearPostsToRemove() {
        this.cachedPostsToRemove.clear()
    }

    fun fetchPostsToRemove() = Observable.just(cachedPostsToRemove)

    //endregion


    fun likeUnlikePost(caller: OnServerMessageReceivedListener, post: Post, like: Boolean):
            Observable<RequestStatus> = postApi.doLikeUnlikePost(caller, post.uid, if (like) { 1 } else { 2 })


    fun likeUnlikeComment(caller: OnServerMessageReceivedListener, commentID: String, like: Boolean):
            Observable<RequestStatus> = postApi.doLikeUnlikeComment(caller, commentID, if (like) { 1 } else { 2 })

    fun deletePost(caller: OnServerMessageReceivedListener, post: Post):
            Observable<RequestStatus> = postApi.doDeletePost(caller, post.uid)

    fun fetchPostLikes(caller: OnServerMessageReceivedListener,
                          post: Post,
                          page: Int = 0):
            Observable<RequestStatus> = postApi.doGetLikes(caller, post.uid, page * PAGINATION_SIZE)

    fun fetchPostComments(caller: OnServerMessageReceivedListener,
                          post: Post,
                          page: Int = 0):
            Observable<RequestStatus> = postApi.doGetComments(caller, post.uid, page * PAGINATION_SIZE)

    fun saveComment(caller: OnServerMessageReceivedListener, postID: String, comment: PostComment): Observable<RequestStatus> =
        postApi.doCreateComments(caller, postID, comment)

    fun reportPost(caller: OnServerMessageReceivedListener, post: Post):
            Observable<RequestStatus> = postApi.doReportPost(caller, post.uid)

    fun sharePost(caller: OnServerMessageReceivedListener, post: Post):
            Observable<RequestStatus> = postApi.doSharePost(caller, post.uid)

    fun getPost(caller: OnServerMessageReceivedListener, postID: String):
            Observable<RequestStatus> = postApi.doGetPost(caller, postID)

    fun cacheProfilePosts(items: MutableList<Post>, currentPage: Int) {
        cachedProfilePosts = Pair(items, currentPage)
    }

    fun clearCachedProfilePosts() {
        cachedProfilePosts = null
    }
}
