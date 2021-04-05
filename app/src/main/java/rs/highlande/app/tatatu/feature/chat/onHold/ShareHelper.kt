/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat.onHold

import android.content.Context
import android.view.View
import rs.highlande.app.tatatu.core.util.BaseHelper

class ShareHelper(context: Context, private val provider: ShareableProvider?): BaseHelper(context){

//    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
//        if (operationId == Constants.SERVER_OP_GET_SHAREABLE_LINK) {
//
//            handleProgress(false)
//
//            val link = responseObject?.getJSONObject(0)?.optString("uri")
//
//            Utils.fireShareIntent(contextRef.get(), link)
//
//            provider?.afterOps()
//        }
//    }
//
//    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
//        handleProgress(false)
//
//        if (operationId == Constants.SERVER_OP_GET_SHAREABLE_LINK)
//            (contextRef.get() as? BaseActivity)?.showAlert(R.string.error_shareable_creation)
//        else
//            (contextRef.get() as? BaseActivity)?.let { it.showError(it.getString(R.string.error_generic)) }
//    }

    fun initOps(isChat: Boolean) {
        /*var result: Array<Any?>? = null

        try {
            Handler().postDelayed({
                result = HLServerCalls.performShareCreation(provider?.getUserID(), provider?.getPostOrMessageID(), isChat)
            }, 500)

        } catch (e: JSONException) {
            e.printStackTrace()
        }

        handleProgress(true)

        if (contextRef.get() is BaseActivity)
           HLRequestTracker.getInstance((contextRef.get() as BaseActivity).application as? TTUApp)
                   .handleCallResult(this, contextRef.get() as BaseActivity, result)*/
    }


    private fun handleProgress(show: Boolean) {
        /*if (provider?.getProgressView() != null) {
            if (show) provider.getProgressView()!!.visibility = View.VISIBLE
            else provider.getProgressView()!!.visibility = View.GONE
        }
        else {
            (contextRef.get() as? BaseActivity)?.setProgressMessage(R.string.creating_shareable_link)
            (contextRef.get() as? BaseActivity)?.handleProgress(show)
        }*/
    }



    interface ShareableProvider {
        fun getProgressView(): View? { return null }
        fun afterOps() {
            // do nothing by default
        }

        fun getUserID(): String?
        fun getPostOrMessageID(): String?
    }

}