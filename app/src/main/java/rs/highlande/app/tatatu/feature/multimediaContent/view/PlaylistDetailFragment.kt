package rs.highlande.app.tatatu.feature.multimediaContent.view

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.formatTTUTokens
import rs.highlande.app.tatatu.databinding.FragmentMultimediaDetailBinding
import rs.highlande.app.tatatu.feature.multimediaContent.view.MultimediaActivity.Companion.BUNDLE_KEY_VIDEO
import rs.highlande.app.tatatu.model.TTUVideo

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-21.
 */
class PlaylistDetailFragment : BaseFragment() {

    private val viewModel: PlaylistDetailViewModel by sharedViewModel()

    lateinit var binding: FragmentMultimediaDetailBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        retainInstance = true

        // FIXME: 2019-07-22    shared element transition
//        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)

        viewModel.currentTTUVideo = arguments?.getParcelable<TTUVideo>(BUNDLE_KEY_VIDEO)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_multimedia_detail, container, false)
        binding.lifecycleOwner = this

        configureLayout(binding.root)

        savedInstanceState?.let {
            viewModel.currentTTUVideo = it.getParcelable<TTUVideo>(BUNDLE_KEY_VIDEO)
        }

        binding.video = viewModel.currentTTUVideo

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mBaseActivity?.setToolbarTitle(viewModel.currentTTUVideo?.name ?: "")
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

        binding.executePendingBindings()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(BUNDLE_KEY_VIDEO, viewModel.currentTTUVideo)
    }

    override fun onPause() {
        viewModel.clearObservers(this)
        super.onPause()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_playlist_detail, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.share) {
            handleShare()
            true
        } else super.onOptionsItemSelected(item)
    }



    override fun configureLayout(view: View) {

        // FIXME: 2019-07-22    shared element transition
//        binding.poster.transitionName = viewModel.currentTTUVideo?.id ?: ""

        binding.btnPlay.setOnClickListener {

            startActivity(
                Intent(it.context, VideoPlayerActivity::class.java).apply {
                    putExtra(VideoPlayerActivity.BUNDLE_KEY_VIDEO_ID, viewModel.currentTTUVideo?.id)
                })

        }
    }

    override fun bindLayout() {
        binding.tokensToEarn.text = formatTTUTokens(viewModel.getVideoEstimatedEarnings())
    }

    fun handleShare() {
        viewModel.movieDeeplinkLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                hideLoader()
                if (!it.isNullOrBlank())
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, it)
                        type = "text/plain"
                        startActivity(Intent.createChooser(this, getString(R.string.share_with_label)))
                    }
                viewModel.movieDeeplinkLiveData.value = null
                viewModel.movieDeeplinkLiveData.removeObservers(viewLifecycleOwner)
            }
        })
        showLoader(R.string.progress_share)
        viewModel.shareMovie()
    }

    companion object {

        val logTag = PlaylistDetailFragment::class.java.simpleName

        fun newInstance(currentVideo: TTUVideo): PlaylistDetailFragment {

            val fragment = PlaylistDetailFragment()

            val args = Bundle().apply {
                putParcelable(BUNDLE_KEY_VIDEO, currentVideo)
            }

            return fragment.apply { arguments = args }

        }

    }

}