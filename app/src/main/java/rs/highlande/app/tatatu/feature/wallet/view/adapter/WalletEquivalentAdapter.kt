package rs.highlande.app.tatatu.feature.wallet.view.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.resolveColorAttribute
import rs.highlande.app.tatatu.databinding.EquivalentViewModelBinding
import rs.highlande.app.tatatu.model.Equivalent

/**
 * Created by Abhin.
 */

class WalletEquivalentAdapter(private val mContext: Context, private var mList: List<Equivalent>, private var mItemClickListener: ItemClickListener) : RecyclerView.Adapter<WalletEquivalentAdapter.DisplayAdapterViewHolder>() {

    var oldPosition: Int = 0
    private var mCreatePostImageViewModelBinding: EquivalentViewModelBinding? = null
    private var regular: Typeface? = null
    private var bold: Typeface? = null

    init {
        regular = ResourcesCompat.getFont(mContext, R.font.lato)
        bold = ResourcesCompat.getFont(mContext, R.font.lato_bold)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayAdapterViewHolder {
        mCreatePostImageViewModelBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_equivalent_to, parent, false)
        return DisplayAdapterViewHolder(mCreatePostImageViewModelBinding!!)
    }

    override fun getItemCount() = mList.size

    override fun onBindViewHolder(holder: DisplayAdapterViewHolder, position: Int) {
        holder.displayBind(mList[position])
        setSelectionUI(holder.mCreatePostDisplayModelBinding.imgChecked, position, holder.mCreatePostDisplayModelBinding.txtName, mContext)
        holder.itemView.setOnClickListener {
            mItemClickListener.itemClick(position)
            userSelection(holder.mCreatePostDisplayModelBinding.imgChecked, position)
        }
    }

    //set the selected position
    private fun setSelectionUI(img: AppCompatImageView, position: Int, txt: AppCompatTextView, mContext: Context) {
        if (mList[position].isSelected) {
            img.visibility = View.VISIBLE
            txt.setTextColor(resolveColorAttribute(mContext, R.attr.textColorPrimary))
            txt.typeface = Typeface.create(bold, Typeface.NORMAL)
        } else {
            img.visibility = View.GONE
            txt.setTextColor(resolveColorAttribute(mContext, R.attr.textColorPrimary))
            txt.typeface = Typeface.create(regular, Typeface.NORMAL)
        }
    }

    //set the selected position on item Click
    private fun userSelection(img: AppCompatImageView, position: Int) {
        if (position != oldPosition) {
            mList[position].isSelected = true
            img.visibility = View.VISIBLE

            if (oldPosition != -1) {
                mList[oldPosition].isSelected = false
                notifyItemChanged(oldPosition)
                notifyItemChanged(position)
            }
            oldPosition = position
        }
    }

    inner class DisplayAdapterViewHolder(var mCreatePostDisplayModelBinding: EquivalentViewModelBinding) : RecyclerView.ViewHolder(mCreatePostDisplayModelBinding.root) {
        fun displayBind(list: Equivalent) = mCreatePostDisplayModelBinding.apply {
            data = list
            executePendingBindings()
        }
    }

    interface ItemClickListener {
        fun itemClick(position: Int)
    }
}




