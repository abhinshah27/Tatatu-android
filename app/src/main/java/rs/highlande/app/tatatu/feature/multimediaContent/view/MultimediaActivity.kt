package rs.highlande.app.tatatu.feature.multimediaContent.view

import android.content.Context
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.common_activity_container.view.*
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.ActivityAnimationHolder
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.*
import rs.highlande.app.tatatu.databinding.ActivityMultimediaBinding
import rs.highlande.app.tatatu.feature.commonView.BottomNavigationActivity
import rs.highlande.app.tatatu.model.HomeNavigationData
import rs.highlande.app.tatatu.model.TTUVideo

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-18.
 */
class MultimediaActivity : BottomNavigationActivity() {

    private lateinit var mediaBinding: ActivityMultimediaBinding

    private val playlistViewModel: PlaylistViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaBinding = DataBindingUtil.setContentView(this, R.layout.activity_multimedia)
        mediaBinding.lifecycleOwner = this

        setupBottomNavigation(mediaBinding.activityContainer.bottomBar as BottomNavigationView)

        configureLayout()

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 1) {
                supportFragmentManager.findFragmentByTag(PlaylistFragment.logTag)?.let{
                    setToolbarTitle(playlistViewModel.navigationData?.sectionTitle ?: "")
                }
            }
        }

    }


    override fun configureLayout() {

        mediaBinding.toolbar.let {
            setSupportActionBar(it as Toolbar)
            supportActionBar?.let{ actionBar ->
                actionBar.setDisplayShowTitleEnabled(false)
                actionBar.setDisplayShowHomeEnabled(false)
                actionBar.setDisplayHomeAsUpEnabled(false)
            }

            it.backArrow.setOnClickListener {
                onBackPressed()
            }
        }

    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount <= 1) {
            finish()
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    override fun bindLayout() {}



    override fun manageIntent() {
        val showFragment = intent?.getIntExtra(
            FRAGMENT_KEY_CODE,
            FRAGMENT_INVALID
        )
        val extras = intent.extras

        var fragment: BaseFragment?

        when (showFragment) {
            null -> return

            FRAGMENT_MM_PLAYLIST -> {
                fragment = supportFragmentManager.findFragmentByTag(PlaylistFragment.logTag) as? BaseFragment
                val playlist = extras?.getParcelable<HomeNavigationData>(BUNDLE_KEY_HOME_DATA)
                if (playlist != null && fragment == null) {
                    addReplaceFragment(
                        R.id.container,
                        PlaylistFragment.newInstance(playlist),
                        false,
                        true,
                        null
                    )
                }
            }

            FRAGMENT_MM_VIDEO -> {
                fragment = supportFragmentManager.findFragmentByTag(PlaylistDetailFragment.logTag) as? BaseFragment
                val video = extras?.getParcelable<TTUVideo>(BUNDLE_KEY_VIDEO)
                if (video != null && fragment == null) {
                    addReplaceFragment(
                        R.id.container,
                        PlaylistDetailFragment.newInstance(video),
                        false,
                        true,
                        null
                    )
                }
            }
        }
    }


    override fun getToolbar(): Toolbar? {
        return mediaBinding.toolbar as Toolbar
    }

    override fun getFinishAnimations(): ActivityAnimationHolder? {
        return ActivityAnimationHolder(R.anim.no_animation, R.anim.slide_out_to_right)
    }


    companion object {

        const val BUNDLE_KEY_HOME_DATA = "home_data"
        const val BUNDLE_KEY_VIDEO = "video"

        fun openPlaylistFragment(context: Context, playlist: HomeNavigationData) {
            val extras = Bundle().apply {
                putParcelable(BUNDLE_KEY_HOME_DATA, playlist)
            }
            openFragment<MultimediaActivity>(context, FRAGMENT_MM_PLAYLIST, extras, animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.no_animation))
        }

        fun openVideoFragment(context: Context, video: TTUVideo) {
            val extras = Bundle().apply {
                putParcelable(BUNDLE_KEY_VIDEO, video)
            }
            openFragment<MultimediaActivity>(context, FRAGMENT_MM_VIDEO, extras, animations = ActivityAnimationHolder(R.anim.slide_in_from_right, R.anim.no_animation))
        }

    }

}