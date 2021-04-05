package rs.highlande.app.tatatu.core.util

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.webkit.MimeTypeMap
import android.widget.ImageView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import jp.wasabeef.glide.transformations.BlurTransformation
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.feature.chat.HLMediaType
import rs.highlande.app.tatatu.model.chat.ChatMessageType
import java.io.File
import java.net.URISyntaxException
import java.net.URLConnection

/**
 * File holding all the methods related to media handling.
 * @author mbaldrighi on 2019-06-28.
 */


fun <T> ImageView.setPicture(obj: T, options: RequestOptions? = null, listener: RequestListener<Drawable>? = null) {
    GlideApp.with(this).load(obj).also {
        it.apply(options ?: RequestOptions.centerCropTransform().diskCacheStrategy(DiskCacheStrategy.ALL))
        if (listener != null)
            it.listener(listener)
        it.into(this)
    }
}

fun <T> CustomViewTarget<ImageView, Drawable>.setPicture(obj: T, options: RequestOptions? = null, listener: RequestListener<Drawable>? = null) {
    GlideApp.with(this.view).load(obj).also {
        it.apply(options ?: RequestOptions.centerCropTransform().diskCacheStrategy(DiskCacheStrategy.ALL))
        if (listener != null)
            it.listener(listener)
        it.into(this)
    }
}

fun <T> ImageView.setIcon(obj: T) {
    GlideApp.with(this).load(obj).also {
        it.apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
        it.into(this)
    }
}

//fun CropperView.setBitmapPicture(obj: DataListDisplay, options: RequestOptions? = null, fade: Boolean = false) {
//    GlideApp.with(this).asBitmap().load(obj.imagePath).also {
//        it.apply(options ?: RequestOptions.centerCropTransform().placeholder(R.color.white).error(R.color.white).diskCacheStrategy(DiskCacheStrategy.NONE))
//        it.into(object : CustomTarget<Bitmap>() {
//            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                setImageBitmap(resource)
//            }
//
//            override fun onLoadCleared(placeholder: Drawable?) {
//
//            }
//        })
//    }
//}


fun <T> ImageView.setProfilePicture(obj: T) {
    setPicture(obj, RequestOptions.circleCropTransform().placeholder(R.drawable.ic_placeholder_profile).fallback(R.drawable.ic_placeholder_profile).error(R.drawable.ic_placeholder_profile).diskCacheStrategy(DiskCacheStrategy.ALL))
}

fun <T> ImageView.setBlurredPicture(obj: T) {

    setPicture(obj, RequestOptions.bitmapTransform(BlurTransformation(100)).diskCacheStrategy(DiskCacheStrategy.ALL))

//    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
//    GlideApp.with(this).load(obj).also {
//        it.apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.DATA))
//        it.into(object : CustomViewTarget<ImageView, Bitmap>(this) {
//
//            override fun onLoadFailed(errorDrawable: Drawable?) {
//                LogUtils.e("", "Resource loading: FAILED")
//            }
//
//            override fun onResourceCleared(placeholder: Drawable?) {
//                LogUtils.d("", "Resource loading: CLEARED")
//            }
//
//            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
////                val canvas = Canvas(resource)
//
//                val blurPaintOuter = Paint()
//                blurPaintOuter.color = Color.WHITE
//                blurPaintOuter.maskFilter = BlurMaskFilter(100f, BlurMaskFilter.Blur.NORMAL)
////                canvas.drawBitmap(resource, 0f, 0f, blurPaintOuter)
//
////                val blurPaintInner = Paint()
////                blurPaintInner.color = Color.WHITE
////                blurPaintInner.maskFilter = BlurMaskFilter(100f, BlurMaskFilter.Blur.OUTER)
////                canvas.drawRect(Rect(view.left, view.top, view.bottom, view.right), blurPaintInner)
//
//
//                setLayerType(View.LAYER_TYPE_SOFTWARE, blurPaintOuter)
//                setLayerPaint(blurPaintOuter)
//                setImageBitmap(resource)
//            }
//        })
//    }
}

fun <T> ImageView.setPictureWithTarget(obj: T, options: RequestOptions? = null, target: CustomViewTarget<ImageView, Drawable>) {
    GlideApp.with(this)
        .load(obj)
        .apply(options ?: RequestOptions.centerCropTransform())
        .into(target)
}

fun <T> ImageView.setPictureWithTargetBitmap(obj: T, options: RequestOptions? = null, target: CustomViewTarget<ImageView, Bitmap>) {
    GlideApp.with(this)
        .asBitmap()
        .load(obj)
        .apply(options ?: RequestOptions.centerCropTransform())
        .into(target)
}


fun ImageView.clear() {
    if (context.isValid())
        GlideApp.with(this).clear(this)
}

fun getReadableVideoDuration(duration: Long): String {
    val timeDifference = duration / 1000
    val h = (timeDifference / 3600).toInt()
    val m = ((timeDifference - h * 3600) / 60).toInt()
    val s = (timeDifference - (h * 3600) - m * 60).toInt()
    return if (h != 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}


/**
 * It deletes the file at the specified path.
 * @param filePath the provided file path.
 * @param deleteOnClose boolean value needed only in some places (default = false).
 */
fun deleteMediaFile(filePath: String?, deleteOnClose: Boolean) {
    if (isStringValid(filePath)) {
        val f = File(Uri.parse(filePath).path!!)
        deleteMediaFile(f, deleteOnClose)
    }
}

/**
 * It deletes the specified file.
 * @param file the provided file.
 * @param deleteOnClose boolean value needed only in some places (default = false).
 */
fun deleteMediaFile(file: File?, deleteOnClose: Boolean) {
    if (file != null) {
        val deleteAnyway = file.length() == 0L || file.length() < 1000
        if (deleteOnClose || deleteAnyway) {
            if (file.exists()) {
                val filePath = file.path
                if (file.delete())
                    LogUtils.d("MediaUtils", "Audio file deleted successfully @ $filePath")
                else
                    LogUtils.e("MediaUtils", "COULDN'T DELETE audio file")
            }
        }
    }
}


/**
 * Created by Abhin.
 */
object PathUtil {

    @SuppressLint("NewApi", "Recycle")
    @Throws(URISyntaxException::class)
    fun getPath(context: Context, uri: Uri): String? {
        var mURI = uri
        var selection: String? = null
        var selectionArgs: Array<String>? = null

        if (DocumentsContract.isDocumentUri(context.applicationContext, mURI)) {
            when {
                isExternalStorageDocument(mURI) -> {
                    val docId = DocumentsContract.getDocumentId(mURI)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
                isDownloadsDocument(mURI) -> {
                    val id = DocumentsContract.getDocumentId(mURI)
                    mURI = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
                }
                isMediaDocument(mURI) -> {
                    val docId = DocumentsContract.getDocumentId(mURI)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    when (split[0]) {
                        "image" -> mURI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> mURI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> mURI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    selection = "_id=?"
                    selectionArgs = arrayOf(split[1])
                }
            }
        }
        if ("content".equals(mURI.scheme!!, ignoreCase = true)) {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            var cursor: Cursor?
            try {
                cursor = context.contentResolver.query(mURI, projection, selection, selectionArgs, null)
                val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor.moveToFirst()) {
                    return cursor.getString(columnIndex)
                }
            } catch (e: Exception) {
            }

        } else if ("file".equals(mURI.scheme!!, ignoreCase = true)) {
            return mURI.path
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }


    //Check file type video format
    fun isVideoFormat(imagePath: String): Boolean {
        val extension = getExtension(imagePath)
        val mimeType = if (TextUtils.isEmpty(extension)) URLConnection.guessContentTypeFromName(imagePath)
        else MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return mimeType != null && mimeType.startsWith("video")

    }

    //Get file Extension
    private fun getExtension(path: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
        if (!TextUtils.isEmpty(extension)) {
            return extension
        }
        return if (path.contains(".")) {
            path.substring(path.lastIndexOf(".") + 1, path.length)
        } else {
            ""
        }
    }

    fun getFileNameForMedia(urlFromServer: String, type: HLMediaType): String? {
        if (isStringValid(urlFromServer)) {
            val lastIndex = urlFromServer.lastIndexOf("/") + 1
            val name = urlFromServer.substring(lastIndex)

            var prefix: String? = null
            when (type) {
                HLMediaType.AUDIO -> prefix = FILENAME_MEDIA_AUDIO
                HLMediaType.PHOTO -> prefix = FILENAME_MEDIA_PHOTO
                HLMediaType.VIDEO -> prefix = FILENAME_MEDIA_VIDEO
            }

            if (areStringsValid(prefix, name)) return prefix + "_" + name
        }

        return null
    }

    fun getFileNameFromPath(path: String): String? {
        if (isStringValid(path)) {
            val lastIndex = path.lastIndexOf("/") + 1
            return path.substring(lastIndex)
        }

        return null
    }

}

fun mapMediaTypeForChat(mediaType: String): ChatMessageType {
    return when(mediaType) {
        "photo" -> ChatMessageType.PICTURE
        "video" -> ChatMessageType.VIDEO
        else -> ChatMessageType.TEXT
    }
}
