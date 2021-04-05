package rs.highlande.app.tatatu.feature.account.followFriendList.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.ui.NavigationAnimationHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecyclerListener
import rs.highlande.app.tatatu.databinding.FragmentFollowBaseBinding
import rs.highlande.app.tatatu.feature.account.profile.view.fragment.MyProfileFragment
import rs.highlande.app.tatatu.feature.account.profile.view.fragment.ProfileFragment
import rs.highlande.app.tatatu.model.User

abstract class BaseFollowFragment: BaseFragment() {

    //FIXME 07/08: Had to add this check because observeMyUserAction is firing multiple times, revise set user logic
    protected var ignoreMyUserAction = false

    protected lateinit var followBinding: FragmentFollowBaseBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        followBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_follow_base, container, false)
        subscribeToLiveData()
        configureLayout(followBinding.root)
        return followBinding.root
    }

    abstract fun subscribeToLiveData()

    abstract fun updateList()

    override fun configureLayout(view: View) {
        followBinding.apply {
            followSRL.setOnRefreshListener { updateList() }
            followUserList.addItemDecoration(DividerItemDecoration(
                followUserList.context, (followUserList.layoutManager as LinearLayoutManager).orientation
            ))
        }
    }

    fun showProgressBar() {
        followBinding.followLoadingProgressBar.show()
    }

    fun hideProgressBar() {
        followBinding.followLoadingProgressBar.hide()
    }

    abstract class FollowClickListener(private val fragment: BaseFragment, private val currentUser: User?): BaseRecyclerListener {
        open fun onItemClick(user: User) {
            with(fragment) {
                currentUser?.let {
                    if (it.uid == user.uid) {
                        addReplaceFragment(
                            R.id.container,
                            MyProfileFragment.newInstance(false),
                            false,
                            true,
                            NavigationAnimationHolder()
                        )
                    }
                    else {
                        addReplaceFragment(
                            R.id.container,
                            ProfileFragment.newInstance(user.uid),
                            false,
                            true,
                            NavigationAnimationHolder()
                        )
                    }
                } ?: run {
                    addReplaceFragment(
                        R.id.container,
                        ProfileFragment.newInstance(user.uid),
                        false,
                        true,
                        NavigationAnimationHolder()
                    )
                }
            }
        }

        abstract fun onActionClick(user: User)
    }

}