package rs.highlande.app.tatatu.core.ui.recyclerView

import android.view.View
import com.google.android.exoplayer2.ui.PlayerView
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.getVisiblePercentage
import rs.highlande.app.tatatu.feature.post.timeline.adapter.PostTimelineAdapter
import java.util.concurrent.ConcurrentHashMap

/**
 * Base adapter class to handle video playing. Uses [androidx.recyclerview.widget.DiffUtil] implementation.
 * @author mbaldrighi on 2019-08-09.
 */
abstract class BaseVideoPlayerDiffAdapter <T : VideoObject, L : BaseRecyclerListener, VH : BaseViewHolder<T, L>> (
    diffCallback: BaseDiffUtilCallback<T>,
    clickListener: L
) : BaseRecViewDiffAdapter<T, L, VH>(diffCallback, clickListener) {

    var playingVideos = ConcurrentHashMap<String, Long>()
    protected var playingViews = ConcurrentHashMap<PlayerView, VideoViewHolder>()


    val videoVisibilityScrollListener =
        object : ViewVisibilityTrackerListener<PlayerView>(object : OnVisibilityChangeListener {
            override fun onVisibilityChange(views: Map<View, Float>) {
                views.forEach { entry ->
                    rs.highlande.app.tatatu.core.util.LogUtils.d(PostTimelineAdapter.logTag, "VIDEO_TEST: visibility changed for view ${entry.key} by ${entry.value}")

                    playingViews[entry.key]?.let { vvh ->

                        when {
                            entry.value > PLAYABLE_THRESHOLD -> {
                                if (!isVideoCurrentlyPlaying() && vvh.videoObject != null && !vvh.hasVideoBeenPausedByUser())
                                    vvh.startVideo(playingVideos[vvh.videoObject!!.getUniqueID()])
                            }
                            entry.value < PLAYABLE_THRESHOLD -> {
                                val cache = vvh.pauseVideo()
                                playingVideos[cache.first] = cache.second
                            }
                        }
                    }

                }
            }
        }) {}


    override fun onBindViewHolder(holder: VH, position: Int) {
        if (holder is VideoViewHolder)
            holder.onBindVideoItem(getItems()[position], isVideoCurrentlyPlaying())
        else
            super.onBindViewHolder(holder, position)
    }


    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)

        (holder as? VideoViewHolder)?.let { vvh ->
            LogUtils.d(logTag, "VIDEO_TEST: view RECYCLED for VH: $holder")

            vvh.onRecycle()
        }

    }

    override fun onViewDetachedFromWindow(holder: VH) {
        super.onViewDetachedFromWindow(holder)

        (holder as? VideoViewHolder)?.let { vvh ->
            LogUtils.d(logTag, "VIDEO_TEST: view ATTACHED for VH: $holder")
            playingViews.remove(vvh.getVideoView())
            videoVisibilityScrollListener.removeView(vvh.getVideoView())
        }
    }

    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)

        (holder as? VideoViewHolder)?.let { vvh ->
            LogUtils.d(logTag, "VIDEO_TEST: view ATTACHED for VH: $holder")
            videoVisibilityScrollListener.addView(vvh.getVideoView())
            playingViews[vvh.getVideoView()] = vvh

            if (vvh.getVideoView().getVisiblePercentage() > PLAYABLE_THRESHOLD)
                vvh.startVideo(playingVideos[vvh.videoObject?.getUniqueID()])
        }
    }

    fun stopVideos(release: Boolean) {
        playingViews.forEach {
            if (release) it.value.stopVideo()
            else it.value.pauseVideo()
        }
    }

    private fun isVideoCurrentlyPlaying() = playingViews.filterValues { it.isPlaying() }.isNotEmpty()

    private fun canAutoPlay(view: PlayerView) = (view.getVisiblePercentage() > PLAYABLE_THRESHOLD) && !isVideoCurrentlyPlaying()


    fun cachePositions() {
        playingViews.forEach {
            val holder = it.value
            holder.videoObject?.let { obj ->
                playingVideos[obj.getUniqueID()] = holder.getCurrentPosition()
            }
        }
    }


    companion object {
        val logTag = BaseVideoPlayerDiffAdapter::class.java.simpleName
    }

}