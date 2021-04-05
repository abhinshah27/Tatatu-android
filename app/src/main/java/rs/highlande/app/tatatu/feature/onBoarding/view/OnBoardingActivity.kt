package rs.highlande.app.tatatu.feature.onBoarding.view

import android.os.Bundle
import android.view.View
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.activity_on_boarding.*
import org.koin.android.ext.android.inject
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseActivity
import rs.highlande.app.tatatu.core.util.AnalyticsUtils
import rs.highlande.app.tatatu.core.util.PreferenceHelper
import rs.highlande.app.tatatu.feature.authentication.AuthActivity
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager
import rs.highlande.app.tatatu.feature.onBoarding.view.adapter.ViewPagerAdapter

/**
 * Created by Abhin.
 */

class OnBoardingActivity : BaseActivity() {

    private val preferenceHelper: PreferenceHelper by inject()
    private val authManager: AuthManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_boarding)

        val introViewPagerAdapter = ViewPagerAdapter(this, supportFragmentManager)
        vp_on_boarding.adapter = introViewPagerAdapter
        tb_indicator.setupWithViewPager(vp_on_boarding)
        setButtonLayout()
    }

    override fun onStart() {
        super.onStart()

        AnalyticsUtils.trackScreen(logTag)

    }

    private fun setButtonLayout() {
        vp_on_boarding.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                if (position == 4) {
                    btn_get_started.visibility = View.VISIBLE
                    txt_skip.visibility = View.INVISIBLE
                } else {
                    btn_get_started.visibility = View.INVISIBLE
                    txt_skip.visibility = View.VISIBLE
                }
            }
        })

        txt_skip.setOnClickListener {
            openAuthActivity()
        }
        btn_get_started.setOnClickListener {
            openAuthActivity()
        }
    }

    //save the data in shared Preference with help of new shared Object
    private fun openAuthActivity() {
        preferenceHelper.setOnBoard(true)

        if (authManager.hasPendingInvitation())
            AuthActivity.openSignupFragment(this)
        else
            AuthActivity.openLoginFragment(this)

        this@OnBoardingActivity.finish()
    }

    override fun configureLayout() {}
    override fun bindLayout() {}
    override fun manageIntent() {}


    companion object {
        val logTag = OnBoardingActivity::class.java.simpleName
    }

}