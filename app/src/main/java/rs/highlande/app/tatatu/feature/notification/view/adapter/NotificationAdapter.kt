package rs.highlande.app.tatatu.feature.notification.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.clear
import rs.highlande.app.tatatu.databinding.BigNotificationViewModelBinding
import rs.highlande.app.tatatu.databinding.NotificationViewModelBinding
import rs.highlande.app.tatatu.model.NotificationResponse

/**
 * Created by Abhin.
 */

class NotificationAdapter(private var mList: List<NotificationResponse>, private var mItemClickListener: ItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var oldPosition: Int = 0
    private var mNotificationViewModelBinding: NotificationViewModelBinding? = null
    private var mBigNotificationViewModelBinding: BigNotificationViewModelBinding? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SMALL -> {
                mNotificationViewModelBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_notification, parent, false)
                NotificationSmallAdapterViewHolder(mNotificationViewModelBinding!!)
            }
            TYPE_BIG -> {
                mBigNotificationViewModelBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_notification_big, parent, false)
                NotificationBigAdapterViewHolder(mBigNotificationViewModelBinding!!)
            }
            else -> null!!
        }
    }

    override fun getItemViewType(position: Int): Int {
        return mList[position].viewType
    }

    override fun getItemCount() = mList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_SMALL -> {
                (holder as NotificationSmallAdapterViewHolder)
                holder.dataBind(mList[position])
            }

            TYPE_BIG -> {
                (holder as NotificationBigAdapterViewHolder)
                holder.dataBind(mList[position])
            }
        }
        holder.itemView.setOnClickListener {
            mItemClickListener.itemClick(position)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder.itemViewType) {
            TYPE_BIG -> {
                ((holder as NotificationBigAdapterViewHolder).mBigNotificationViewModelBinding.imgNotificationBigPicture as ImageView).clear()
            }
        }
        super.onViewRecycled(holder)
    }

    inner class NotificationSmallAdapterViewHolder(var mNotificationViewModelBinding: NotificationViewModelBinding) : RecyclerView.ViewHolder(mNotificationViewModelBinding.root) {
        fun dataBind(list: NotificationResponse) = mNotificationViewModelBinding.apply {
            data = list
            executePendingBindings()
        }
    }

    inner class NotificationBigAdapterViewHolder(var mBigNotificationViewModelBinding: BigNotificationViewModelBinding) : RecyclerView.ViewHolder(mBigNotificationViewModelBinding.root) {
        fun dataBind(list: NotificationResponse) = mBigNotificationViewModelBinding.apply {
            data = list
            executePendingBindings()
        }
    }

    interface ItemClickListener {
        fun itemClick(position: Int)
    }


    companion object {
        const val TYPE_SMALL = 0
        const val TYPE_BIG = 1
    }
}




