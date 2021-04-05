

package rs.highlande.app.tatatu.feature.account.profile.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.recyclerView.EndlessScrollListener
import rs.highlande.app.tatatu.core.ui.recyclerView.NestedItemVisibilityManager
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.databinding.FragmentProfileGridBinding
import rs.highlande.app.tatatu.feature.account.profile.view.ProfileViewModel
import rs.highlande.app.tatatu.feature.account.profile.view.adapter.ProfilePostGridAdapter
import rs.highlande.app.tatatu.feature.commonApi.CommonApi
import rs.highlande.app.tatatu.feature.createPost.view.CreatePostActivity
import rs.highlande.app.tatatu.feature.post.PostActivity
import rs.highlande.app.tatatu.model.Post
import rs.highlande.app.tatatu.model.PostType

class ProfilePostsFragment: BaseFragment() {

    companion object {
        private const val SHOW_CREATE_ITEM = "SHOW_CREATE_ITEM"
        fun newInstance(showCreateItem: Boolean = false): ProfilePostsFragment {
            val bundle = Bundle()
            bundle.putBoolean(SHOW_CREATE_ITEM, showCreateItem)
            val instance = ProfilePostsFragment()
            instance.arguments = bundle
            return instance
        }
    }

    lateinit var profilePostBinding: FragmentProfileGridBinding
    lateinit var profilePostGridGridAdapter: ProfilePostGridAdapter

    private var reloadList = false
    private var loaderCanBeShow = false

    private val viewModel by sharedViewModel<ProfileViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        profilePostBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile_grid, container, false)
        profilePostBinding.lifecycleOwner = this
        subscribeToLiveData()
        configureLayout(profilePostBinding.root)
        return profilePostBinding.root
    }

    override fun configureLayout(view: View) {
        profilePostGridGridAdapter = ProfilePostGridAdapter(
            object : OnItemClickListener<Post> {
                override fun onItemClick(item: Post) {
                    when {
                        item.type == PostType.NEW -> {
                            reloadList = true
                            CreatePostActivity.openCreatePostActivity(context!!)
                        }
                        else -> {
                            viewModel.profileUserLiveData.value?.let {
                                viewModel.cachePostList(profilePostGridGridAdapter.getItems().toMutableList())
                                PostActivity.openPostTimelineFragment(context!!, it.uid, item.uid, CommonApi.TimelineType.PROFILE)
                            }
                        }
                    }
                }
            },
            NestedItemVisibilityManager(viewModel.setNewPostsScrollListener(
                object : EndlessScrollListener() {
                    override fun onLoadMore() {
                        viewModel.profileUserLiveData.value?.let {
                            viewModel.fetchPosts(it.uid)
                        }
                    }
                }) as EndlessScrollListener),
            parentFragment is MyProfileFragment
        )

        profilePostBinding.profileList.adapter = profilePostGridGridAdapter

        viewModel.profileUserLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                viewModel.profileUserLiveData.removeObservers(viewLifecycleOwner)
                initPostList(it.uid)
            }
        })

        (parentFragment as BaseProfileFragment).addScrollListener(profilePostBinding.profileList)

    }

    override fun onResume() {
        super.onResume()
        loaderCanBeShow = true
        if (reloadList) {
            viewModel.profileUserLiveData.value?.let {
                reloadList = false
                initPostList(it.uid)
            }
        }
    }

    override fun onPause() {
        loaderCanBeShow = false
        super.onPause()
    }

    private fun initPostList(userID: String) {
        if (loaderCanBeShow) showLoader(R.string.loader_fetching_posts)
        viewModel.fetchPosts(userID, true)
    }

    override fun bindLayout() {
        arguments?.let {
            viewModel.showCreateItem = it.getBoolean(SHOW_CREATE_ITEM, false)
            loaderCanBeShow = !viewModel.showCreateItem
        }
    }

    fun subscribeToLiveData() {
        viewModel.profilePostsLiveData.observe(viewLifecycleOwner, Observer {
            if (loaderCanBeShow) hideLoader()
            viewModel.profileUserLiveData.value?.let { _ ->
                if (viewModel.replaceItemsOnNextUpdate) {
                    if (viewModel.showCreateItem) {
                        it.add(0, Post().apply { type = PostType.NEW })
                    }
                    profilePostGridGridAdapter.setItems(it)
                } else {
                    profilePostGridGridAdapter.addAll(it)
                }
                (parentFragment as BaseProfileFragment).hideProgressBar()
            }
        })

    }

}