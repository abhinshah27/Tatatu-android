package rs.highlande.app.tatatu.feature.account.settings.view.blockAccounts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.databinding.BlockAccountViewModelBinding
import rs.highlande.app.tatatu.model.BlockAccountDataList


/**
 * Created by Abhin.
 */
class SettingsBlockAccountsAdapter(private val mContext: Context, var mList: ArrayList<BlockAccountDataList>, var mItemClickListener: ItemClickListener) : RecyclerView.Adapter<SettingsBlockAccountsAdapter.BlockAccountsAdapterViewHolder>() {

    private var mBlockAccountViewModelBinding: BlockAccountViewModelBinding? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockAccountsAdapterViewHolder {
        mBlockAccountViewModelBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_block_user, parent, false)
        return BlockAccountsAdapterViewHolder(mBlockAccountViewModelBinding!!)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: BlockAccountsAdapterViewHolder, position: Int) {
        val mData = mList[position]
        holder.displayBind(mData)
        //        holder.mName.text = mData.username
        //        holder.mDes.text = mData.name
        //        Glide.with(mContext).load(mData.picture).apply(RequestOptions().circleCrop()).into(holder.mImages)

        holder.mConfirm.setOnClickListener {
            mItemClickListener.itemClick(position)
        }

        if (position == mList.size - 1) {
            holder.mDivider.visibility = View.INVISIBLE
        } else {
            holder.mDivider.visibility = View.VISIBLE
        }
    }

    class BlockAccountsAdapterViewHolder(var mBlockAccountViewModelBinding: BlockAccountViewModelBinding) : RecyclerView.ViewHolder(mBlockAccountViewModelBinding.root) {
        fun displayBind(list: BlockAccountDataList) = mBlockAccountViewModelBinding.apply {
            mViewModel = list
            executePendingBindings()
        }
        val mConfirm = itemView.findViewById<AppCompatTextView>(R.id.btn_Confirm)!!
        val mDivider = itemView.findViewById<View>(R.id.item_divider)!!
    }

    interface ItemClickListener {
        fun itemClick(position: Int)
    }
}