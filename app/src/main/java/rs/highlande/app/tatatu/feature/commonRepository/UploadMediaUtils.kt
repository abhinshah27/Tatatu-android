@file:Suppress("INACCESSIBLE_TYPE")

package rs.highlande.app.tatatu.feature.commonRepository

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import com.vincent.videocompressor.VideoCompress
import okhttp3.MediaType
import okhttp3.MultipartBody
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.http.ProgressRequestBody
import rs.highlande.app.tatatu.core.util.BitmapFileName
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.model.createPost.uploadImage.UploadMediaResponse
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by Abhin.
 */

//create the multipart body for Image
@RequiresApi(Build.VERSION_CODES.KITKAT)
fun prepareFileImagePart(partName: String, fileUri: String, listener: ProgressRequestBody.ProgressListener): MultipartBody.Part {
    val file = File(fileUri)
    return MultipartBody.Part.createFormData(
        partName,
        file.name,
        ProgressRequestBody(file, MediaType.parse("image/*").toString(), listener)
    )
}

//create the multipart body for video
@RequiresApi(Build.VERSION_CODES.KITKAT)
fun prepareFileVideoPart(partName: String, fileUri: String, listener: ProgressRequestBody.ProgressListener): MultipartBody.Part {
    val file = File(fileUri)
    return MultipartBody.Part.createFormData(
        partName,
        file.name,
        ProgressRequestBody(file, MediaType.parse("video/*").toString(), listener)
    )
}

//compress the video file using VideoCompress Module
fun compressVideoFile(mContext: Context, mPath: String, mCompressInterface: CompressInterface): String? {
    val mediaFile = getNewTempFile(mContext, "compressed", ".mp4")
    var destVideoPath: String? = null
    if (mediaFile?.exists() == true) {
        destVideoPath = mediaFile.absolutePath
        LogUtils.d(CommonTAG, "STARTS COMPRESSION - " + System.currentTimeMillis() + "\nPATH BEFORE COMPRESSION: " + destVideoPath)
        VideoCompress.compressVideoMedium(mPath, destVideoPath, object : VideoCompress.CompressListener {
            override fun onStart() {
            }

            override fun onSuccess() {
                mCompressInterface.compressVideoPath(destVideoPath,mContext)
            }

            override fun onFail() {

            }

            override fun onProgress(percent: Float) {
                mCompressInterface.getCompressionProgress(percent.toLong())
            }
        })
    } else {
        LogUtils.e(CommonTAG, mContext.resources.getString(R.string.file_not_create))
    }
    return destVideoPath
}

//create the new temp file for store the compress video file
fun getNewTempFile(context: Context, fileName: String, extension: String): File? {
    try {
        return File.createTempFile(fileName, extension, context.getExternalFilesDir("tmp"))
    } catch (ex: IOException) {
        // Error occurred while creating the File
        LogUtils.e(CommonTAG, ex.message, ex)
        return null
    }
}

//convert bitmap image
fun convertBitmapIntoFile(context: Context, bitmap: Bitmap): File {
    val file = File(context.cacheDir, BitmapFileName)
    file.createNewFile()
    val bos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
    val bData = bos.toByteArray()
    val fos = FileOutputStream(file)
    fos.write(bData)
    fos.flush()
    fos.close()
    return file
}

//upload media success or error interface
interface UploadingInterFace {
    fun getSuccessResponse(response: UploadMediaResponse?)
    fun getErrorResponse(error: String?)
}