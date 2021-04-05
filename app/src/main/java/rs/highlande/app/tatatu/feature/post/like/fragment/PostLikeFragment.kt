package rs.highlande.app.tatatu.feature.post.like.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.common_toolbar_back.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.manager.RelationshipAction
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.ImageSingleBottomSheetFragment
import rs.highlande.app.tatatu.core.ui.recyclerView.CommonScrollListener
import rs.highlande.app.tatatu.core.ui.recyclerView.EndlessScrollListener
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListenerWithView
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.setProfilePicture
import rs.highlande.app.tatatu.databinding.FragmentPostLikesBinding
import rs.highlande.app.tatatu.feature.account.AccountActivity
import rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment.BaseFollowFragment
import rs.highlande.app.tatatu.feature.post.like.adapter.PostLikeAdapter
import rs.highlande.app.tatatu.feature.post.like.viewmodel.PostLikeViewModel
import rs.highlande.app.tatatu.model.User

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class PostLikeFragment: BaseFragment() {

    companion object {

        val logTag = PostLikeFragment::class.java.simpleName

        fun newInstance() = PostLikeFragment()
    }

    private lateinit var postsLikeBinding: FragmentPostLikesBinding
    private lateinit var postLikeAdapter: PostLikeAdapter
    private val postLikeViewModel: PostLikeViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        postsLikeBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_post_likes, container, false)
        postsLikeBinding.root.apply {
            configureLayout(this)
            return this
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        subscribeToLiveData()
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

    }

    fun subscribeToLiveData() {
        postLikeViewModel.errorResponseLiveData.observe(viewLifecycleOwner, Observer {
            (postsLikeBinding.srl as SwipeRefreshLayout).isRefreshing = false
        })


        postLikeViewModel.postLikeListLiveData.observe(viewLifecycleOwner, Observer {
            (postsLikeBinding.srl as SwipeRefreshLayout).isRefreshing = false
            postLikeAdapter.setItems(it)
        })

        postLikeViewModel.relationshipChangeLiveData.observe(viewLifecycleOwner, Observer {
            postLikeViewModel.cachedLikeUser?.let {
                postLikeAdapter.updateItem(it)
            }
        })

        postLikeViewModel.actionRequestLiveData.observe(viewLifecycleOwner, Observer {
            when(it) {
                RelationshipAction.FOLLOWING_FRIENDS_ACTION -> {
                    val bottomSheet = ImageSingleBottomSheetFragment.newInstance(object : ImageSingleBottomSheetFragment.BottomSheetListener {
                        override fun onBottomSheetReady(bottomSheet: ImageSingleBottomSheetFragment) {
                            postLikeViewModel.cachedLikeUser?.let {
                                bottomSheet.binding.bottomSheetAction.setText(R.string.bottom_sheet_unfollow)
                                bottomSheet.binding.bottomSheetHint.setText(R.string.bottom_sheet_tap_unfollow_hint)

                                bottomSheet.binding.bottomImageView.setProfilePicture(postLikeViewModel.cachedLikeUser!!.picture)

                                bottomSheet.binding.bottomSheetAction.setOnClickListener {
                                    showMessage(getString(R.string.result_action_unfollow, postLikeViewModel.cachedLikeUser!!.username))
                                    postLikeViewModel.handleFollowerRelationChange()
                                    bottomSheet.dismiss()
                                }
                            }
                        }
                    })
                    bottomSheet.show(childFragmentManager, "bottomSheet")
                }
                RelationshipAction.FOLLOW_BACK_ACTION -> {
                    postLikeViewModel.handleFollowerRelationChange()
                }
                RelationshipAction.REQUEST_CANCEL_ACTION -> {
                    val bottomSheet = ImageSingleBottomSheetFragment.newInstance(object : ImageSingleBottomSheetFragment.BottomSheetListener {
                        override fun onBottomSheetReady(bottomSheet: ImageSingleBottomSheetFragment) {
                            postLikeViewModel.cachedLikeUser?.let {
                                bottomSheet.binding.bottomSheetAction.setText(R.string.bottom_sheet_cancel_request)
                                bottomSheet.binding.bottomSheetHint.setText(R.string.bottom_sheet_tap_cancel_request_hint)

                                bottomSheet.binding.bottomImageView.setProfilePicture(it.picture)

                                bottomSheet.binding.bottomSheetAction.setOnClickListener {
                                    showMessage(getString(R.string.result_action_cancel_request, postLikeViewModel.cachedLikeUser!!.username))
                                    postLikeViewModel.handleFollowerRelationChange()
                                    bottomSheet.dismiss()
                                }
                            }
                        }
                    })
                    bottomSheet.show(childFragmentManager, "bottomSheet")
                }
                RelationshipAction.FOLLOW_ACTION -> postLikeViewModel.handleFollowerRelationChange()
            }
        })

    }



    override fun configureLayout(view: View) {
        postLikeAdapter = PostLikeAdapter(object: BaseFollowFragment.FollowClickListener(this, getUser()) {
            override fun onItemClick(user: User) {
                AccountActivity.openProfileFragment(context!!, user.uid)
            }

            override fun onActionClick(user: User) {
                postLikeViewModel.onRelationshipActionClick(user)
            }
        }, context!!)

        postsLikeBinding.toolbar.title.setText(R.string.tb_likes)
        postsLikeBinding.toolbar.backArrow.setOnClickListener {
            activity!!.onBackPressed()
        }
        postLikeViewModel.getPostLikes()
        postsLikeBinding.srl.setOnRefreshListener{
            postLikeViewModel.getPostLikes()
        }
    }

    override fun bindLayout() {
        postsLikeBinding.likesList.apply {
            adapter = postLikeAdapter
            addItemDecoration(DividerItemDecoration(context, (layoutManager as LinearLayoutManager).orientation))
        }
    }
}