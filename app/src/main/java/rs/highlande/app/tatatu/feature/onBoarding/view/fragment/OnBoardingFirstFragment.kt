package rs.highlande.app.tatatu.feature.onBoarding.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_on_boarding_first.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_ON_BOARD_IMAGE
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_ON_BOARD_SUB_TITLE
import rs.highlande.app.tatatu.core.util.BUNDLE_KEY_ON_BOARD_TITLE


/**
 * Created by Abhin.
 */
class OnBoardingFirstFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_on_boarding_first, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    private fun init() {
        if (arguments != null) {
            val mTitle = arguments?.getString(BUNDLE_KEY_ON_BOARD_TITLE, "")
            val mSubTitle = arguments?.getString(BUNDLE_KEY_ON_BOARD_SUB_TITLE, "")
            val mImage = arguments?.getInt(BUNDLE_KEY_ON_BOARD_IMAGE, R.drawable.ic_better_than_free_illustration)
            setUI(mImage!!, mTitle!!, mSubTitle!!)
        }
    }

    private fun setUI(imgUrl: Int = R.drawable.on_board_first, title: String = resources.getString(R.string.title_welcome), subTitle: String = resources.getString(R.string.sub_title_choose_from)) {
        img_on_boarding.setImageResource(imgUrl)
        txt_title_on_boarding.text = title
        txt_sub_title_on_boarding.text = subTitle
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}
}