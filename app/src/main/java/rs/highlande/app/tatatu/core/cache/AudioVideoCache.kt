/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.core.cache

import android.app.DownloadManager
import android.content.Context
import android.content.IntentFilter
import android.net.Uri
import org.koin.core.inject
import rs.highlande.app.tatatu.core.util.PATH_CUSTOM_HIGHLANDERS
import rs.highlande.app.tatatu.core.util.PATH_EXTERNAL_DIR_MEDIA_AUDIO
import rs.highlande.app.tatatu.core.util.PATH_EXTERNAL_DIR_MEDIA_VIDEO
import rs.highlande.app.tatatu.core.util.PathUtil
import rs.highlande.app.tatatu.feature.chat.HLMediaType
import java.io.File

/**
 * @author mbaldrighi on 11/15/2018.
 */
class AudioVideoCache(
    private val context: Context
): ICache {

    companion object {
        val LOG_TAG = AudioVideoCache::class.java.simpleName
    }

    private val audioVideoMap = mutableMapOf<String, Uri>()

    private val downloadReceiver: DownloadReceiver by inject()

    init {
        context.applicationContext.registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        downloadReceiver.audioVideoCache = this
    }


    override fun getMedia(url: String?, typeEnum: HLMediaType): Any? {
        if (url.isNullOrBlank()) return null

        val name = PathUtil.getFileNameForMedia(url, typeEnum)
        if (name.isNullOrEmpty()) return null
        val checkDisk = isFileOnDisk(name, typeEnum)
        return when {
            audioVideoMap[name] != null -> {
                audioVideoMap[name]!!.path?.let {
                    File(audioVideoMap[name]!!.path!!)
                }
            }
            checkDisk.first -> {
                addToMemoryCache(checkDisk.second?.path, checkDisk.second?.length())
                checkDisk.second
            }
            else -> {
                download(context, url, name, typeEnum, this)
                null
            }
        }
    }

    override fun isFileOnDisk(name: String?, typeEnum: HLMediaType): Pair<Boolean, File?> {
        val dir = context.getExternalFilesDir(PATH_CUSTOM_HIGHLANDERS)
        if (dir?.exists() == true && dir.isDirectory) {

            val arrDir = dir.listFiles { _, fileName ->
                val dirName = when (typeEnum) {
                    HLMediaType.AUDIO -> PATH_EXTERNAL_DIR_MEDIA_AUDIO
                    HLMediaType.VIDEO -> PATH_EXTERNAL_DIR_MEDIA_VIDEO
                    else -> null
                }

                fileName == dirName
            }
            if (arrDir?.size == 1) {
                val arrFiles =  arrDir[0].listFiles { _, fileName ->
                    fileName == (name?.replace("..", "."))
                }

                if (arrFiles?.size == 1)
                    return true to arrFiles[0]
            }
        }
        return false to null
    }

    override fun addToMemoryCache(path: String?, size: Long?): Uri? {
        val fileName = path?.substring(path.lastIndexOf("/") + 1)

        return if (!fileName.isNullOrBlank()) {
            val uri = Uri.parse(path)
            audioVideoMap[fileName] = uri

            if (size != null) {
                storeCacheObject(
                    HLCacheObject(
                        fileName,
                        System.currentTimeMillis(),
                        size
                    )
                )
            }

            uri
        }
        else null
    }

//    override fun moveTmpFileAndRename(oldPath: String, newPath: String?, mime: String?, fromGallery: Boolean) {
//        if (newPath.isNullOrBlank() && mime.isNullOrBlank()) return
//
//        val fileName = MediaHelper.getFileNameFromPath(if (!newPath.isNullOrBlank()) newPath else oldPath)
//        val checkFile = File(Uri.parse(newPath ?: "").path)
//        if (!checkFile.exists() || checkFile.length() <= 0) {
//            val file = File(context.filesDir, if (mime!!.isAudioType()) PATH_EXTERNAL_DIR_MEDIA_AUDIO else PATH_EXTERNAL_DIR_MEDIA_VIDEO)
//            var exists = true
//            if (!file.exists()) exists = file.mkdir()
//
//            val newFile = if (exists) File(file, fileName) else null
//            if (newFile != null)
//                super.moveTmpFileAndRename(oldPath, newFile.path, mime, fromGallery)
//        }
//    }

    override fun mustFreeUpSpace(fileSize: Int, typeEnum: HLMediaType): Triple<Boolean, Int?, File?> {
        return if (typeEnum == HLMediaType.VIDEO) {
            val dir = context.getExternalFilesDir("$PATH_CUSTOM_HIGHLANDERS/$PATH_EXTERNAL_DIR_MEDIA_VIDEO")
            val currentSize = getDirCurrentSize(dir)

            if (currentSize != null) {
                val sizeNeeded = currentSize + fileSize
                Triple(sizeNeeded > MAX_SIZE_CACHE_VIDEOS_BYTES, sizeNeeded - MAX_SIZE_CACHE_VIDEOS_BYTES, dir)
            }
            else Triple(false, null, null)
        }
        else Triple(false, null, null)
    }

    override fun flushCache() {
        audioVideoMap.clear()
    }

    override fun getTag(): String? {
        return LOG_TAG
    }

}