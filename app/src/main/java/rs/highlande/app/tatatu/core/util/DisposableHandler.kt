package rs.highlande.app.tatatu.core.util

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * TODO - File description
 * @author mbaldrighi on 2019-07-04.
 */


interface DisposableHandler {

    fun addDisposable(disposable: Disposable?)

    fun addDisposables(disposables: List<Disposable>)

    fun clearDisposables()

    fun disposeOf()
}


class CompositeDisposableHandler : DisposableHandler {

    private val compositeDisposable = CompositeDisposable()

    override fun addDisposable(disposable: Disposable?) {
        if (disposable != null)
            compositeDisposable.add(disposable)
    }

    override fun addDisposables(disposables: List<Disposable>) {
        compositeDisposable.addAll(*disposables.toTypedArray())
    }

    override fun clearDisposables() {
        compositeDisposable.clear()
    }

    override fun disposeOf() {
        compositeDisposable.dispose()
    }
}