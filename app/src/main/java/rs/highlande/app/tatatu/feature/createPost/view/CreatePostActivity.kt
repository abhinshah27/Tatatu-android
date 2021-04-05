package rs.highlande.app.tatatu.feature.createPost.view

import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.ActivityAnimationHolder
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.model.Post


/**
 * Created by Abhin.
 */
class CreatePostActivity : BaseActivity() {

    private val viewModel by viewModel<CreatePostViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        manageIntent()
    }

    override fun configureLayout() {}
    override fun bindLayout() {}
    override fun manageIntent() {

        val intent = intent ?: return

        val showFragment = intent.getIntExtra(
            FRAGMENT_KEY_CODE,
            FRAGMENT_INVALID
        )
        val requestCode = intent.getIntExtra(REQUEST_CODE_KEY, NO_RESULT)
        val extras = intent.extras

        when (showFragment) {

            FRAGMENT_CREATE_POST -> {
                addReplaceFragment(R.id.fl_container, CreatePostFragment.newInstance(), false, false, null)
            }

            FRAGMENT_EDIT_POST -> {
                addReplaceFragment(R.id.fl_container, CreatePostPreviewFragment.newInstance(extras), true, true, null)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)

        // TODO: 2019-07-09    reconsider Parcelable BUT ISSUE with InstanceCreator
        outState.putParcelable(BUNDLE_KEY_POST, viewModel.getPost() ?: Post())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        // TODO: 2019-07-09    reconsider Parcelable BUT ISSUE with InstanceCreator
        viewModel.setPost(
            savedInstanceState.getParcelable<Post>(BUNDLE_KEY_POST) ?: Post()
        )
    }


    companion object {

        const val BUNDLE_KEY_POST = "post"

        fun openCreatePostActivity(context: Context) {
            openFragment<CreatePostActivity>(context, FRAGMENT_CREATE_POST)
        }

        fun openEditPostFragment(context: Context, userId: String, post: Bundle) {
            post.apply { putString(BUNDLE_KEY_USER_ID, userId) }
            openFragment<CreatePostActivity>(context, FRAGMENT_EDIT_POST, post, animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.no_animation))
        }

    }

}