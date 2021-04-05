package rs.highlande.app.tatatu.feature.account.settings.view

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_complete_your_profile.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.BaseFragment
import rs.highlande.app.tatatu.core.util.resolveColorAttribute
import rs.highlande.app.tatatu.databinding.SettingsCompleteYourProfileViewModelBinding
import rs.highlande.app.tatatu.feature.account.settings.view.viewModel.SettingsCompleteYourProfileViewModel

/**
 * Created by Abhin.
 */

class SettingsCompleteYourProfileFragment : BaseFragment() {
    private val mSettings: SettingsCompleteYourProfileViewModel by viewModel()
    private var mBinding: SettingsCompleteYourProfileViewModelBinding? = null
    private var bold: Typeface? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_complete_your_profile, container, false)
        mBinding?.lifecycleOwner = this // view model attach with lifecycle
        mBinding?.mViewModel = mSettings //setting up view model
        return mBinding!!.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObserver()
    }

    private fun initObserver() {
        mSettings.mErrorShow.observe(this, Observer {
            if (it != null && it != -1) {
                val message = when (it) {
                    101 -> resources.getString(R.string.btn_update_profile)
                    102 -> resources.getString(R.string.btn_update_later)
                    else -> ""
                }
                showError(message)
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()

        changeSelectedTextColor(100.toString())
    }

    private fun init() {

    }

    private fun changeSelectedTextColor(ttuPoint: String) {

        val terms = SpannableString(context!!.resources.getString(R.string.msg_complete_your_profile, ttuPoint))
        terms.setSpan(object : MetricAffectingSpan() {
            override fun updateMeasureState(p0: TextPaint) {
                setTextColor(p0)
            }

            override fun updateDrawState(textPaint: TextPaint) {
                setTextColor(textPaint)
            }
        }, 31, 40 + ttuPoint.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        txt_complete_profile_msg.setText(terms, TextView.BufferType.SPANNABLE) // set the above text with Buffer
    }

    //set the color and false underline[underline are default]
    fun setTextColor(textPaint: TextPaint) {
        bold = ResourcesCompat.getFont(context!!, R.font.lato_bold)
        textPaint.color = resolveColorAttribute(context!!, R.attr.textColorPrimary)
        textPaint.typeface = bold
        textPaint.isUnderlineText = false
    }

    override fun configureLayout(view: View) {}
    override fun bindLayout() {}
}