package rs.highlande.app.tatatu.feature.createPost.view

/**
 * Created by Abhin.
 */
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.bottom_bar_post_preview.*
import kotlinx.android.synthetic.main.bottom_bar_post_preview.view.*
import kotlinx.android.synthetic.main.common_toolbar_back.*
import kotlinx.android.synthetic.main.fragment_create_post_preview.*
import kotlinx.android.synthetic.main.include_create_post_preview.*
import org.greenrobot.eventbus.EventBus
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.feature.chat.ActionType
import rs.highlande.app.tatatu.model.Post
import rs.highlande.app.tatatu.model.PostType
import rs.highlande.app.tatatu.model.createPost.CreatePostRequest
import rs.highlande.app.tatatu.model.event.PostChangeEvent


class CreatePostPreviewFragment : BaseFragment() {

    private val createPostViewModel: CreatePostViewModel by sharedViewModel()

    private var isVideo: Boolean = false
    private var mVideoPlayerHelper: VideoPlayerHelper? = null
    private var mMediaType = "Photo"
    private var isEditPost = false
    private var tbTitle = ""
    private var post: Post? = null
    private var btnClick = false
    private var position = -1
    private var mCaption = ""


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        tbTitle = resources.getString(R.string.tb_new_post)
        return inflater.inflate(R.layout.fragment_create_post_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureLayout(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        createPostViewModel.caption.value?.let {
            edt_write_a_caption.setText(createPostViewModel.caption.value)
        } ?: edt_write_a_caption.setText("")

        isVideo = savedInstanceState?.getBoolean(KEY_SELECTED_VIDEO, false) ?: false
        init()
    }

    private fun init() {
        if (arguments != null && arguments!!.getParcelable<Post>(BUNDLE_KEY_EDIT_POST) == null) {
            btn_video.text = resources.getString(R.string.btn_publish)
        } else {
            isEditPost = arguments!!.getBoolean(BUNDLE_KEY_IS_EDIT)
            position = arguments!!.getInt(BUNDLE_KEY_POSITION)

            post = arguments!!.getParcelable(BUNDLE_KEY_EDIT_POST) as? Post
            setEditPostUI(post!!)
        }
        initData()
        initObserver()
        backArrow.setOnClickListener {
            if (!isEditPost) {
                fragmentManager?.popBackStack()
            } else {
                mBaseActivity?.onBackPressed()
            }
        }
    }

    //set UI for edit post
    private fun setEditPostUI(post: Post?) {
        isEditPost = true
        post?.let {
            isVideo = it.type == PostType.VIDEO
            createPostViewModel.selectedPath.value = it.preview
            createPostViewModel.isSelectedVideo.value = isVideo
            createPostViewModel.croppedBitmap = null
            createPostViewModel.caption.value = it.caption
            edt_write_a_caption.setText(it.caption!!)
        }

        tbTitle = resources.getString(R.string.tb_update_post)
        title.text = tbTitle
        btn_video.text = resources.getString(R.string.btn_edit_post_save)
    }

    //get Data from Bundle
    private fun initData() {
        isVideo = createPostViewModel.isSelectedVideo.value ?: false
        mMediaType = if (isVideo) "video" else "photo"
        setUI()
    }

    //set the UI
    private fun setUI() {
        img_new_post_image.visibility = View.VISIBLE
        when {
            createPostViewModel.croppedBitmap != null -> img_new_post_image.setPicture(createPostViewModel.croppedBitmap)
            createPostViewModel.selectedPath.value != null -> img_new_post_image.setPicture(createPostViewModel.selectedPath.value)
            else -> showError(getString(R.string.error_generic))
        }
        if (isVideo) {
            initVideoUI()
        }
    }

    //set VideoUI
    private fun initVideoUI() {
        exo_pv.visibility = View.VISIBLE
        img_new_post_image.visibility = View.INVISIBLE
        mVideoPlayerHelper = VideoPlayerHelper(exo_pv, context!!)
        mVideoPlayerHelper?.let { videoPlayerHelper ->
            createPostViewModel.selectedPath.value?.let {
                videoPlayerHelper.videoPlay(it, img_play)
            }
            videoPlayerHelper.pause(img_play)
            img_play?.setOnClickListener {
                videoPlayerHelper.onClick(img_play)
            }
            exo_pv.setOnClickListener {
                videoPlayerHelper.onClick(img_play)
            }
            img_new_post_image.setOnClickListener {
                videoPlayerHelper.onClick(img_play)
            }
        }
    }

    private fun initObserver() {
        createPostViewModel.mUploadImageResponse.observe(viewLifecycleOwner, Observer {
            if (it != null && it.responseStatus.equals("Ok", true)) {
                val mCreatePostRequest = CreatePostRequest()
                mCreatePostRequest.caption = createPostViewModel.caption.value
                mCreatePostRequest.type = mMediaType
                mCreatePostRequest.preview = it.mData?.preview
                mCreatePostRequest.mediaItems.mediaType = mMediaType
                mCreatePostRequest.mediaItems.mediaEndpoint = it.mData?.original
                mCreatePostRequest.mediaItems.scale = "3"
            }
        })

        createPostViewModel.mProgressShow.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                if (it.first && it.second != null) {
                    showLoader(
                        when (it.second) {
                            ActionType.COMPRESS -> getString(
                                R.string.progress_video_compress,
                                it.third
                            )
                            ActionType.UPLOAD -> getString(R.string.progress_media_upload, it.third)
                            ActionType.CREATE -> getString(R.string.progress_post_create)
                            else -> getString(R.string.progress_post_edit)
                        }
                    )
                } else {
                    hideLoader()
                }
            }
        })

        createPostViewModel.actionSuccess.observe(viewLifecycleOwner, Observer {
            if (it.first && (it.second == ActionType.EDIT || it.second == ActionType.CREATE)) {
                mBaseActivity?.let{ activity ->
                    activity.finish()
                    activity.overridePendingTransition(R.anim.no_animation, R.anim.slide_out_to_right)
                }
            }
        })

        createPostViewModel.actionError.observe(viewLifecycleOwner, Observer {
            if (it.first)
                showError(getString(R.string.error_generic))
        })

        createPostViewModel.observeOnCaption(viewLifecycleOwner)

        createPostViewModel.errorOnRx.observe(viewLifecycleOwner, Observer {
            hideLoader()
            it?.let {
                showError(getString(it))
            }
        })
    }

    //EventBus for updating data previous screen
    override fun onDestroyView() {
        if (btnClick) {
            if (position != -1) {
                EventBus.getDefault().post(PostChangeEvent(post, position))
            } else {
                EventBus.getDefault().post(PostChangeEvent(post))
            }
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
        if (mVideoPlayerHelper != null) mVideoPlayerHelper!!.stop()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

        title.text = tbTitle
    }

    override fun configureLayout(view: View) {
        ll_post_preview.btn_video.setOnClickListener {

            btnClick = true

            createPostViewModel.doPostAction(
                if (isEditPost) ActionType.EDIT else ActionType.UPLOAD,
                mCaption,
                post,
                isVideo
            )
        }

        addDisposable(
            edt_write_a_caption.textChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        LogUtils.d(logTag, "POST CAPTION CHANGED: $it")
                        mCaption = it.toString().trim()
                        createPostViewModel.setCaption(it.toString().trim())
                    },
                    {
                        it.printStackTrace()
                        mCaption = ""; createPostViewModel.setCaption("")
                    }
                )
        )
    }

    override fun bindLayout() {}

    companion object {
        const val KEY_SELECTED_VIDEO = "key_is_video"

        val logTag = CreatePostPreviewFragment::class.java.simpleName

        fun newInstance(mBundle: Bundle?): CreatePostPreviewFragment {
            val instance = CreatePostPreviewFragment()
            if (mBundle == null) {
                val bundle = Bundle()
                instance.arguments = bundle
            } else {
                instance.arguments = mBundle
            }
            return instance
        }
    }

}