/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.feature.chat

import android.Manifest
import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.provider.MediaStore
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.MediaController
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.VideoView
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.HashSet
import java.util.concurrent.TimeUnit

import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.http.ProgressRequestBody
import rs.highlande.app.tatatu.core.util.GlideApp

import rs.highlande.app.tatatu.core.util.dpToPx
import rs.highlande.app.tatatu.core.util.isContextValid
import rs.highlande.app.tatatu.core.util.isStringValid
import rs.highlande.app.tatatu.feature.commonRepository.CompressInterface
import rs.highlande.app.tatatu.feature.commonRepository.UploadingInterFace
import rs.highlande.app.tatatu.feature.commonRepository.UploadingMedia
import rs.highlande.app.tatatu.feature.createPost.view.CreatePostViewModel

/**
 * @author mbaldrighi on 9/27/2017.
 */
enum class HLMediaType(val value: String) {
    AUDIO("audio"),
    PHOTO("photo"),
    PHOTO_PROFILE("photoprofile"),
    PHOTO_WALL("photowall"),
    VIDEO("video"),
    DOCUMENT("document");

    override fun toString(): String {
        return value.toLowerCase()
    }
}

enum class ActionType { COMPRESS, UPLOAD, CREATE, EDIT }

class MediaUploadManager(val listener: MediaUploadManagerListener, val uploadingInterface: UploadingInterFace) {
    fun getUploadMedia(context: Context, path: String, croppedBitmap: Bitmap? = null, isVideo: Boolean = false) {
        UploadingMedia(uploadingInterface, context, path, croppedBitmap, isVideo).uploadingData(object : ProgressRequestBody.ProgressListener {
            override fun transferred(num: Long) {
                listener.postProgress(Triple(true, ActionType.UPLOAD, num))
            }

            override fun exceptionCaught(e: Exception) {
                listener.postProgress(Triple(false, null, 0))
                listener.postError(true to ActionType.UPLOAD)
            }
        }, object : CompressInterface {
            override fun compressVideoPath(path: String?, mContext: Context) {
                //no op
            }

            override fun getCompressionProgress(progress: Long) {
                listener.postProgress(Triple(true, ActionType.COMPRESS, progress))
            }
        })
    }
}

interface MediaUploadManagerListener {
    fun postProgress(event: Triple<Boolean, ActionType?, Long>)
    fun postError(error: Pair<Boolean, ActionType?>)
}