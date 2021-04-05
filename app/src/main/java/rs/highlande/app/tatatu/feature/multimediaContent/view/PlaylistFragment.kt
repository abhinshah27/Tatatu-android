package rs.highlande.app.tatatu.feature.multimediaContent.view

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.connection.http.ACCOUNT_ID
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.EndlessScrollListener
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListenerWithView
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.databinding.FragmentMultimediaPlaylistBinding
import rs.highlande.app.tatatu.feature.multimediaContent.view.MultimediaActivity.Companion.BUNDLE_KEY_HOME_DATA
import rs.highlande.app.tatatu.model.HomeNavigationData
import rs.highlande.app.tatatu.model.TTUVideo

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-18.
 */
class PlaylistFragment : BaseFragment() {

    private val viewModel: PlaylistViewModel by sharedViewModel()

    lateinit var binding: FragmentMultimediaPlaylistBinding
    lateinit var adapter: PlaylistAdapter

    private var loadingMore = false


    private val endlessScrollListener = object : EndlessScrollListener() {
        override fun onLoadMore() {
            loadingMore = true
            viewModel.getMoreVideos(ACCOUNT_ID)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(false)
        retainInstance = true

        viewModel.navigationData = arguments?.getParcelable(BUNDLE_KEY_HOME_DATA)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_multimedia_playlist, container, false)

        configureLayout(binding.root)

        // INFO: 2019-09-30    removing instance state saving process, fixes crashes:
        //  https://console.firebase.google.com/u/3/project/tatatu-38064/crashlytics/app/android:com.tatatu/issues/e5650d200ead44b397112d6d8700219b
        //  https://console.firebase.google.com/u/3/project/tatatu-38064/crashlytics/app/android:com.tatatu/issues/7f1ef404153e51730a4fdd44d7b1bd80
//        savedInstanceState?.let {
//            val videos = it.getParcelableArrayList<TTUVideo>(BUNDLE_KEY_STATE_VIDEOS)
//            adapter.setItems(videos)
//        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mBaseActivity?.setToolbarTitle(viewModel.navigationData?.sectionTitle ?: "")
    }


    // INFO: 2019-09-30    removing instance state saving process, fixes crashes:
    //  https://console.firebase.google.com/u/3/project/tatatu-38064/crashlytics/app/android:com.tatatu/issues/e5650d200ead44b397112d6d8700219b
    //  https://console.firebase.google.com/u/3/project/tatatu-38064/crashlytics/app/android:com.tatatu/issues/7f1ef404153e51730a4fdd44d7b1bd80
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//
//        outState.putParcelableArrayList(BUNDLE_KEY_STATE_VIDEOS, arrayListOf<TTUVideo>().apply { addAll(adapter.getItems()) })
//    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

    }


    override fun onResume() {
        super.onResume()

        viewModel.apply {

            videoList.observe(this@PlaylistFragment, Observer {

                if (binding.srl.isRefreshing) binding.srl.isRefreshing = false

                // notify scrollListener that more scroll is accepted
                endlessScrollListener.canFetch = it.second

                if (it.first != null) showData(it.first!!)
            })

            if (this@PlaylistFragment.adapter.itemCount == 0) getVideos(ACCOUNT_ID)
        }
    }

    private fun showData(videos: List<TTUVideo>) {
        if (videos.isEmpty()) {

            // TODO: 2019-07-18    add empty management

        } else {
            if (loadingMore) {
                val currentSize = adapter.itemCount
                adapter.addAll(videos)
                adapter.notifyItemRangeInserted(currentSize, videos.size)
                loadingMore = false
            } else adapter.setItems(videos.toMutableList())
        }
    }


    override fun onPause() {
        viewModel.clearObservers(this)
        viewModel.resetResultLiveData()
        super.onPause()
    }


    override fun configureLayout(view: View) {
        this@PlaylistFragment.adapter = PlaylistAdapter(object : OnItemClickListenerWithView<TTUVideo> {
            override fun onItemClick(view: View, item: TTUVideo) {

                addReplaceFragment(R.id.container, PlaylistDetailFragment.newInstance(item), true, true, NavigationAnimationHolder())

            }
        })

        binding.videoList.apply {
            layoutManager = GridLayoutManager(context!!, 3)
            adapter = this@PlaylistFragment.adapter

            addOnScrollListener(endlessScrollListener)

            //            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            itemAnimator = null
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    super.getItemOffsets(outRect, view, parent, state)

                    val offset = resources.getDimensionPixelSize(R.dimen.padding_margin_small_6)
                    outRect.set(offset, offset, offset, offset)
                }
            })

        }

        binding.srl.setOnRefreshListener {
            viewModel.refreshVideos(ACCOUNT_ID)
        }

    }

    override fun bindLayout() {}


    companion object {

        val logTag = PlaylistFragment::class.java.simpleName

        const val BUNDLE_KEY_STATE_VIDEOS = "state_videos"

        fun newInstance(playlist: HomeNavigationData): PlaylistFragment {

            val fragment = PlaylistFragment()

            val args = Bundle().apply {
                putParcelable(BUNDLE_KEY_HOME_DATA, playlist)
            }

            return fragment.apply { arguments = args }

        }

    }

}