package rs.highlande.app.tatatu.feature.multimediaContent.view

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseRecViewAdapter
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseViewHolder
import rs.highlande.app.tatatu.core.ui.recyclerView.OnItemClickListenerWithView
import rs.highlande.app.tatatu.core.util.setPicture
import rs.highlande.app.tatatu.databinding.ItemMultimediaVideoBinding
import rs.highlande.app.tatatu.model.TTUVideo

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-07-18.
 */
class PlaylistAdapter(
    clickListener: OnItemClickListenerWithView<TTUVideo>
) : BaseRecViewAdapter<TTUVideo, OnItemClickListenerWithView<TTUVideo>, PlaylistAdapter.VideoVH>(clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoVH {
        return VideoVH(
            DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_multimedia_video, parent, false)
        )
    }

    inner class VideoVH(private val binding: ItemMultimediaVideoBinding) : BaseViewHolder<TTUVideo, OnItemClickListenerWithView<TTUVideo>>(binding.root) {

        override fun onBind(item: TTUVideo, listener: OnItemClickListenerWithView<TTUVideo>?) {

            // FIXME: 2019-07-22    shared element transition
//            binding.preview.transitionName = item.id

            binding.preview.setPicture(item.poster)
            binding.executePendingBindings()

            if (listener != null) {
                binding.root.setOnClickListener { listener.onItemClick(binding.preview, item) }
            }
        }

        override fun getImageViewsToRecycle(): Set<ImageView?> {
            return setOf(
                binding.preview
            )
        }
    }

}