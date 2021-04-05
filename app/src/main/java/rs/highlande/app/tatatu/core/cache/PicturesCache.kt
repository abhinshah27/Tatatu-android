/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.core.cache

import android.app.DownloadManager
import android.content.Context
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import org.koin.core.inject
import rs.highlande.app.tatatu.core.util.PATH_CUSTOM_HIGHLANDERS
import rs.highlande.app.tatatu.core.util.PATH_EXTERNAL_DIR_MEDIA_PHOTO
import rs.highlande.app.tatatu.core.util.PathUtil
import rs.highlande.app.tatatu.feature.chat.HLMediaType
import java.io.File

/**
 * @author mbaldrighi on 11/15/2018.
 */
class PicturesCache(
    private val context: Context
): ICache {

    companion object {
        val LOG_TAG = PicturesCache::class.java.simpleName
    }

    private val picturesMapUri = mutableMapOf<String, Uri>()

    private val downloadReceiver: DownloadReceiver by inject()

    init {
        context.applicationContext.registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        downloadReceiver.picturesCache = this

    }


    override fun getMedia(url: String?, typeEnum: HLMediaType): Any? {
        if (url.isNullOrBlank()) return null

        val name = PathUtil.getFileNameForMedia(url, typeEnum)?.replace("..", ".")
        val checkDisk = isFileOnDisk(name, typeEnum)
        return when {
            picturesMapUri[name] != null -> {
                picturesMapUri[name]!!.path?.let {
                    File(picturesMapUri[name]!!.path!!)
                }
            }
            checkDisk.first -> {
                addToMemoryCache(checkDisk.second?.path, checkDisk.second?.length())
                checkDisk.second
            }
            else -> {
                download(context, url, name!!, typeEnum, this)
                null
            }
        }
    }


    override fun isFileOnDisk(name: String?, typeEnum: HLMediaType): Pair<Boolean, File?> {
        val dir = context.getExternalFilesDir("$PATH_CUSTOM_HIGHLANDERS/$PATH_EXTERNAL_DIR_MEDIA_PHOTO")
        if (dir?.exists() == true && dir.isDirectory) {
            val arrFiles =  dir.listFiles { _, fileName ->
                fileName == (name?.replace("..", "."))
            }
            if (arrFiles?.size == 1)
                return true to arrFiles[0]
        }
        return false to null
    }

//    override fun addToMemoryCache(path: String?) {
//        FileToDrawableTask(picturesMap).execute(path)
//    }

    override fun addToMemoryCache(path: String?, size: Long?): Uri? {
        val fileName = path?.substring(path.lastIndexOf("/") + 1)

        return if (!fileName.isNullOrBlank()) {
            val uri = Uri.parse(path)
            picturesMapUri[fileName] = uri

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
//        val fileName = MediaHelper.getFileNameFromPath(oldPath)
//
//        val checkFile = File(Uri.parse(newPath ?: "").path)
//        if (!checkFile.exists() || checkFile.length() <= 0) {
//            val file = File(context.filesDir, PATH_EXTERNAL_DIR_MEDIA_PHOTO)
//            var exists = true
//            if (!file.exists()) exists = file.mkdir()
//
//            val newFile = if (exists) File(file, fileName) else null
//            if (newFile != null)
//                super.moveTmpFileAndRename(oldPath, newFile.path, mime, fromGallery)
//        }
//    }

    override fun mustFreeUpSpace(fileSize: Int, typeEnum: HLMediaType): Triple<Boolean, Int?, File?> {
        val dir = context.getExternalFilesDir("$PATH_CUSTOM_HIGHLANDERS/$PATH_EXTERNAL_DIR_MEDIA_PHOTO")
        val currentSize = getDirCurrentSize(dir)

        return if (currentSize != null) {
            val sizeNeeded = currentSize + fileSize
            Triple(sizeNeeded > MAX_SIZE_CACHE_PICTURES_BYTES, sizeNeeded - MAX_SIZE_CACHE_PICTURES_BYTES, dir)
        }
        else Triple(false, null, null)
    }

    override fun flushCache() {
        picturesMapUri.clear()
    }

    override fun getTag(): String? {
        return LOG_TAG
    }



    private class FileToDrawableTask(private val picturesMap: MutableMap<String, Drawable>):
        AsyncTask<String?, Void, Drawable?>() {

        private var path: String? = null

        override fun doInBackground(vararg params: String?): Drawable? {
            path = params[0]
            return if (!path.isNullOrBlank()) Drawable.createFromPath(path) else null
        }

        override fun onPostExecute(result: Drawable?) {
            super.onPostExecute(result)

            val fileName = path?.substring(path!!.lastIndexOf("/") + 1)
            if (!fileName.isNullOrBlank())
                if (result != null) picturesMap[fileName] = result
        }
    }


}