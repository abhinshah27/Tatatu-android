package rs.highlande.app.tatatu.feature.inviteFriends.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.clear
import rs.highlande.app.tatatu.databinding.InviteContactViewModelBinding
import rs.highlande.app.tatatu.model.ContactList

/**
 * Created by Abhin.
 */

class ContactAdapter(private var mList: List<ContactList>, private var mItemClickListener: ItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mInviteContactViewModelBinding: InviteContactViewModelBinding? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        mInviteContactViewModelBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_contact, parent, false)
        return ContactAdapterViewHolder(mInviteContactViewModelBinding!!)
    }

    override fun getItemCount() = mList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ContactAdapterViewHolder)
        holder.imageBind(mList[position])

        holder.mInviteContactViewModelBinding.btnContactInvite.setOnClickListener {
            mItemClickListener.itemClick(position)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        ((holder as ContactAdapterViewHolder).mInviteContactViewModelBinding.imgContact as ImageView).clear()
        super.onViewRecycled(holder)
    }

    inner class ContactAdapterViewHolder(var mInviteContactViewModelBinding: InviteContactViewModelBinding) : RecyclerView.ViewHolder(mInviteContactViewModelBinding.root) {
        fun imageBind(list: ContactList) = mInviteContactViewModelBinding.apply {
            data = list
            executePendingBindings()
        }
    }

    interface ItemClickListener {
        fun itemClick(position: Int)
    }

}




