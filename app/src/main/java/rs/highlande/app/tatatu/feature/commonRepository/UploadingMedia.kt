package rs.highlande.app.tatatu.feature.commonRepository

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import net.ypresto.qtfaststart.QtFastStart
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.Response
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.http.ProgressRequestBody
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.ui.BaseViewModel
import rs.highlande.app.tatatu.core.util.FILE_BODY_PART
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.isValid
import rs.highlande.app.tatatu.feature.createPost.repository.CreatePostRepository
import java.io.File
import java.io.IOException

/**
 * Created by Abhin.
 */
class UploadingMedia(
    private var mUploadingInterFace: UploadingInterFace,
    val mContext: Context?,
    var mPath: String? = null,
    var mBitmap: Bitmap? = null,
    var isVideo: Boolean = false,
    var mUploadType: String = "1"
) : BaseViewModel(), Callback {

    private lateinit var mMediaType: String
    private var mFile: File? = null
    private val repo: CreatePostRepository by inject()
    private val usersRepo: UsersRepository by inject()

    //Data Uploading
    fun uploadingData(listener: ProgressRequestBody.ProgressListener, redirectProgressListener: CompressInterface) {
        if (!isVideo) {
            mMediaType = "photo"
            if (mBitmap != null) {
                mContext?.let {
                    addDisposable(
                        Observable.just<Bitmap>(mBitmap)
                            .map {
                                convertBitmapIntoFile(mContext, it)
                            }
                            .observeOn(Schedulers.computation())
                            .subscribeOn(Schedulers.computation())
                            .subscribe({
                                getUpload(
                                    usersRepo.fetchCachedMyUserId(),
                                    mMediaType,
                                    mUploadType,
                                    prepareFileImagePart(FILE_BODY_PART, it!!.absolutePath, listener),
                                    mBitmap?.height!!,
                                    mBitmap?.width!!
                                )
                            }, { thr ->
                                thr.printStackTrace()
                                mUploadingInterFace.getErrorResponse("")
                                if (mContext.isValid() && mContext is BaseActivity)
                                    mContext.showError(mContext.getString(R.string.error_upload_media))
                            })
                    )
                }
            } else {
                mFile = File(mPath!!)
                getUpload(
                    usersRepo.fetchCachedMyUserId(),
                    mMediaType,
                    mUploadType,
                    prepareFileImagePart(FILE_BODY_PART, mFile!!.absolutePath, listener)
                )
            }
        } else {
            if (mContext?.isValid() == true) {
                mMediaType = "video"
                mFile = File(mPath!!)
                compressVideoFile(mContext, mPath!!, object : CompressInterface {
                    override fun compressVideoPath(path: String?, mContext: Context) {
                        mFile = File(path!!)

                        replaceMoovAtom(listener)

                    }

                    override fun getCompressionProgress(progress: Long) {
                        redirectProgressListener.getCompressionProgress(progress)
                    }
                })
            }
        }
    }

    //call upload media
    fun getUpload(
        xID: String,
        mediaType: String,
        uploadType: String,
        files: MultipartBody.Part,
        height: Int = 0,
        width: Int = 0
    ) {
        addDisposable(
            repo.getMediaUpload(xID, mediaType, uploadType, files)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {
                        if (it != null && it.responseCode == 0) {
                            it.height = height
                            it.width = width
                            it.mediaType = mMediaType
                            mUploadingInterFace.getSuccessResponse(it)
                        } else {
                            mUploadingInterFace.getErrorResponse(it.responseError)
                        }
                    },
                    { thr ->
                        thr.printStackTrace()
                        mUploadingInterFace.getErrorResponse("")
                        if ((mContext?.isValid() == true) && mContext is BaseActivity)
                            mContext.showError(mContext.getString(R.string.error_upload_media))
                    }
                )
        )
    }


    private fun replaceMoovAtom(listener: ProgressRequestBody.ProgressListener) {

        mContext?.let {
            if (it.isValid()) {

                val newFile = getNewTempFile(mContext, "compressed-opt", ".mp4")

                if (mFile != null && newFile?.exists() == true) {
                    addDisposable(
                        Observable.just<Boolean>(QtFastStart.fastStart(mFile, newFile))
                            .subscribeOn(Schedulers.computation())
                            .observeOn(Schedulers.computation())
                            .subscribe(
                                { result ->

                                    LogUtils.d(logTag, "Moov atom replacement: $result")

                                    mFile = newFile

                                    getUpload(
                                        usersRepo.fetchCachedMyUserId(),
                                        mMediaType,
                                        mUploadType,
                                        prepareFileVideoPart(FILE_BODY_PART, mFile!!.absolutePath, listener)
                                    )

                                },
                                { thr ->
                                    thr.printStackTrace()
                                    mUploadingInterFace.getErrorResponse("")
                                    if (mContext is BaseActivity)
                                        mContext.showError(mContext.getString(R.string.error_upload_media))
                                }
                            )
                    )
                }
            }
        }
    }


    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf()
    }

    /*
    //call upload media
    fun getUpload(
        xID: String,
        mediaType: String,
        uploadType: String,
        file: File,
        listener: ProgressRequestBody.ProgressListener
    ) {
        addDisposable(
            uploadUsingOKHTTP(xID, mediaType, uploadType, file, listener)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {
                        LogUtils.d("UPLOAD MEDIA", "SUCCESS for file ${file.name}")
                    },
                    { thr ->
                        thr.printStackTrace()
                        if ((mContext?.isValid() == true) && mContext is BaseActivity)
                            mContext.showError(mContext.getString(R.string.error_upload_media))
                    }
                )
        )
    }

    private fun uploadUsingOKHTTP(xID: String,
                                  mediaType: String,
                                  uploadType: String,
                                  file: File,
                                  listener: ProgressRequestBody.ProgressListener
    ): Observable<Boolean> {
        val client = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .build()

        try {
            val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(FILE_BODY_PART,
                    file.name,
                    ProgressRequestBody(file, mediaType, listener)
                )
                .build()

            val request = Request.Builder().url("http://ttudev.highlanders.app:5000/api/UploadMedia")
                .addHeader(PARAM_X_ID, xID)
                .addHeader(PARAM_X_MEDIA_TYPE, mediaType)
                .addHeader(PARAM_X_UPLOAD_TYPE, uploadType)
                .post(requestBody).build()


            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    LogUtils.d("UPLOAD MEDIA", "FAILURE for file ${file.name} with exception: ${e.message}")

                }

                override fun onResponse(call: Call, response: Response) {
                    LogUtils.d("UPLOAD MEDIA", "SUCCESS for file ${file.name} with upload response: ${response.body()}")
                }

            })

            return Observable.just(true)

        } catch (e: Exception) {
            LogUtils.e("", e.message, e)
            return Observable.just(false)
        }

    }
    */


    // INFO: 2019-09-02    following 2 methods are here because of TEST implementation of okhttp.Callback

    override fun onFailure(call: Call, e: IOException) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onResponse(call: Call, response: Response) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    companion object {
        val logTag = UploadingMedia::class.java.simpleName
    }


}
