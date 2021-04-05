/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.connection

import org.json.JSONArray

/**
 * @author mbaldrighi on 2019-07-11.
 */
interface OnServerMessageReceivedListener {

    fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?)
    fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?)

}
