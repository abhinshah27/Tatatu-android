package rs.highlande.app.tatatu.feature.account.profile.view.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.greenrobot.eventbus.EventBus
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.hideKeyboard
import rs.highlande.app.tatatu.model.event.ImageBottomEvent


/**
 * Created by Abhin.
 */
class ImageBottomSheetDialog : BottomSheetDialogFragment(), View.OnClickListener {

    private var gallery: ConstraintLayout? = null
    private var camera: ConstraintLayout? = null
    private var mBehavior: BottomSheetBehavior<*>? = null
    private lateinit var mView: View
    private var clickImage: Boolean = false
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        mView = View.inflate(context, R.layout.bottom_sheet_image, null)
        dialog.setContentView(mView)
        initViews()
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
        camera = mView.findViewById(R.id.ll_camera)
        gallery = mView.findViewById(R.id.ll_gallery)
    }

    private fun init() {
        camera!!.setOnClickListener(this)
        gallery!!.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.ll_camera -> {
                clickImage = true
                dismiss()
            }
            R.id.ll_gallery -> {
                clickImage = false
                dismiss()
            }
        }
    }


    override fun onDismiss(dialog: DialogInterface) {
        if (clickImage) {
            EventBus.getDefault().post(ImageBottomEvent(mImageClick = true, mGalleryClick = false))
        } else {
            EventBus.getDefault().post(ImageBottomEvent(mImageClick = false, mGalleryClick = true))
        }
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        hideKeyboard(activity!!)
        super.onDestroy()
    }
}