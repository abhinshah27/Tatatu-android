package rs.highlande.app.tatatu.feature.commonRepository

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import rs.highlande.app.tatatu.model.Post

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-04.
 */
interface UnFollowRepository {

    fun followSuggested(userID: String): Observable<String> {
        return Observable.just(userID)
    }

}

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-09.
 */
interface CreatePostRepository {

    fun savePost(post: Post, block: (Post) -> Unit) {
        Observable.just(post)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(
                { block.invoke(it) },
                { thr -> thr.printStackTrace() }
            )
            .dispose()
    }

}