package rs.highlande.app.tatatu.feature.inviteFriends.view.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.common_toolbar_back.*
import kotlinx.android.synthetic.main.fragment_invite_friends.*
import kotlinx.android.synthetic.main.include_invite_friends_expandable.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.feature.inviteFriends.view.adapter.InviteTabAdapter
import rs.highlande.app.tatatu.feature.inviteFriends.view.viewModel.InviteFriendsViewModel

/**
 * Created by Abhin.
 */
class InviteFriendsFragment : BaseFragment() {

    private var isTextShow = false
    private val mInviteFriendsViewModel: InviteFriendsViewModel by sharedViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_invite_friends, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title.text = resources.getString(R.string.invite_friend)
        init()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObserver()
    }

    private fun initObserver() {
        mInviteFriendsViewModel.isProgress.observe(this, Observer {
            if (it) {
                showLoader(resources.getString(R.string.loader_link))
            } else {
                hideLoader()
            }
        })
    }

    private fun init() {
        mInviteFriendsViewModel.getInvitationLink()
        val fragmentAdapter = InviteTabAdapter(childFragmentManager, context!!)
        viewPager_Invite_friends.adapter = fragmentAdapter
        tab_Invite_Friends.setupWithViewPager(viewPager_Invite_friends)

        img_expandable.setImageResource(R.drawable.ic_dropdown_icon_plus)
        isTextShow = false

        ll_how_much.setOnClickListener {
            if (isTextShow) {
                img_expandable.setImageResource(R.drawable.ic_dropdown_icon_plus)
                collapse(txt_expandable_text)
            } else {
                img_expandable.setImageResource(R.drawable.ic_dropdown_icon_minus)
                expand(txt_expandable_text)
            }
            isTextShow = !isTextShow
        }

        backArrow.setOnClickListener {
            (it.context as? Activity)?.let { activity ->
                activity.finish()
                activity.overridePendingTransition(R.anim.no_animation, R.anim.slide_out_to_right)
            }
        }
    }


    private fun expand(v: View) {
        val matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec((v.parent as View).width, View.MeasureSpec.EXACTLY)
        val wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)
        val targetHeight = v.measuredHeight

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.layoutParams.height = 1
        v.visibility = View.VISIBLE
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                v.layoutParams.height = if (interpolatedTime == 1f) ViewGroup.LayoutParams.WRAP_CONTENT
                else (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // Expansion speed of 1dp/ms
        a.duration = (targetHeight / v.context.resources.displayMetrics.density).toInt().toLong()
        v.startAnimation(a)
    }

    private fun collapse(v: View) {
        val initialHeight = v.measuredHeight

        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // Collapse speed of 1dp/ms
        a.duration = (initialHeight / v.context.resources.displayMetrics.density).toInt().toLong()
        v.startAnimation(a)
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}


    companion object {

        val logTag = InviteFriendsFragment::class.java.simpleName

        fun newInstance(): InviteFriendsFragment {
            val frg = InviteFriendsFragment()

            val args = Bundle().apply { }

            return frg.apply { arguments = args }
        }
    }
}