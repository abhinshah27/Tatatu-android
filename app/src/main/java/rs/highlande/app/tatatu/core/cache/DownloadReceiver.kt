/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.core.cache

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import io.reactivex.Observable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.PathUtil
import rs.highlande.app.tatatu.feature.chat.HLMediaType
import java.util.concurrent.LinkedBlockingQueue

/**
 * @author mbaldrighi on 11/15/2018.
 */
class DownloadReceiver: BroadcastReceiver() {

    var picturesCache: PicturesCache? = null
    var audioVideoCache: AudioVideoCache? = null

    var listeners = mutableSetOf<OnDownloadCompletedListener?>()


    companion object {
        private val LOG_TAG = DownloadReceiver::class.java.simpleName

        var linkedBlockingQueue: LinkedBlockingQueue<Long> = LinkedBlockingQueue()
        var currentDownloads = mutableSetOf<String>()
    }

    override fun onReceive(context: Context, intent: Intent) {

        val disposable = Observable.just(true)
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .subscribeWith(
                object : DisposableObserver<Boolean>() {
                    override fun onComplete() {
                        dispose()
                    }

                    override fun onNext(t: Boolean) {
                        var cursor: Cursor? = null
                        try {
                            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
                            val query = DownloadManager.Query()
                            val downloadManager =
                                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                            if (downloadId == linkedBlockingQueue.peek()) {
                                val enqueue = linkedBlockingQueue.take()
                                cursor = downloadManager.query(query.setFilterById(enqueue))
                                cursor.moveToFirst()
                                while (!cursor.isAfterLast) {
                                    val status =
                                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                                    val reason =
                                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                                    val mime =
                                        cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE))
                                    val size =
                                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                                    var mediaType: HLMediaType? = null
                                    var cache: ICache? = null
                                    when {
                                        picturesCache != null && mime.isPictureType() -> {
                                            mediaType = HLMediaType.PHOTO
                                            cache = picturesCache
                                        }
                                        audioVideoCache != null && (mime.isAudioType() || mime.isVideoType()) -> {
                                            if (mime.isVideoType()) mediaType = HLMediaType.VIDEO
                                            cache = audioVideoCache
                                        }
                                    }

                                    cache?.let {
                                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                            val downloadFileLocalUri =
                                                cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))

                                            it.addToMemoryCache(
                                                downloadFileLocalUri,
                                                size.toLong()
                                            )

                                            val fileName =
                                                PathUtil.getFileNameFromPath(downloadFileLocalUri)
                                            if (!fileName.isNullOrBlank())
                                                currentDownloads.remove(fileName)

                                            LogUtils.d(LOG_TAG, "Download SUCCESS: $downloadFileLocalUri")

                                            for (i in listeners)
                                                i?.onDownloadCompleted(enqueue, downloadFileLocalUri, mime)

                                            if (size > 0 && mediaType != null) {
                                                it.checkCacheSize(0, mediaType)
                                            }
                                        } else if (reason == DownloadManager.ERROR_INSUFFICIENT_SPACE) {
                                            if (size > 0 && mediaType != null) {
                                                it.checkCacheSize(0, mediaType)
                                            }
                                        }
                                    }

                                    cursor.moveToNext()
                                }
                            }
                        } catch (e: InterruptedException) {
                            LogUtils.e(LOG_TAG, e.toString())
                        } finally {
                            cursor?.close()
                        }
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                        dispose()
                    }
                }
            )
    }


    interface OnDownloadCompletedListener {
        fun onDownloadCompleted(reference: Long, downloadFileLocalUri: String?, mime: String?)
    }


}