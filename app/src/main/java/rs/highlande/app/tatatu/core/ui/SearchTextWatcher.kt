package rs.highlande.app.tatatu.core.ui

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import rs.highlande.app.tatatu.core.util.LogUtils
import java.util.concurrent.TimeUnit

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class SearchTextWatcher(val listener: SearchListener): TextWatcher {

    override fun afterTextChanged(p0: Editable?) {}

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(string: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (!string.isNullOrEmpty()) {
            if (string.length >= 3) {
                listener.onQueryStringAvailable(string.toString())
            }
        } else {
            listener.onQueryStringIsEmpty()
        }
    }

    interface SearchListener {
        fun onQueryStringAvailable(query: String)

        fun onQueryStringIsEmpty()
    }


    companion object {

        fun createTextChangeObservable(
            editText: EditText?,
            actionsOnStringAvailable: (() -> Unit)? = null,
            actionsOnStringEmpty: (() -> Unit)? = null,
            actionsOnValueEmitted: ((String) -> Unit)?,
            actionsOnRxError: (() -> Unit)? = null,
            tag: String?
        ): Disposable? {

            return editText?.let {
                Observable
                    .create<String> { emitter ->
                        val textWatcher = SearchTextWatcher(
                            object :
                                SearchListener {
                                override fun onQueryStringIsEmpty() {
                                    actionsOnStringEmpty?.invoke()
                                }

                                override fun onQueryStringAvailable(query: String) {
                                    LogUtils.d(tag ?: "createTextChangeObservable()", "Current string in ET: $query")

                                    actionsOnStringAvailable?.invoke()

                                    emitter.onNext(query)
                                }
                            })
                        editText.addTextChangedListener(textWatcher)
                        emitter.setCancellable { editText.removeTextChangedListener(textWatcher) }
                    }
                    .debounce(300, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                    .toFlowable(BackpressureStrategy.LATEST)
                    .observeOn(Schedulers.io())
                    .subscribe(
                        { search ->
                            LogUtils.d(tag ?: "createTextChangeObservable()", "Emitted value: $search")
                            actionsOnValueEmitted?.invoke(search)
                        },
                        {
                            it.printStackTrace()
                            actionsOnRxError?.invoke()
                        }
                    )
            }
        }
    }

}