/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat

import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import rs.highlande.app.tatatu.core.ui.BaseActivity

/**
 * @author mbaldrighi on 10/22/2018.
 */
open class RemoveFocusClickListener(private val victim: View): View.OnClickListener {
    override fun onClick(v: View?) {
        victim.clearFocus()
    }
}

class PictureOrVideoMenuItemClickListener(private val activity: BaseActivity,
                                          private val fragment: Fragment? = null,
                                          private val mListener: OnTargetMediaUriSelectedListener):
        PopupMenu.OnMenuItemClickListener {

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        /*var mediaFileUri: String? = null
        when (menuItem.itemId) {
            R.id.take_picture -> {
                mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, HLMediaType.PHOTO, fragment)
            }
            R.id.record_video -> {
                mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, HLMediaType.VIDEO, fragment)
            }
        }
        if (!mediaFileUri.isNullOrBlank())
            mListener.onTargetMediaUriSelect(mediaFileUri)*/
        return true
    }

}

interface OnTargetMediaUriSelectedListener {
    fun onTargetMediaUriSelect(mediaFileUri: String?)
}