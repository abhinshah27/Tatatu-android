package rs.highlande.app.tatatu.core.util

import android.content.Context
import androidx.annotation.StringRes

class ResourcesHelper(val context: Context) {

    fun getString(@StringRes stringId: Int, vararg formatArgs: Any?): String {
        return if (formatArgs.isNotEmpty())
            context.getString(stringId, formatArgs)
        else
            context.getString(stringId)
    }

}