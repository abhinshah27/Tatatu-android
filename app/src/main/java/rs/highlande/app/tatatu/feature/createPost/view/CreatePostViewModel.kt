package rs.highlande.app.tatatu.feature.createPost.view

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.gson.Gson
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.koin.core.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.http.ProgressRequestBody
import rs.highlande.app.tatatu.connection.webSocket.RequestStatus
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_CREATE_POST
import rs.highlande.app.tatatu.connection.webSocket.SERVER_OP_EDIT_POST
import rs.highlande.app.tatatu.core.ui.BaseAndroidViewModel
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.nowToDBDate
import rs.highlande.app.tatatu.feature.account.profile.repository.ProfileRepository
import rs.highlande.app.tatatu.feature.chat.ActionType
import rs.highlande.app.tatatu.feature.commonRepository.*
import rs.highlande.app.tatatu.feature.createPost.repository.CreatePostRepository
import rs.highlande.app.tatatu.feature.createPost.repository.ImageVideoLoadHelper
import rs.highlande.app.tatatu.feature.home.repository.HomeRepository
import rs.highlande.app.tatatu.model.*
import rs.highlande.app.tatatu.model.createPost.uploadImage.UploadMediaResponse

/**
 * Created by Abhin.
 */
class CreatePostViewModel(application: Application) : BaseAndroidViewModel(application), UploadingInterFace {

    private val postRepository: PostRepository by inject()
    // TODO: 2019-07-09    TEST
    private val homeRepository: HomeRepository by inject()
    private val profileRepository: ProfileRepository by inject()
    private val repo: CreatePostRepository by inject()

    var mList = MutableLiveData<ArrayList<DataList>>()
    var mFolderList = MutableLiveData<ArrayList<String>>()
    var selectedPath = MutableLiveData<String>()
    var height = MutableLiveData<Int>()
    var width = MutableLiveData<Int>()
    var isSelectedVideo = MutableLiveData<Boolean>()
    var caption = MutableLiveData<String>()

    val actionSuccess: MutableLiveData<Pair<Boolean, ActionType?>> = mutableLiveData(false to null)
    val actionError: MutableLiveData<Pair<Boolean, ActionType?>> = mutableLiveData(false to null)

    var croppedBitmap: Bitmap? = null
    var mUploadImageResponse = MutableLiveData<UploadMediaResponse>()

    var isBitmapToGlide = false


    // INFO: 2019-08-01    every LiveData should be handled like this
    private val _progress: MutableLiveData<Triple<Boolean, ActionType?, Long>> = mutableLiveData(Triple(false, null, 0L))
    val mProgressShow: LiveData<Triple<Boolean, ActionType?, Long>>
        get() = _progress


    /**
     * Holds the reference of the [Post] to create/edit across configuration changes and data transfer.
     */
    private var mPost: Post? = null


    //Loading media Files
    fun loadMedia(mContext: Context, mFragment: Fragment) {
        ImageVideoLoadHelper(mContext).getAllImagesAndVideos(mFragment, object : ImageVideoLoadHelper.DataLoadComplete {
            override fun getAllData(mDataArrayList: ArrayList<DataList>, mDataArrayFolderList: ArrayList<String>) {
                mList.value = mDataArrayList
                mFolderList.value = mDataArrayFolderList
            }
        })
    }

    fun getPost(): Post? = mPost
    fun setPost(post: Post) {
        mPost = post
    }

    fun savePost(uid: String) {

        if (mPost != null) {

            mPost!!.uid = uid

            // TODO: 2019-07-09    REMOVE -> TEST
            postRepository.savePost(mPost!!)
            homeRepository.savePost(mPost!!)
            profileRepository.savePost(mPost!!)
        }
    }


    fun setCaption(caption: String) {
        this.caption.value = caption
    }

    fun setSelectionPath(path: String) {
        selectedPath.value = path
    }

    //getting Height width form list
    fun setHeightWidth(height: Int, width: Int) {
        this.height.value = height
        this.width.value = width
    }

    fun setIsSelectionVideo(isVideo: Boolean) {
        isSelectedVideo.value = isVideo
    }

    fun observeOnPath(owner: LifecycleOwner) {
        selectedPath.observe(owner, Observer {

            if (mPost != null) {
                // TODO: 2019-07-09    the logic applies only to single-media Post
                if (mPost!!.mediaItems.isEmpty()) mPost?.mediaItems?.add(PostMediaItem())

                mPost!!.mediaItems[0].tmpUri = Uri.parse(it)
            }
        })
    }

    fun observeOnIsVideo(owner: LifecycleOwner) {
        isSelectedVideo.observe(owner, Observer {

            if (mPost != null) {
                // TODO: 2019-07-09    the logic applies only to single-media Post
                if (mPost!!.mediaItems.isEmpty()) mPost!!.mediaItems.add(PostMediaItem())

                mPost!!.mediaItems[0].mediaType = if (it) MediaType.VIDEO else MediaType.IMAGE
                mPost!!.type = if (it) PostType.VIDEO else PostType.IMAGE
            }
        })
    }

    fun observeOnCaption(owner: LifecycleOwner) {
        caption.observe(owner, Observer { mPost?.caption = it })
    }

    fun initPost(owner: User) {
        if (mPost == null) {
            mPost = Post().apply {
                userData = owner
            }
        }
    }


    override fun onCleared() {
        try {
            if (!isBitmapToGlide && croppedBitmap?.isRecycled == false) croppedBitmap!!.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onCleared()
    }

    override fun getAllObservedData(): Array<MutableLiveData<*>> {
        return arrayOf(mList, mFolderList, selectedPath, isSelectedVideo)
    }

    //handle the api call success
    override fun handleSuccessResponse(idOp: String, callCode: Int, response: JSONArray?) {
        super.handleSuccessResponse(idOp, callCode, response)

        _progress.postValue(Triple(false, null, 0))

        when (callCode) {

            SERVER_OP_CREATE_POST -> {
                LogUtils.d(CommonTAG, "Create Post Success Response--> ${Gson().toJson(response)}")
                actionSuccess.postValue(true to ActionType.CREATE)
            }
            SERVER_OP_EDIT_POST -> {
                LogUtils.d(CommonTAG, "Edit Post Success Response--> ${Gson().toJson(response)}")
                actionSuccess.postValue(true to ActionType.EDIT)
            }
        }

    }

    //error response from Api Calling
    override fun handleErrorResponse(idOp: String, callCode: Int, errorCode: Int, description: String?) {
        super.handleErrorResponse(idOp, callCode, errorCode, description)

        _progress.postValue(Triple(false, null, 0))

        when (callCode) {
            SERVER_OP_CREATE_POST -> {
                LogUtils.d(CommonTAG, "Create Post error Response--> ${Gson().toJson(description)}")
                actionError.postValue(true to ActionType.CREATE)
            }
            SERVER_OP_EDIT_POST -> {
                LogUtils.d(CommonTAG, "Edit Post error Response--> ${Gson().toJson(description)}")
                actionError.postValue(true to ActionType.EDIT)
            }
        }

    }

    //Getting Success of Upload Media After this we upload the
    override fun getSuccessResponse(response: UploadMediaResponse?) {
        LogUtils.d(CommonTAG, "Success Code-->${Gson().toJson(response)}")
        val bundle = getPostData(response!!)

        _progress.postValue(Triple(true, ActionType.CREATE, 0))

        //call the create post
        addDisposable(repo.getCreatePost(this, bundle).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            if (it == RequestStatus.SENT) LogUtils.d(logTag, "Create post call: SUCCESS")
            else LogUtils.e(logTag, "Create post call: FAILED with $it")
        }, { thr ->
            thr.printStackTrace()
            errorOnRx.postValue(R.string.error_generic)
        }))
    }

    //add post data
    private fun getPostData(response: UploadMediaResponse): Bundle {
        mPost?.preview = response.mData?.preview!!
        mPost?.mediaItems?.get(0)?.mediaEndpoint = response.mData?.original!!

        if (response.height != 0 || response.width != 0) {
            mPost?.mediaItems?.get(0)?.size?.height = response.height!!
            mPost?.mediaItems?.get(0)?.size?.width = response.width!!
        } else {
            mPost?.mediaItems?.get(0)?.size?.height = height.value!!
            mPost?.mediaItems?.get(0)?.size?.width = width.value!!
        }

        setMediaType(response.mediaType)
        setType(response.mediaType)

        return Bundle().apply {
            putSerializable("serializable1", getPost())
        }
    }


    fun doPostAction(action: ActionType, caption: String?, postToEdit: Post?, isVideo: Boolean) {
        when (action) {
            ActionType.UPLOAD -> {
                _progress.value = (Triple(true, ActionType.UPLOAD, 0))
                setCaption(caption ?: "")
                mPost?.date = nowToDBDate()
                getUploadMedia(isVideo)
            }
            ActionType.EDIT -> {
                if (postToEdit != null) {

                    _progress.postValue(Triple(true, ActionType.EDIT, 0))

                    editPost(postToEdit, caption ?: "")
                }
            }
            else -> {
            }
        }
    }


    private fun getUploadMedia(isVideo: Boolean) {
        UploadingMedia(this, getApplication(), selectedPath.value, croppedBitmap, isVideo).uploadingData(object : ProgressRequestBody.ProgressListener {
            override fun transferred(num: Long) {
                _progress.postValue(Triple(true, ActionType.UPLOAD, num))
            }

            override fun exceptionCaught(e: Exception) {
                _progress.postValue(Triple(false, null, 0))
                actionError.postValue(true to ActionType.UPLOAD)
            }
        }, object : CompressInterface {
            override fun compressVideoPath(path: String?, mContext: Context) {
                //no op
            }

            override fun getCompressionProgress(progress: Long) {
                _progress.postValue(Triple(true, ActionType.COMPRESS, progress))
            }
        })
    }

    //call edit post data
    private fun editPost(response: Post, caption: String) {
        response.caption = caption
        val bundle = Bundle().apply {
            putSerializable("serializable1", response)
        }

        addDisposable(repo.getEditPost(this, bundle).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe({
            if (it == RequestStatus.SENT) LogUtils.d(logTag, "Edit post call: SUCCESS")
            else LogUtils.e(logTag, "Edit post call: FAILED with $it")
        }, { thr ->
            thr.printStackTrace()
            errorOnRx.postValue(R.string.error_generic)
        }))
    }

    //set type according to mMediaType
    private fun setType(mediaType: String?) {
        when (mediaType) {
            "photo" -> mPost?.type = PostType.IMAGE
            "video" -> mPost?.type = PostType.VIDEO
        }
    }

    //set media type according to mMediaType
    private fun setMediaType(mediaType: String?) {
        when (mediaType) {
            "photo" -> mPost?.mediaItems?.get(0)?.mediaType = MediaType.IMAGE
            "video" -> mPost?.mediaItems?.get(0)?.mediaType = MediaType.VIDEO
        }
    }

    //error response from video compressing
    override fun getErrorResponse(error: String?) {
        LogUtils.d(CommonTAG, "Error Code-->$error")
    }


    companion object {
        val logTag = CreatePostViewModel::class.java.simpleName
    }

}