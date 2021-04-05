package rs.highlande.app.tatatu.feature.inviteFriends.view.emailBottomSheet

import android.app.Activity
import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.isEmailValid
import rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel.EmailBottomSheetViewModel
import kotlin.math.roundToInt


/**
 * Created by Abhin.
 */
class EmailBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {

    private var mSendClick = false
    private var send: AppCompatTextView? = null
    private var cancel: AppCompatTextView? = null
    private var emailTo: AppCompatAutoCompleteTextView? = null
    private var emailFrom: AppCompatAutoCompleteTextView? = null
    private var emailSubject: AppCompatAutoCompleteTextView? = null
    private var emailMessage: AppCompatEditText? = null
    private var mBehavior: BottomSheetBehavior<*>? = null
    private lateinit var mView: View
    private val emailBottomSheetViewModel: EmailBottomSheetViewModel by sharedViewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        mView = View.inflate(context, R.layout.bottom_sheet_create_email, null)
        val llRoot = mView.findViewById(R.id.ll_root) as LinearLayoutCompat
        initViews()
        val params = llRoot.layoutParams
        params.height = getScreenHeight()
        llRoot.layoutParams = params
        dialog.setContentView(mView)
        mBehavior = BottomSheetBehavior.from(mView.parent as View)
        return dialog
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
        cancel = mView.findViewById(R.id.txt_cancel)
        send = mView.findViewById(R.id.txt_send)
        emailTo = mView.findViewById(R.id.edt_email_to)
        emailFrom = mView.findViewById(R.id.edt_email_from)
        emailSubject = mView.findViewById(R.id.edt_email_subject)
        emailMessage = mView.findViewById(R.id.edt_email_message)
    }

    private fun init() {
        checkEditText()
        cancel!!.setOnClickListener(this)
        send!!.setOnClickListener(this)
        emailFrom!!.setText(emailBottomSheetViewModel.mEmail)
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

    //check the fields
    private fun checkEditText() {
        val mTextWatcher = object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (emailTo!!.text!!.isNotEmpty() && isEmailValid(emailTo?.text.toString()) && emailFrom!!.text!!.isNotEmpty() && isEmailValid(
                        emailFrom?.text.toString()
                    ) && emailSubject!!.text!!.isNotEmpty() && emailMessage!!.text!!.isNotEmpty()
                ) {
                    send!!.setTextColor(ContextCompat.getColor(activity!!, R.color.ttu_d_primary))
                    mSendClick = true
                } else {
                    send!!.setTextColor(ContextCompat.getColor(activity!!, R.color.ttu_light_gray_primary))
                    mSendClick = false
                }
            }
        }
        emailTo!!.addTextChangedListener(mTextWatcher)
        emailFrom!!.addTextChangedListener(mTextWatcher)
        emailSubject!!.addTextChangedListener(mTextWatcher)
        emailMessage!!.addTextChangedListener(mTextWatcher)
    }

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.txt_cancel -> {
                dismiss()
            }
            R.id.txt_send -> {
                if (mSendClick) dismiss()
            }
        }
    }

    override fun onDestroy() {
        hideKeyboard(activity!!)
        super.onDestroy()
    }

    //hiding the keyboard
    private fun hideKeyboard(activity: Activity) {
        val imm: InputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) view = View(activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // Convert dp to pixel
    private fun getScreenHeight(): Int {
        val dip = 32f
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, resources.displayMetrics)
        return (Resources.getSystem().displayMetrics.heightPixels - px).roundToInt()
    }
}