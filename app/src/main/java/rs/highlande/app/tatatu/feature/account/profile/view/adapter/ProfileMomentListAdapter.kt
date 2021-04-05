package rs.highlande.app.tatatu.feature.account.profile.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.setPicture
import rs.highlande.app.tatatu.databinding.ProfileMomentItemBinding
import rs.highlande.app.tatatu.databinding.ProfileNewMomentItemBinding
import rs.highlande.app.tatatu.model.Post
import rs.highlande.app.tatatu.model.ProfileListItem

class ProfileMomentListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_NEW_MOMENT = 0
        const val VIEW_TYPE_REGULAR = 1
    }

    private val momentsList = ArrayList<ProfileListItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_NEW_MOMENT) {
            ProfileNewMomentViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.profile_new_moment_item, parent, false
                )
            )
        } else {
            ProfileMomentViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.profile_moment_item, parent, false
                )
            )
        }
    }

    override fun getItemCount() = momentsList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewType = getItemViewType(position)
        if (viewType == ProfilePostGridAdapter.VIEW_TYPE_NEW_POST) {
            (holder as ProfileNewMomentViewHolder).itemBinding.profileMomentItem.setOnClickListener {
                Toast.makeText(it.context, "New Moment", Toast.LENGTH_LONG).show()
            }
        } else {
            with((holder as ProfileMomentViewHolder).itemBinding) {
                val postItem = momentsList[position]
                profileMomentItemImageview.setPicture((postItem as Post).getPostPreview())
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return with(momentsList[position]) {
            if ((this as Post).isCreateNewPost()) {
                VIEW_TYPE_NEW_MOMENT
            } else {
                VIEW_TYPE_REGULAR
            }
        }
    }

    fun addItems(momentList: MutableList<ProfileListItem>) {
        this.momentsList.addAll(momentList)
        notifyDataSetChanged()
    }

    class ProfileMomentViewHolder(val itemBinding: ProfileMomentItemBinding): RecyclerView.ViewHolder(itemBinding.root)

    class ProfileNewMomentViewHolder(val itemBinding: ProfileNewMomentItemBinding): RecyclerView.ViewHolder(itemBinding.root)

}