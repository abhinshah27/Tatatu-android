package rs.highlande.app.tatatu.feature.post

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.ActivityAnimationHolder
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.ActivityPostBinding
import rs.highlande.app.tatatu.feature.commonApi.CommonApi
import rs.highlande.app.tatatu.feature.main.view.MainActivity
import rs.highlande.app.tatatu.feature.post.detail.fragment.MyPostDetailFragment
import rs.highlande.app.tatatu.feature.post.detail.fragment.NewsPostDetailFragment
import rs.highlande.app.tatatu.feature.post.detail.fragment.PostDetailFragment
import rs.highlande.app.tatatu.feature.post.detail.viewModel.PostDetailViewModel
import rs.highlande.app.tatatu.feature.post.like.fragment.PostLikeFragment
import rs.highlande.app.tatatu.feature.post.timeline.fragment.PostTimelineFragment

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class PostActivity : BaseActivity() {

    private lateinit var binding: ActivityPostBinding
    private val postViewModel: PostDetailViewModel by viewModel()
    private var startedFromNotification: Boolean = false
    private var appForeground: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_post)
        binding.lifecycleOwner = this

        if (savedInstanceState == null) {
            manageIntent()
        }
    }


    override fun configureLayout() {}

    override fun bindLayout() {}

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        manageIntent(intent)
    }

    override fun manageIntent() {
        val intent = intent ?: return
        manageIntent(intent)
    }

    fun manageIntent(newIntent: Intent?) {
        val intent = newIntent ?: return

        val showFragment = intent.getIntExtra(FRAGMENT_KEY_CODE, FRAGMENT_INVALID)
        val extras = intent.extras
        showFragment(showFragment, extras)

    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount <= 1) {
            if (startedFromNotification) {
                if (!appForeground)
                    startActivity(Intent(this, MainActivity::class.java))

                setResult(Activity.RESULT_OK)
                finish()

                if (!appForeground)
                    overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out)
            } else {
                finish()
            }
        } else {
            supportFragmentManager.popBackStack()
        }
    }


    private fun showFragment(showFragment: Int, extras: Bundle?) {
        when (showFragment) {

            FRAGMENT_POST_TIMELINE -> {
                val userId = extras?.getString(BUNDLE_KEY_USER_ID) ?: ""
                val postId = extras?.getString(BUNDLE_KEY_POST_ID) ?: ""
                val timelineType = extras?.getString(BUNDLE_KEY_TIMELINE_TYPE) ?: CommonApi.TimelineType.TIMELINE.name
                addReplaceFragment(R.id.container, PostTimelineFragment.newInstance(userId, postId, timelineType), false, true, null)
            }

            FRAGMENT_POST_DETAIL -> {
                val postId = extras?.getString(BUNDLE_KEY_POST_ID)
                val userId = extras?.getString(BUNDLE_KEY_USER_ID)
                val fromNotification = extras?.getBoolean(BUNDLE_KEY_FROM_NOTIFICATION)
                val appForeground = extras?.getBoolean(BUNDLE_NOTIFICATION_APP_FOREGROUND)

                fromNotification?.let {
                    startedFromNotification = it
                }
                appForeground?.let {
                    this.appForeground = it
                }

                postViewModel.currentPostLiveData.observe(this, Observer {
                    postViewModel.setCurrentPost(it)
                    val fragment = when {
                        it.isMainUserAuthor(userId) -> MyPostDetailFragment.newInstance()
                        it.isNews() -> NewsPostDetailFragment.newInstance()
                        else -> PostDetailFragment()
                    }
                    addReplaceFragment(R.id.container, fragment, false, !startedFromNotification, null)
                    postViewModel.currentPostLiveData.removeObservers(this@PostActivity)
                })
                postViewModel.getPost(postId!!)
            }

            FRAGMENT_MY_POST_DETAIL -> {
                addReplaceFragment(R.id.container, MyPostDetailFragment.newInstance(), false, true, null)
            }

            FRAGMENT_NEWS_POST_DETAIL -> {
                addReplaceFragment(R.id.container, NewsPostDetailFragment.newInstance(), false, true, null)
            }

            FRAGMENT_POST_LIKES -> {
                addReplaceFragment(R.id.container, PostLikeFragment.newInstance(), false, true, null)
            }
        }
    }

    companion object {

        fun openPostDetailFragment(
            context: Context,
            userId: String,
            postId: String,
            fromNotification: Boolean? = null,
            appForeground: Boolean = true,
            flags: Int? = null
        ) {
            val bundle = Bundle().apply {
                putString(BUNDLE_KEY_USER_ID, userId)
                putString(BUNDLE_KEY_POST_ID, postId)
                if (fromNotification != null) {
                    putBoolean(BUNDLE_KEY_FROM_NOTIFICATION, fromNotification)
                    putBoolean(BUNDLE_NOTIFICATION_APP_FOREGROUND, appForeground)
                }
            }
            openFragment<PostActivity>(
                context,
                FRAGMENT_POST_DETAIL,
                bundle,
                requestCode = if (fromNotification == true) REQUEST_RESULT_NOTIFICATION else null,
                animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.no_animation),
                flags = flags
            )
        }

        fun openPostTimelineFragment(
            context: Context,
            userId: String,
            postId: String? = null,
            type: CommonApi.TimelineType = CommonApi.TimelineType.TIMELINE
        ) {
            val bundle = Bundle().apply {
                putString(BUNDLE_KEY_USER_ID, userId)
                if (!postId.isNullOrEmpty()) {
                    putString(BUNDLE_KEY_POST_ID, postId)
                }
                putString(BUNDLE_KEY_TIMELINE_TYPE, type.name)
            }
            openFragment<PostActivity>(context, FRAGMENT_POST_TIMELINE, bundle, animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.no_animation))
        }

        fun openPostLikesFragment(context: Context) {
            openFragment<PostActivity>(context, FRAGMENT_POST_LIKES, null, animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.no_animation))
        }
    }
}