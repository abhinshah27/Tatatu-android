package rs.highlande.app.tatatu.core.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.get
import androidx.databinding.DataBindingUtil
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.profile_tab_item.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.databinding.ProfileTabItemBinding


class ProfileTabView: TabLayout {

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, R.style.ProfileTabLayout) {
        init()
    }

    private val icons = intArrayOf(R.drawable.post_tab_selector, R.drawable.tag_tab_selector, R.drawable.moment_tab_selector)
    private val labels = intArrayOf(R.string.profile_posts, R.string.profile_tags, R.string.profile_moments)

    @SuppressLint("ResourceAsColor")
    private fun init() {
        for (i in icons.indices) {
            DataBindingUtil.bind<ProfileTabItemBinding>((context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                .inflate(R.layout.profile_tab_item, null))?.let { tabBinding ->

                tabBinding.profileTabItemImageview.setImageDrawable(context.resources.getDrawable(icons[i], context.theme))
                tabBinding.profileTabItemTextview.text = context.resources.getText(labels[i])

                newTab().apply { addTab(this.setCustomView(tabBinding.root), i) }
            }
        }
    }

    fun setTabItemCount(tabPosition: Int, itemCount: Int) {
        getTabAt(tabPosition)?.let { tab ->
            DataBindingUtil.getBinding<ProfileTabItemBinding>(tab.customView!!)?.let {
                it.profileTabItemTextview.text = context.resources.getString(labels[tabPosition]).plus(" ").plus(itemCount)
            }
        }
    }
}