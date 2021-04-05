package rs.highlande.app.tatatu.feature.search.view.fragment

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.*
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListenerWithView
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.databinding.FragmentSearchBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.multimediaContent.view.MultimediaActivity
import rs.highlande.app.tatatu.feature.multimediaContent.view.PlaylistAdapter
import rs.highlande.app.tatatu.feature.search.view.adapter.UserResultAdapter
import rs.highlande.app.tatatu.feature.search.view.viewModel.SearchViewModel
import rs.highlande.app.tatatu.model.TTUVideo
import rs.highlande.app.tatatu.model.User


/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class SearchFragment: BaseFragment() {

    private lateinit var binding: FragmentSearchBinding
    private val viewModel: SearchViewModel by viewModel()

    lateinit var mediaAdapter: PlaylistAdapter
    lateinit var userAdapter: UserResultAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false)
        binding.lifecycleOwner = this
        binding.root.apply {
            configureLayout(this)
            return this
        }
    }

    override fun configureLayout(view: View) {
        (activity as? BaseActivity)?.apply {
            setSupportActionBar(binding.toolbar as Toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            supportActionBar!!.setDisplayShowTitleEnabled(false)
            supportActionBar!!.setDisplayShowHomeEnabled(false)
            binding.toolbar!!.title.setText(R.string.title_search)
            binding.toolbar.backArrow.visibility = View.INVISIBLE
        }
        mediaAdapter = PlaylistAdapter(
            object : OnItemClickListenerWithView<TTUVideo> {
                override fun onItemClick(view: View, item: TTUVideo) {
                    MultimediaActivity.openVideoFragment(context!!, item)
                }
            }
        )
        userAdapter = UserResultAdapter(
            object : OnItemClickListener<User> {
                override fun onItemClick(item: User) {
                    binding.root.hideKeyboard()
                    AccountActivity.openProfileFragment(context!!, item.uid)
                }
            }
        )

        binding.userList.apply {
            layoutManager = GridLayoutManager(context!!, 3)
            adapter = this@SearchFragment.userAdapter

            itemAnimator = null
            addItemDecoration(
                object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        super.getItemOffsets(outRect, view, parent, state)

                        val offset = resources.getDimensionPixelSize(R.dimen.padding_margin_small_6)
                        outRect.set(offset, offset, offset, offset)
                    }
                }
            )
        }
        binding.videoList.apply {
            layoutManager = GridLayoutManager(context!!, 3)
            adapter = this@SearchFragment.mediaAdapter
            itemAnimator = null
            addItemDecoration(
                object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        super.getItemOffsets(outRect, view, parent, state)

                        val offset = resources.getDimensionPixelSize(R.dimen.padding_margin_small_6)
                        outRect.set(offset, offset, offset, offset)
                    }
                }
            )
        }
    }

    override fun bindLayout() {
        binding.srl.isEnabled = false
        with(binding.searchBox) {
            root.setOnClickListener {
                searchLabelEditText.requestFocus()
                searchLabelEditText.showKeyboard()
            }
            searchLabelEditText.setOnFocusChangeListener { _, _ ->
                if (searchLabelEditText.hasFocus()) {
                    clearSearchIconImageView.visibility = View.VISIBLE
                } else {
                    clearSearchIconImageView.visibility = View.GONE
                }
                clearSearchIconImageView.setOnClickListener {
                    searchLabelEditText.setText("")
                }

            }
            searchLabelEditText.setHint(R.string.title_search)
            searchLabelEditText.addTextChangedListener(SearchTextWatcher(object: SearchTextWatcher.SearchListener {
                override fun onQueryStringIsEmpty() {
                    if (binding.srl.isRefreshing)
                        binding.srl.isRefreshing = false
                    with(binding.searchBox) {
                        searchLabelEditText.clearFocus()
                        clearSearchIconImageView.visibility = View.GONE
                        viewModel.cancelSearch()
                        mediaAdapter.clear()
                        userAdapter.clear()
                        binding.noResultsFoundTextView.visibility = View.GONE
                        binding.userListLabel.visibility = View.GONE
                        binding.mediaListLabel.visibility = View.GONE
                    }
                }

                override fun onQueryStringAvailable(query: String) {
                    performSearch(query)
                }
            }))
            searchLabelEditText.isSingleLine = true
            searchLabelEditText.setOnEditorActionListener { textView, i, keyEvent ->
                if (!textView.text.isNullOrBlank()) {
                    if (i == EditorInfo.IME_ACTION_SEARCH) {
                        performSearch(textView.text.toString())
                        true
                    }
                }
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        AnalyticsUtils.trackScreen(logTag)

        subscribeToLiveData()
    }

    override fun onPause() {
        viewModel.clearObservers(this)
        super.onPause()
    }

    fun subscribeToLiveData() {
        viewModel.mediaSearchResultsLiveData.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrEmpty()) {
                if (binding.srl.isRefreshing)
                    binding.srl.isRefreshing = false
                binding.noResultsFoundTextView.visibility = View.GONE
                showMediaData(it)
            } else {
                binding.mediaListLabel.visibility = View.GONE
                mediaAdapter.clear()
            }
        })
        viewModel.usersSearchResultsLiveData.observe(viewLifecycleOwner, Observer {
            if (!it.isNullOrEmpty()) {
                if (binding.srl.isRefreshing)
                    binding.srl.isRefreshing = false
                binding.noResultsFoundTextView.visibility = View.GONE
                showUserData(it)
            } else {
                binding.userListLabel.visibility = View.GONE
                userAdapter.clear()
            }
        })
        viewModel.noResultsLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                if (binding.srl.isRefreshing)
                    binding.srl.isRefreshing = false
                mediaAdapter.clear()
                userAdapter.clear()
                binding.userListLabel.visibility = View.GONE
                binding.mediaListLabel.visibility = View.GONE
                binding.noResultsFoundTextView.visibility = View.VISIBLE
            }
        })
    }

    private fun performSearch(query: String) {
        binding.srl.isRefreshing = true
        binding.noResultsFoundTextView.visibility = View.GONE
        viewModel.performSearch(query)
    }

    private fun showMediaData(videos: List<TTUVideo>) {
        binding.mediaListLabel.visibility = View.VISIBLE
        mediaAdapter.setItems(videos.toMutableList())
    }

    private fun showUserData(users: List<User>) {
        binding.userListLabel.visibility = View.VISIBLE
        userAdapter.setItems(users.toMutableList())
    }

    companion object {
        val logTag = SearchFragment::class.java.simpleName

        fun newInstance() = SearchFragment()
    }

}