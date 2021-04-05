package rs.highlande.app.tatatu.feature.onBoarding.view.adapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_ON_BOARD_IMAGE
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_ON_BOARD_SUB_TITLE
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_ON_BOARD_TITLE
import rs.highlande.app.tatatu.feature.onBoarding.view.fragment.OnBoardingFirstFragment
import rs.highlande.app.tatatu.feature.onBoarding.view.fragment.OnBoardingFragment

/**
 * Created by Abhin.
 */
class ViewPagerAdapter(var activity: AppCompatActivity, supportFragmentManager: FragmentManager, mVisibleOtherFragment: Int = 0) : FragmentStatePagerAdapter(supportFragmentManager, mVisibleOtherFragment) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> loadOnBoardFirst()
            1 -> loadOnBoard(1)
            2 -> loadOnBoard(2)
            3 -> loadOnBoard(3)
            4 -> loadOnBoard(4)
            else -> null!!
        }
    }

    override fun getCount(): Int {
        return 5
    }

    private fun loadOnBoard(position: Int): OnBoardingFragment {
        val mBundle = Bundle()
        val mOnBoardingFragment = OnBoardingFragment()
        when (position) {
            1 -> mBundle.apply {
                putString(BUNDLE_KEY_ON_BOARD_TITLE, activity.resources.getString(R.string.title_post_your_photos_and_videos))
                putString(BUNDLE_KEY_ON_BOARD_SUB_TITLE, activity.resources.getString(R.string.sub_title_earn_ttu))
                putInt(BUNDLE_KEY_ON_BOARD_IMAGE, R.drawable.ic_post_your_photos_and_videos_illustration)
            }
            2 -> mBundle.apply {
                putString(BUNDLE_KEY_ON_BOARD_TITLE, activity.resources.getString(R.string.title_better_than_free))
                putString(BUNDLE_KEY_ON_BOARD_SUB_TITLE, activity.resources.getString(R.string.sub_title_you_ll))
                putInt(BUNDLE_KEY_ON_BOARD_IMAGE, R.drawable.ic_better_than_free_illustration)
            }
            3 -> mBundle.apply {
                putString(BUNDLE_KEY_ON_BOARD_TITLE, activity.resources.getString(R.string.title_earn_more_with_friends))
                putString(BUNDLE_KEY_ON_BOARD_SUB_TITLE, activity.resources.getString(R.string.sub_title_invite_friends))
                putInt(BUNDLE_KEY_ON_BOARD_IMAGE, R.drawable.ic_earn_more_with_friends_illustration)
            }
            4 -> mBundle.apply {
                putString(BUNDLE_KEY_ON_BOARD_TITLE, activity.resources.getString(R.string.title_redeem_ttu_tokens))
                putString(BUNDLE_KEY_ON_BOARD_SUB_TITLE, activity.resources.getString(R.string.sub_title_receive_discount))
                putInt(BUNDLE_KEY_ON_BOARD_IMAGE, R.drawable.ic_redeem_ttu_tokens_illustration)
            }
        }
        mOnBoardingFragment.arguments = mBundle
        return mOnBoardingFragment
    }

    private fun loadOnBoardFirst(): OnBoardingFirstFragment {
        val mOnBoardingFirstFragment = OnBoardingFirstFragment()
        mOnBoardingFirstFragment.arguments = Bundle().apply {
            putString(BUNDLE_KEY_ON_BOARD_TITLE, activity.resources.getString(R.string.title_welcome))
            putString(BUNDLE_KEY_ON_BOARD_SUB_TITLE, activity.resources.getString(R.string.sub_title_choose_from))
            putInt(BUNDLE_KEY_ON_BOARD_IMAGE, R.drawable.on_board_first)
        }
        return mOnBoardingFirstFragment
    }
}