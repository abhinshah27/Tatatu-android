package rs.highlande.app.tatatu.core.util

import android.content.Context
import androidx.annotation.StringRes
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.http.ERROR_EXISTING_USERNAME
import rs.highlande.app.tatatu.connection.http.ERROR_SOCIAL_TO_CONFIRM

/**
 * TODO - File description
 * @author mbaldrighi on 2019-07-25.
 */


abstract class TTUException(protected val errorCode: Int, protected val context: Context? = null) : Exception() {
    @StringRes protected abstract fun getMessageId(): Int
}


class TTUAuthException(errorCode: Int, context: Context?) : TTUException(errorCode, context) {

    override val message: String?
        get() {
            return if (context?.isValid() == true) {
                when (errorCode) {
                    ERROR_EXISTING_USERNAME -> context.getString(getMessageId())
                    ERROR_SOCIAL_TO_CONFIRM -> "Social profile to be SIGNED UP -> GOTO checkboxes screen"
                    else -> super.message
                }
            } else null
        }

    override fun getMessageId(): Int {
        return when (errorCode) {
            ERROR_EXISTING_USERNAME -> R.string.error_existing_username
            else -> 0
        }
    }

    fun isSocialToConfirmException() = errorCode == ERROR_SOCIAL_TO_CONFIRM

}