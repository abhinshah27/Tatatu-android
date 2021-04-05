/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.core.cache

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.os.HandlerThread
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.Sort
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.koin.core.KoinComponent
import rs.highlande.app.tatatu.BuildConfig
import rs.highlande.app.tatatu.core.cache.DownloadReceiver.Companion.linkedBlockingQueue
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.chat.HLMediaType
import java.io.File


// TODO: 11/28/2018    NEED TO IMPROVE BY MAKING IT PROPORTIONAL
const val MAX_SIZE_CACHE_VIDEOS_BYTES = 500 * 1024 * 1024
const val MAX_SIZE_CACHE_PICTURES_BYTES = 200 * 1024 * 1024


/**
 * @author mbaldrighi on 11/15/2018.
 */
interface ICache: KoinComponent {

    fun getTag(): String?

    fun getMedia(url: String?, typeEnum: HLMediaType = HLMediaType.PHOTO): Any?

    fun isFileOnDisk(name: String?, typeEnum: HLMediaType = HLMediaType.PHOTO): Pair<Boolean, File?>

    fun addToMemoryCache(path: String?, size: Long?): Any?

    /**
     * Checks whether the cache must be freed because the max ([MAX_SIZE_CACHE_PICTURES_BYTES]
     * /[MAX_SIZE_CACHE_VIDEOS_BYTES]) has been reached.
     * @param fileSize The size of the present downloaded file. It can be `0` if called from
     * [DownloadReceiver] when and the download has already happened.
     * @param typeEnum The [HLMediaType] of the downloaded file.
     * @return The [Triple] containing infomation on (1) whether the cache must be freed up, (2) the
     * actual size to free up, and (3) the directory from which remove the cached files.
     */
    fun mustFreeUpSpace(fileSize: Int, typeEnum: HLMediaType = HLMediaType.PHOTO): Triple<Boolean, Int?, File?>

    fun moveTmpFileAndRename(oldPath: String?, newPath: String?, mime: String?, fromGallery: Boolean) {
        if (!oldPath.isNullOrBlank() && !newPath.isNullOrBlank()) {
            val mHandlerThread = HandlerThread("moveTmpMediaFile")
            mHandlerThread.start()
            Handler(mHandlerThread.looper).post {

                val oldFile = File(oldPath)
                if (oldFile.exists() && oldFile.length() > 0) {
                    oldFile.renameTo(File(newPath))

                    if (!fromGallery) oldFile.delete()
                }

                mHandlerThread.quitSafely()
            }
        }
    }

    fun flushCache()

    fun download(context: Context, uri: String, name: String, mediaType: HLMediaType, cache: ICache) {

        val editedName = name.replace("..", ".")

        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadUri = Uri.parse(uri)
            val request = DownloadManager.Request(downloadUri)

            //Restrict the types of networks over which this download may proceed.
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            //Set whether this download may proceed over a roaming connection.
            request.setAllowedOverRoaming(false)

            //Set UI reactions
            if (!hasQ()) request.setVisibleInDownloadsUi(false)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            //Set the title of this download, to be displayed in notifications (if enabled).
            request.setTitle(editedName)
            //Set a description of this download, to be displayed in notifications (if enabled)
            request.setDescription(editedName)

            //Set the MIME type of the download file
            val pair = when (mediaType) {
                HLMediaType.AUDIO -> "audio/mp4" to PATH_EXTERNAL_DIR_MEDIA_AUDIO
                HLMediaType.VIDEO -> "video/mp4" to PATH_EXTERNAL_DIR_MEDIA_VIDEO
                HLMediaType.PHOTO -> "image/jpg" to PATH_EXTERNAL_DIR_MEDIA_PHOTO
                else -> "" to ""
            }
            request.setMimeType(pair.first)

            if (!hasQ()) request.allowScanningByMediaScanner()

            request.setDestinationInExternalFilesDir(
                context,
                PATH_CUSTOM_HIGHLANDERS,
                "${pair.second}/$editedName"
            )

            // changes User-Agent
            request.addRequestHeader("User-Agent", BuildConfig.APPLICATION_ID)

            //Enqueue a new download and same the referenceId
            val downloadReference = downloadManager.enqueue(request)

            linkedBlockingQueue.put(downloadReference)

            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadReference))
            if (!cursor.moveToFirst()) {
                return
            }
            val sizeOfDownloadingFile = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            cursor.close()

            if (sizeOfDownloadingFile > 0) {
                checkCacheSize(sizeOfDownloadingFile, mediaType)
            }

            LogUtils.d(cache.getTag(), "Downloading $editedName")
        } catch (e: Exception) {
            val s = when (e) {
                is InterruptedException -> "Download of $editedName INTERRUPTED"
                else -> "ERROR with download $editedName"
            }

            LogUtils.d(cache.getTag(), s)
            e.printStackTrace()
        }
    }

    fun getDirCurrentSize(dir: File?): Int? {
        return if (dir?.exists() == true && dir.isDirectory) {
            var size = 0L
            val iter = dir.walkTopDown().iterator()
            iter.forEach {
                if (it.exists() && it.isFile) size += it.length()
            }
            size.toInt()
        }
        else null
    }

    fun freeUpDisk(smallestNeededSize: Int, dir: File?) {

        if (smallestNeededSize <= 0 || dir?.isDirectory == false) return

        RealmUtils.useTemporaryRealm {
            it.executeTransactionAsync { realm ->

                val objects =
                    RealmUtils.readFromRealmSorted(
                        realm,
                        HLCacheObject::class.java,
                        "creationDate",
                        Sort.ASCENDING
                    )
                var freedSpace = 0L

                objects?.let {
                    for (obj in objects) {
                        if (smallestNeededSize >= freedSpace) {
                            val list = dir
                                ?.listFiles()
                                ?.filter { file -> file.name == (obj as HLCacheObject).id }
                            if (list?.size == 1) {
                                if (list[0].delete()) {
                                    freedSpace += (obj as HLCacheObject).size
                                    RealmObject.deleteFromRealm(obj)
                                }
                            }
                        }
                        else break
                    }
                }
            }
        }

    }


    fun storeCacheObject(obj: HLCacheObject) {
        RealmUtils.useTemporaryRealm {
            it.executeTransactionAsync { realm2 ->
                realm2.insertOrUpdate(obj)
            }
        }
    }


    fun checkCacheSize(sizeOfDownloadingFile: Int, mediaType: HLMediaType) {
        val downloadTriple = mustFreeUpSpace(sizeOfDownloadingFile, mediaType)
        if (downloadTriple.first) {
            LogUtils.d(getTag(), "Cache max size about to be reached. Remaining bytes: ${downloadTriple.second}")

            freeUpDisk(sizeOfDownloadingFile, downloadTriple.third)
        }
    }

}

/**
 * Class defining the cache object
 */
@RealmClass open class HLCacheObject(@PrimaryKey var id: String = "", @Index var creationDate: Long = 0, var size: Long = 0): RealmModel

class StoreCacheObjectTask: AsyncTask<HLCacheObject, Void, Void>() {

    companion object {
        const val LOG_TAG = "StoreCacheObjectTask"
    }

    override fun doInBackground(vararg params: HLCacheObject?): Void? {

        RealmUtils.useTemporaryRealm {
            it.executeTransaction { realm2 ->
                realm2.insertOrUpdate(params[0]!!)
            }
        }

        return null
    }

}

fun String.isPictureType(): Boolean {
    return !this.isBlank() && this.startsWith("image")
}

fun String.isAudioType(): Boolean {
    return !this.isBlank() && this.startsWith("audio")
}

fun String.isVideoType(): Boolean {
    return !this.isBlank() && this.startsWith("video")
}
