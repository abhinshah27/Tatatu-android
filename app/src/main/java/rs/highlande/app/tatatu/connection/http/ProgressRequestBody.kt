/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.connection.http

import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.internal.Util
import okio.BufferedSink
import okio.Okio
import okio.Source
import rs.highlande.app.tatatu.core.util.LogUtils
import java.io.File

/**
 * @author mbaldrighi on 5/12/2018.
 */
class ProgressRequestBody(
    private val file: File?,
    private val contentType: String,
    private val listener: ProgressListener
) : RequestBody() {

    override fun contentLength(): Long {
        return file?.length() ?: 0L
    }

    override fun contentType(): MediaType? {
        return MediaType.parse(contentType)
    }

    override fun writeTo(sink: BufferedSink) {
        var source: Source? = null
        try {
            source = Okio.source(file!!)
            var total = 0.0
            var read = 0.0

            val size = contentLength()
            if (size > 0) {
                while (read != -1.0) {
                    read = source!!.read(sink.buffer(), SEGMENT_SIZE.toLong()).toDouble()

                    total += read
                    sink.flush()

                    val div = total / size
                    val div100 = div * 100
                    val progress = div100.toInt()
                    LogUtils.v("UPLOAD FILE PROGRESS", "total: $total size: $size -> percent: $div100 > $progress")
                    this.listener.transferred(progress.toLong())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            this.listener.exceptionCaught(e)
        } finally {
            Util.closeQuietly(source)
        }
    }

    interface ProgressListener {
        fun transferred(num: Long)
        fun exceptionCaught(e: Exception)
    }

    companion object {

        private const val SEGMENT_SIZE = 2048 // okio.Segment.SIZE
    }

}
