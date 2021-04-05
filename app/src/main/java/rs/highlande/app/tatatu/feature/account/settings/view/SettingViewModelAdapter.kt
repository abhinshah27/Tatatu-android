package rs.highlande.app.tatatu.feature.account.settings.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.databinding.SettingsViewModelBinding
import rs.highlande.app.tatatu.model.SettingsList

/**
 * Created by Abhin.
 */
class SettingViewModelAdapter(val mContext: Context, var mList: List<SettingsList>, var mItemClickListener: ItemClickListener) : RecyclerView.Adapter<SettingViewModelAdapter.SettingNormalAdapterAdapterViewHolder>() {

    private var mSettingsViewModelBinding: SettingsViewModelBinding? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingNormalAdapterAdapterViewHolder {
        mSettingsViewModelBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_settings, parent, false)
        return SettingNormalAdapterAdapterViewHolder(mSettingsViewModelBinding!!)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: SettingNormalAdapterAdapterViewHolder, position: Int) {
        val mData = mList[position]
        holder.displayBind(mList[position])

        if (mData.mDescription.isNotEmpty()) {
            holder.mDes.visibility = View.VISIBLE
        } else {
            holder.mDes.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            mItemClickListener.itemClick(position)
        }
        if (position == mList.size - 1) {
            holder.mItemDivider.visibility = View.GONE
        }
    }

    class SettingNormalAdapterAdapterViewHolder(var mSettingsViewModelBinding: SettingsViewModelBinding) : RecyclerView.ViewHolder(mSettingsViewModelBinding.root) {
        fun displayBind(list: SettingsList) = mSettingsViewModelBinding.apply {
            data = list
            executePendingBindings()
        }
        val mItemDivider = itemView.findViewById<View>(R.id.item_divider)!!
        val mDes = itemView.findViewById<AppCompatTextView>(R.id.txt_item_settings_des)!!
    }

    interface ItemClickListener {
        fun itemClick(position: Int)
    }
}









