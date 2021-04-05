package rs.highlande.app.tatatu.feature.commonView.webView

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.LinearLayoutCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.hideKeyboard


/**
 * Created by Abhin.
 */
class WebViewBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {
    private val mWebViewModel: WebViewModel by sharedViewModel()
    private var openBrowser: AppCompatButton? = null
    private var cancel: AppCompatButton? = null
    private var mBehavior: BottomSheetBehavior<*>? = null
    private lateinit var mView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        mView = View.inflate(context, R.layout.bottom_sheet_web_view_browser, null)
        val llRoot = mView.findViewById(R.id.ll_root) as LinearLayoutCompat
        initViews()
        val params = llRoot.layoutParams
        llRoot.layoutParams = params
        dialog.setContentView(mView)
        mBehavior = BottomSheetBehavior.from(mView.parent as View)

        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.SheetDialog)
    }

    override fun onStart() {
        super.onStart()
        mBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    //init the Views
    private fun initViews() {
        cancel = mView.findViewById(R.id.btn_web_view_cancel)
        openBrowser = mView.findViewById(R.id.btn_open_browser)
    }

    private fun init() {
        cancel!!.setOnClickListener(this)
        openBrowser!!.setOnClickListener(this)
        mBehavior!!.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }
        })
    }


    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.btn_web_view_cancel -> {
                dismiss()
            }
            R.id.btn_open_browser -> {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(mWebViewModel.mWebUrl)
                }
                startActivity(Intent.createChooser(sendIntent, resources.getText(R.string.choose_to)))
            }
        }
    }

    override fun onDestroy() {
        hideKeyboard(activity!!)
        super.onDestroy()
    }
}