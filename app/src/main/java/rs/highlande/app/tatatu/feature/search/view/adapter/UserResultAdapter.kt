package rs.highlande.app.tatatu.feature.search.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.common_profile_picture.view.*
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListener
import rs.highlande.app.tatatu.core.util.setProfilePicture
import rs.highlande.app.tatatu.databinding.ItemUserResultBinding
import rs.highlande.app.tatatu.model.User

/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */
class UserResultAdapter(clickListener: OnItemClickListener<User>):
    BaseRecViewAdapter<User, OnItemClickListener<User>, UserResultAdapter.UserVH>(clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UserVH(
        DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_user_result, parent, false))

    inner class UserVH(private val binding: ItemUserResultBinding) : BaseViewHolder<User, OnItemClickListener<User>>(binding.root) {
        override fun onBind(item: User, listener: OnItemClickListener<User>?) {
            with(item) {
                binding.profilePicture.picture.setProfilePicture(picture)
                binding.name.text = name
                binding.profilePicture.celebrityIndicator.visibility =
                    if (isCelebrity()) { View.VISIBLE } else { View.GONE }

                binding.root.setOnClickListener {
                    listener!!.onItemClick(item)
                }
            }
        }

        override fun getImageViewsToRecycle() = setOf(binding.profilePicture.picture)
    }

}