package rs.highlande.app.tatatu.feature.post.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import rs.highlande.app.tatatu.model.Post

/**
 * TODO - File description
 * @author mbaldrighi on 2019-08-02.
 */


abstract class BasePostSheetFragment: BottomSheetDialogFragment() {

    protected lateinit var listener: BasePostSheetListener
    lateinit var post: Post


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(getLayoutId(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener.onBottomSheetReady(this)
    }


    @LayoutRes abstract fun getLayoutId(): Int


}


interface BasePostSheetListener {
    fun onBottomSheetReady(bottomSheet: BasePostSheetFragment)
}