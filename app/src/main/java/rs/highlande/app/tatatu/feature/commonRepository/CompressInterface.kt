package rs.highlande.app.tatatu.feature.commonRepository

import android.content.Context

/**
 * Created by Abhin.
 */
interface CompressInterface {
    fun compressVideoPath(path: String?, mContext: Context)

    fun getCompressionProgress(progress: Long)
}
