package rs.highlande.app.tatatu.feature.createPost.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.clear
import rs.highlande.app.tatatu.databinding.CreatePostDisplayModelBinding
import rs.highlande.app.tatatu.databinding.CreatePostImageViewModelBinding
import rs.highlande.app.tatatu.databinding.CreatePostVideoDurationBinding
import rs.highlande.app.tatatu.model.DataList
import rs.highlande.app.tatatu.model.DataListDisplay
import rs.highlande.app.tatatu.model.DataListVideo

/**
 * Created by Abhin.
 */

class CreatePostAdapter(private var mList: List<DataList>, private var mItemClickListener: ItemClickListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var oldPosition: Int = 0
    private var mCreatePostImageViewModelBinding: CreatePostImageViewModelBinding? = null
    private var mCreatePostVideoDurationBinding: CreatePostVideoDurationBinding? = null
    private var mCreatePostDisplayModelBinding: CreatePostDisplayModelBinding? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_IMAGE -> {
                mCreatePostImageViewModelBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_create_post,
                    parent,
                    false
                )
                DashboardAdapterViewHolder(mCreatePostImageViewModelBinding!!)
            }
            TYPE_VIDEO -> {
                mCreatePostVideoDurationBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_create_video_post,
                    parent,
                    false
                )
                DashboardVideoAdapterViewHolder(mCreatePostVideoDurationBinding!!)
            }
            TYPE_DISPLAY -> {
                mCreatePostDisplayModelBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_create_post_display,
                    parent,
                    false
                )
                DisplayAdapterViewHolder(mCreatePostDisplayModelBinding!!)
            }
            else -> null!!
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mList[position] is DataListDisplay) TYPE_DISPLAY else if (mList[position] is DataListVideo) TYPE_VIDEO else TYPE_IMAGE
    }

    override fun getItemCount() = mList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_IMAGE -> {
                (holder as DashboardAdapterViewHolder)
                holder.imageBind(mList[position])

                setSelectionUI(holder.mCreatePostImageViewModelBinding.imgSelector, position)

                holder.itemView.setOnClickListener {
                    mItemClickListener.itemClick(position)
                    userSelection(holder.mCreatePostImageViewModelBinding.imgSelector, position)
                }
            }

            TYPE_VIDEO -> {
                (holder as DashboardVideoAdapterViewHolder)
                holder.videoBind(mList[position])
                setSelectionUI(holder.mCreatePostVideoDurationBinding.imgSelector, position)
                holder.itemView.setOnClickListener {
                    mItemClickListener.itemClick(position)
                    userSelection(holder.mCreatePostVideoDurationBinding.imgSelector, position)
                }
            }
            TYPE_DISPLAY -> {
                (holder as DisplayAdapterViewHolder)
                holder.displayBind(mList[position])
                holder.itemView.setOnClickListener {

                }
            }
        }
    }

    fun deSelected() {
        if (!mList.isNullOrEmpty() && oldPosition <= mList.size - 1) {
            mList[oldPosition].isSelected = false
            notifyItemChanged(oldPosition)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder.itemViewType) {
            TYPE_IMAGE -> {
                ((holder as DashboardAdapterViewHolder).mCreatePostImageViewModelBinding.imgGallery as ImageView).clear()
            }
            TYPE_VIDEO -> {
                ((holder as DashboardVideoAdapterViewHolder).mCreatePostVideoDurationBinding.imgGallery as ImageView).clear()
            }
        }
        super.onViewRecycled(holder)
    }

    //set the selected position
    private fun setSelectionUI(img: AppCompatImageView, position: Int) {
        if (mList[position].isSelected) img.visibility = View.VISIBLE
        else img.visibility = View.GONE
    }

    //set the selected position on item Click
    private fun userSelection(img: AppCompatImageView, position: Int) {
        if (position != oldPosition) {
            mList[position].isSelected = true
            img.visibility = View.VISIBLE

            if (oldPosition != -1) {
                mList[oldPosition].isSelected = false
                notifyItemChanged(oldPosition)
            }
            oldPosition = position
        }
    }

    inner class DashboardAdapterViewHolder(var mCreatePostImageViewModelBinding: CreatePostImageViewModelBinding) :
        RecyclerView.ViewHolder(mCreatePostImageViewModelBinding.root) {

        fun imageBind(list: DataList) = mCreatePostImageViewModelBinding.apply {
            data = list
            executePendingBindings()
        }
    }

    inner class DashboardVideoAdapterViewHolder(var mCreatePostVideoDurationBinding: CreatePostVideoDurationBinding) :
        RecyclerView.ViewHolder(mCreatePostVideoDurationBinding.root) {
        fun videoBind(list: DataList) = mCreatePostVideoDurationBinding.apply {
            data = list as DataListVideo
            executePendingBindings()
        }
    }

    inner class DisplayAdapterViewHolder(var mCreatePostDisplayModelBinding: CreatePostDisplayModelBinding) :
        RecyclerView.ViewHolder(mCreatePostDisplayModelBinding.root) {
        fun displayBind(list: DataList) = mCreatePostDisplayModelBinding.apply {
            data = list as DataListDisplay
            executePendingBindings()
        }
    }

    interface ItemClickListener {
        fun itemClick(position: Int)
    }


    companion object {
        const val TYPE_IMAGE = 0
        const val TYPE_VIDEO = 1
        const val TYPE_DISPLAY = 2
    }
}




