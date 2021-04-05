package rs.highlande.app.tatatu.feature.post.like.viewmodel

import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_GET_LIKES
import rs.highlande.app.tatatu.core.manager.RelationshipAction
import rs.highlande.app.tatatu.core.manager.RelationshipManager
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.ui.recyclerView.EndlessScrollListener
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.feature.commonRepository.PostRepository
import rs.highlande.app.tatatu.model.User

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class PostLikeViewModel(private val postRepository: PostRepository,private val relationshipManager: RelationshipManager): BaseViewModel() {

    val postLikeListLiveData = MutableLiveData<MutableList<User>>()
    var cachedLikeUser: User? = null

    val relationshipChangeLiveData = MutableLiveData<User>()
    val actionRequestLiveData = MutableLiveData<RelationshipAction>()
    var errorResponseLiveData = MutableLiveData<Boolean>()

    private var loadingMore = false
    private var currentPage = 0


    val scrollListener = object : EndlessScrollListener() {
        override fun onLoadMore() {
            loadingMore = true
            getPostLikes(true)
        }
    }

    fun getPostLikes(loadMore: Boolean = false) {
        addDisposable(
            postRepository.fetchCurrentPost()
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .subscribe(
                    {
                        it?.let {
                            if (loadMore) currentPage++ else currentPage = 0
                            postRepository.fetchPostLikes(this, it, currentPage)
                        }
                    },
                    { thr -> thr.printStackTrace() }
                )
        )
    }

    fun handleFollowerRelationChange() {
        cachedLikeUser?.let { user ->
            cachedLikeUser = relationshipManager.manageRelationship(user, this)
            relationshipChangeLiveData.value = cachedLikeUser
        }

    }

    fun onRelationshipActionClick(user: User) {
        cachedLikeUser = user
        actionRequestLiveData.value = relationshipManager.getRelationshipAction(user.detailsInfo.relationship)
    }

    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)
        if (callCode == SERVER_OP_GET_LIKES) {
            addDisposable(
                Observable.just(response)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(Schedulers.computation())
                    .subscribe(
                        { result ->
                            val tmpList = mutableListOf<User>()
                            for (i in 0 until result!!.length()) {

                                User.get(result.optJSONObject(i))?.let { data ->
                                    tmpList.add(data)
                                }
                            }
                            postLikeListLiveData.postValue(tmpList)
                        },
                        { thr -> thr.printStackTrace() }
                    )
            )

        }
    }

    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)
        LogUtils.e("", "Err Get post timeline")
        errorResponseLiveData.postValue(true)
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> = arrayOf(postLikeListLiveData)
}