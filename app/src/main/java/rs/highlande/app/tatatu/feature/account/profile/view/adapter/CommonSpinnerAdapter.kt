package rs.highlande.app.tatatu.feature.account.profile.view.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.resolveColorAttribute
import rs.highlande.app.tatatu.model.CommonSpinnerList


/**
 * Created by Abhin.
 */
class CommonSpinnerAdapter : ArrayAdapter<CommonSpinnerList> {

    private var mContext: Context? = null
    private var mView: View? = null
    private var mList: List<CommonSpinnerList>? = null
    private var italic: Typeface? = null
    private var normal: Typeface? = null

    constructor(context: Context, resource: Int) : super(context, resource) {
        mContext = context
    }

    constructor(context: Context, resource: Int, textViewResourceId: Int) : super(context, resource, textViewResourceId) {
        mContext = context
    }

    constructor(context: Context, resource: Int, objects: Array<CommonSpinnerList>) : super(context, resource, objects) {
        mContext = context
    }

    constructor(context: Context, resource: Int, textViewResourceId: Int, objects: Array<CommonSpinnerList>) : super(context, resource, textViewResourceId, objects) {
        mContext = context
    }

    constructor(context: Context, resource: Int, objects: List<CommonSpinnerList>) : super(context, resource, objects) {
        mContext = context
        mList = objects
        mContext?.let {
            italic = ResourcesCompat.getFont(it, R.font.lato_ltalic)
            normal = ResourcesCompat.getFont(it, R.font.lato)
        }
    }

    constructor(context: Context?, resource: Int, textViewResourceId: Int, objects: List<CommonSpinnerList>) : super(context!!, resource, textViewResourceId, objects) {
        mContext = context
        mList = objects
    }

    override fun getCount(): Int {
        return mList!!.size
    }

    override fun getItem(position: Int): CommonSpinnerList? {
        return mList!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    private fun getCustomView(position: Int, convertView: View?, parent: ViewGroup): View {
        mView = LayoutInflater.from(mContext).inflate(R.layout.item_spinner_settings_profile_selection, parent, false)
        val mName = mView!!.findViewById<AppCompatTextView>(R.id.txt_name)
        mName.text = mList!![position].mName
        val mIndicator = mView!!.findViewById<FrameLayout>(R.id.fl_indicator)

        //set the font family
        if (position == 0) {
            mName.typeface = Typeface.create(italic, Typeface.NORMAL)
        } else {
            mName.typeface = Typeface.create(normal, Typeface.NORMAL)
        }

        //set color on selected position
        if (mList!![position].mSelected) {
            mIndicator.visibility = View.VISIBLE
            mName.setTextColor(resolveColorAttribute(context, R.attr.textColorPrimary))
        } else {
            mIndicator.visibility = View.GONE
            mName.setTextColor(resolveColorAttribute(context, R.attr.textColorSecondary))
        }

        return mView!!
    }

    //disable click on selected position
    override fun isEnabled(position: Int): Boolean {
        return position != 0
    }
}
