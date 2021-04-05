package rs.highlande.app.tatatu.feature.createPost.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.bottom_bar_create_post.*
import kotlinx.android.synthetic.main.fragment_create_post.*
import kotlinx.android.synthetic.main.include_create_post_image_video.*
import kotlinx.android.synthetic.main.toolbar_create_post_2.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.CreatePost
import rs.highlande.app.tatatu.model.DataList
import rs.highlande.app.tatatu.model.DataListDisplay
import rs.highlande.app.tatatu.model.DataListVideo
import rs.highlande.app.tatatu.model.User
import java.io.File


/**
 * Created by Abhin.
 */

class CreatePostFragment : BaseFragment(), VideoPlayerHelper.IsVideoPauseByUser {

    override fun isVideoPauseByUser(isVideoPause: Boolean) {
        isVideoPauseByUser = isVideoPause
    }

    private var isVideoPauseByUser: Boolean = false
    private val createPostViewModel: CreatePostViewModel by sharedViewModel()

    private var mAdapter: CreatePostAdapter? = null
    private var mLayoutManager: GridLayoutManager? = null
    private var permissionHelper: PermissionHelper? = null

    private var mList = ArrayList<DataList>()
    private var mArrayFolderList = ArrayList<String>()
    private var mMasterArrayList = ArrayList<DataList>()
    private var mPath: String = ""
    private var oldPosition = -1
    private var currentPosition = -1
    private val mVideoCapture = 1
    private val mRequestCamera = 0
    private val mPermissionRequestCode = 10003

    private var isVideo = false
    private var mImagePath: String? = null
    private var mVideoPath: String? = null
    private var mCameraImageUri: Uri? = null
    private var mVideoPlayerHelper: VideoPlayerHelper? = null
    private var itemSelector: AdapterView.OnItemSelectedListener? = null
    private var mCreatePostBinding: CreatePost? = null
    private var mFirstTime: Boolean = true


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mCreatePostBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_create_post, container, false)
        mCreatePostBinding?.lifecycleOwner = this
        return mCreatePostBinding?.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init(savedInstanceState)
    }

    override fun onDestroyView() {
        createPostViewModel.clearObservers(activity!!)
        super.onDestroyView()
    }


    private fun initObserver() {
        with(createPostViewModel) {
            mList.observe(viewLifecycleOwner, Observer {
                if (!it.isNullOrEmpty()) {
                    this@CreatePostFragment.mList.clear()
                    this@CreatePostFragment.mList.addAll(it)
                    if (mFirstTime) {
                        mMasterArrayList.clear()
                        mMasterArrayList.addAll(it)
                        mFirstTime = false
                    }
                }
                mAdapter?.notifyDataSetChanged()
            })

            mFolderList.observe(viewLifecycleOwner, Observer {
                if (!it.isNullOrEmpty()) {
                    mArrayFolderList.clear()
                    mArrayFolderList.addAll(it)
                    initSpinner()
                }
            })

            observeOnPath(viewLifecycleOwner)
            observeOnIsVideo(viewLifecycleOwner)
        }
    }

    //set tht UI and get data
    private fun init(savedInstanceState: Bundle?) {
        //        img_tb_camera_arrow.visibility = View.VISIBLE
        txt_tb_title.text = resources.getString(R.string.tb_camera_roll)
        mVideoPlayerHelper = VideoPlayerHelper(exo_pv, context!!)
        mVideoPlayerHelper!!.setListener(this)
        initRecyclerView()
        initRequestPermission()
        initObserver()
    }

    //init RecyclerView
    private fun initRecyclerView() {
        mLayoutManager = GridLayoutManager(context!!, 3)
        rv_dashboard.layoutManager = mLayoutManager!!

        mAdapter = CreatePostAdapter(mList, object : CreatePostAdapter.ItemClickListener {
            override fun itemClick(position: Int) {
                mPath = mList[position].imagePath
                checkAndLoadImageOrVideo(position)
            }
        })

        rv_dashboard.adapter = mAdapter
        (rv_dashboard.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        mLayoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (position) {
                    0 -> mLayoutManager?.spanCount!!
                    else -> 1
                }
            }
        }

        setBehavior()
        setRecyclerView()
        txt_tb_next?.setOnClickListener {
            onClickNext()
        }

        img_tb_back?.setOnClickListener {
            activity!!.onBackPressed()
        }

        txt_bottom_lib?.setOnClickListener {
            mVideoPlayerHelper!!.stop()

        }
        txt_bottom_photo?.setOnClickListener {
            mVideoPlayerHelper!!.stop()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) initPermissionCamera() else cameraIntent()
        }
        txt_bottom_video?.setOnClickListener {
            mVideoPlayerHelper!!.stop()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) initPermissionVideo() else videoIntent()
        }

        exo_pv?.setOnClickListener {
            mVideoPlayerHelper!!.onClick(img_play)
        }
    }

    //set the Collapsing toolbar Behavior
    private fun setBehavior() {
        val params: CoordinatorLayout.LayoutParams = appbar_event_detail!!.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior = AppBarLayout.Behavior()
        val behavior: AppBarLayout.Behavior = params.behavior as AppBarLayout.Behavior

        behavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                return false
            }
        })

    }

    //manage the next btn click
    private fun onClickNext() {
        // assign values to ViewModel to be retrieved in Preview fragment
        createPostViewModel.setSelectionPath(mPath)
        createPostViewModel.setIsSelectionVideo(isVideo)
        if (isVideo) {
            mVideoPlayerHelper?.pause(img_play)
        } else {
            if (img_selected_image?.croppedBitmap?.bitmap != null) {
                createPostViewModel.croppedBitmap = img_selected_image?.croppedBitmap?.bitmap
            } else {
                showError(resources.getString(R.string.image_cropping))
            }
        }

        addReplaceFragment(R.id.fl_container, CreatePostPreviewFragment.newInstance(null), addFragment = true, addToBackStack = true, animationHolder = NavigationAnimationHolder())
    }

    //get the permission for Video
    private fun initPermissionVideo() {
        val permissions: Array<String> = if (isWriteStoragePermissionGranted()) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        permissionHelper = PermissionHelper(this, permissions, mPermissionRequestCode)
        permissionHelper!!.request(object : PermissionHelper.PermissionCallback {
            override fun onPermissionGranted() {
                videoIntent()
            }

            override fun onPermissionDenied() {

            }

            override fun onPermissionDeniedBySystem() {

            }
        })
    }

    //get the permission for Camera
    private fun initPermissionCamera() {
        val permissions: Array<String> = if (isWriteStoragePermissionGranted()) {
            arrayOf(Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        permissionHelper = PermissionHelper(this, permissions, mPermissionRequestCode)
        permissionHelper!!.request(object : PermissionHelper.PermissionCallback {
            override fun onPermissionGranted() {
                cameraIntent()
            }

            override fun onPermissionDenied() {

            }

            override fun onPermissionDeniedBySystem() {

            }
        })
    }

    private fun isWriteStoragePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    //get permission for Read and Write External Storage
    @SuppressLint("InlinedApi")
    private fun initRequestPermission() {
        permissionHelper = PermissionHelper(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), mPermissionRequestCode)
        permissionHelper!!.request(object : PermissionHelper.PermissionCallback {
            override fun onPermissionGranted() {
                this@CreatePostFragment.createPostViewModel.loadMedia(context!!, this@CreatePostFragment)
            }

            override fun onPermissionDenied() {

            }

            override fun onPermissionDeniedBySystem() {

            }
        })
    }

    //set the Spinner
    private fun initSpinner() {
        txt_tb_title?.setOnClickListener {
            spinner_folder_list?.performClick()
        }

        if (!mArrayFolderList.isNullOrEmpty()) {
            mArrayFolderList.sort()
            mArrayFolderList.add(0, GALLERY_LABEL)
            //set color for selected text
            val adapter = object : ArrayAdapter<String>(context!!, R.layout.item_spinner, mArrayFolderList) {
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val mTextViewSpinner = super.getDropDownView(position, convertView, parent) as TextView
                    mTextViewSpinner.setTextColor(resolveColorAttribute(context, R.attr.textColorPrimary))
                    if (position == oldPosition) {
                        mTextViewSpinner.setTextColor(resolveColorAttribute(context, R.attr.colorAccent))
                    }
                    return mTextViewSpinner
                }
            }
            spinner_folder_list?.adapter = adapter
            //Listener changing for data
            if (itemSelector == null) {
                spinner_folder_list?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        oldPosition = position
                        val selectedFolder = parent.adapter.getItem(position).toString()
                        mList.clear()
                        if (!selectedFolder.equals(GALLERY_LABEL, true)) {
                            for (e in mMasterArrayList) {
                                LogUtils.d(logTag, "-->${e.id}")
                                if (e.id.equals(selectedFolder, true)) {
                                    mList.add(e)
                                }
                            }
                        } else {
                            this@CreatePostFragment.createPostViewModel.mList.value = mMasterArrayList
                        }

                        mAdapter?.notifyDataSetChanged()

                        if (!mList.isNullOrEmpty()) {
                            mList.add(0, DataListDisplay())
                            txt_tb_title.text = selectedFolder
                            mList[1].isSelected = true //default first selected
                            mAdapter?.oldPosition = 1 //default first selected
                            checkAndLoadImageOrVideo(1)
                        } else {
                            showError(resources.getString(R.string.content_not_available))
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {

                    }
                }
            }
        }
    }


    //callback for permission
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionHelper != null) {
            permissionHelper!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    //video capture open intent
    private fun videoIntent() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivityForResult(intent, mVideoCapture)
    }


    //open the Camera
    private fun cameraIntent() {
        val values = ContentValues()
        values.put(CAMERA_INTENT_TITLE, System.currentTimeMillis().toString() + resources.getString(R.string.app_name) + CAMERA_INTENT_TYPE)
        mCameraImageUri = activity!!.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val intentCamera = Intent("android.media.action.IMAGE_CAPTURE")
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, mCameraImageUri)
        startActivityForResult(intentCamera, mRequestCamera)
    }

    /**
     * getting result and set the arrayList and call adapter
     */
    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (resultCode) {
            Activity.RESULT_OK -> {
                resetUI()
                when (requestCode) {
                    mRequestCamera -> {
                        if (mCameraImageUri != null) {
                            mImagePath = PathUtil.getPath(context!!, mCameraImageUri!!)
                            changeUI(false)
                            loadImage(0, mImagePath)
                        }
                    }
                    mVideoCapture -> {
                        if (mVideoPlayerHelper!!.isPlaying()) mVideoPlayerHelper!!.stop()

                        mVideoPath = PathUtil.getPath(context!!, data!!.data!!)
                        changeUI(true)
                        mVideoPlayerHelper!!.videoPlay(mVideoPath, img_play)
                        mPath = mVideoPath!!
                    }
                }
                mAdapter?.deSelected()
                mAdapter?.oldPosition = -1
            }
            Activity.RESULT_CANCELED -> {
                when (requestCode) {
                    mVideoCapture -> showError(resources.getString(R.string.videoCancel))
                }
            }
            else -> {
                when (requestCode) {
                    mVideoCapture -> showError(resources.getString(R.string.videoFailed))
                }
            }
        }
        /*if (requestCode == mRequestCamera) run {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    if (mCameraImageUri != null && data != null) {
                        try {
                            mImagePath = PathUtil.getPath(context!!, mCameraImageUri!!)
                            resetUI()
                            changeUI(false)
                            loadImage(0, mImagePath)
                            mAdapter?.deSelected()
                            mAdapter?.oldPosition = -1
                        } catch (e: URISyntaxException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } else if (requestCode == mVideoCapture && data != null) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    if (mVideoPlayerHelper!!.isPlaying()) {
                        mVideoPlayerHelper!!.stop()
                    }
                    mVideoPath = PathUtil.getPath(context!!, data!!.data!!)
                    resetUI()
                    changeUI(true)
                    mVideoPlayerHelper!!.videoPlay(mVideoPath, img_play)
                    mPath = mVideoPath!!
                    mAdapter?.deSelected()
                    mAdapter?.oldPosition = -1
                }
                Activity.RESULT_CANCELED -> showError(resources.getString(R.string.videoCancel))
                else -> {
                    showError(resources.getString(R.string.videoFailed))
                }
            }
        }*/
    }


    //set the image in copper with bitmap help of glide
    private fun loadImage(position: Int, imagePath: String? = null) {
        if (imagePath == null) {
            mPath = mList[position].imagePath
            val mFile = File(mPath)
            img_selected_image?.setImageBitmap(null) // remove old image
            if ((mList[position].imageWidth ?: 0) >= 1280 || (mList[position].imageHeight ?: 0) >= 1280) {
                Glide.with(context!!).asBitmap().apply(RequestOptions().override(1280)).load(mFile).into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        createPostViewModel.isBitmapToGlide = true
                        img_selected_image?.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }
                })

            } else { //load without override
                Glide.with(context!!).asBitmap().load(mFile).into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        createPostViewModel.isBitmapToGlide = true
                        img_selected_image?.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }
                })
            }
        } else { //load from Activity Result
            Glide.with(context!!).asBitmap().load(imagePath).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    createPostViewModel.isBitmapToGlide = true
                    img_selected_image?.setImageBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }
            })
        }
    }

    //set video play and pause on show
    private fun setRecyclerView() {
        rv_dashboard.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (mLayoutManager?.findFirstCompletelyVisibleItemPosition()!! != 0 && mVideoPlayerHelper!!.isPlaying()) {
                    mVideoPlayerHelper!!.pause(img_play)
                }

                if (mLayoutManager?.findFirstCompletelyVisibleItemPosition()!! == 0 && isVideo && !isVideoPauseByUser) {
                    mVideoPlayerHelper!!.resume(img_play)
                }

            }
        })
    }

    // scroll to top
    private fun resetUI() {
        img_play.visibility = View.GONE
        rv_dashboard.scrollToPosition(0)
        appbar_event_detail.setExpanded(true, true)
    }

    //check the file are video or image
    fun checkAndLoadImageOrVideo(position: Int) {
        createPostViewModel.setHeightWidth(mList[position].imageHeight ?: 0, mList[position].imageWidth ?: 0)
        resetUI()
        currentPosition = position
        if (mVideoPlayerHelper != null && mVideoPlayerHelper!!.isPlaying()) {
            mVideoPlayerHelper!!.stop()
        }
        if (mList[position] is DataListVideo) {
            changeUI(true)
            mVideoPlayerHelper!!.videoPlay(mList[position].imagePath, img_play)
            mPath = mList[position].imagePath
        } else {
            changeUI(false)
            loadImage(position)
        }
    }

    //Change UI
    private fun changeUI(isVideo: Boolean) {
        if (isVideo) {
            exo_pv.visibility = View.VISIBLE
            img_selected_image.visibility = View.INVISIBLE
            this.isVideo = true
        } else {
            exo_pv.visibility = View.INVISIBLE
            img_selected_image.visibility = View.VISIBLE
            this.isVideo = false
        }
    }

    override fun onPause() {
        mVideoPlayerHelper!!.pause(img_play)
        super.onPause()
    }


    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)
    }


    override fun onResume() {
        super.onResume()
        mVideoPlayerHelper!!.resume(img_play)
    }


    override fun configureLayout(view: View) {}

    override fun bindLayout() {}

    override fun observeOnMyUserAction(user: User) {
        super.observeOnMyUserAction(user)

        createPostViewModel.initPost(user)
    }


    companion object {
        val logTag = CreatePostFragment::class.java.simpleName
        const val GALLERY_LABEL = "Gallery"
        const val CAMERA_INTENT_TITLE = "title"
        const val CAMERA_INTENT_TYPE = "image"

        fun newInstance(): CreatePostFragment {

            val fragment = CreatePostFragment()
            val args = Bundle()

            // TODO: 2019-07-08    put args

            return fragment.apply { arguments = args }

        }

    }

}