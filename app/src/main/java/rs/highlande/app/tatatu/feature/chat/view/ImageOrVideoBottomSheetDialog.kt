package rs.highlande.app.tatatu.feature.chat.view

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.greenrobot.eventbus.EventBus
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.hideKeyboard
import rs.highlande.app.tatatu.databinding.BottomSheetPictureOrVideoBinding
import rs.highlande.app.tatatu.model.event.ImageBottomEvent


/**
 * Created by leandro.garcia@highlande.rs
 */
class ImageOrVideoBottomSheetDialog : BottomSheetDialogFragment() , View.OnClickListener {

    private var gallery: ConstraintLayout? = null
    private var camera: ConstraintLayout? = null
    private var video: ConstraintLayout? = null
    private var videoGallery: ConstraintLayout? = null
    private var mBehavior: BottomSheetBehavior<*>? = null
    private lateinit var mView: View
    private var optionClicked: Int = -1
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
        video = mView.findViewById(R.id.ll_video)
        video?.visibility = View.VISIBLE
    }

    private fun init() {
        camera!!.setOnClickListener(this)
        gallery!!.setOnClickListener(this)
        video!!.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.ll_camera -> {
                optionClicked = 0
                dismiss()
            }
            R.id.ll_gallery -> {
                optionClicked = 1
                dismiss()
            }
            R.id.ll_video -> {
                optionClicked = 2
                dismiss()
            }
        }
    }


    override fun onDismiss(dialog: DialogInterface) {
        when (optionClicked) {
            0 -> EventBus.getDefault().post(ImageBottomEvent(mImageClick = true))
            1 -> EventBus.getDefault().post(ImageBottomEvent(mGalleryClick = true))
            2 -> EventBus.getDefault().post(ImageBottomEvent(mVideoClick = true))
        }
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        hideKeyboard(activity!!)
        super.onDestroy()
    }

}