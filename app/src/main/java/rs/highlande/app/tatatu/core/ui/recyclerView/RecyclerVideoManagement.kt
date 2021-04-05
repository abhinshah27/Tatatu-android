package rs.highlande.app.tatatu.core.ui.recyclerView

import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ui.PlayerView
import org.koin.core.KoinComponent
import org.koin.core.inject
import rs.highlande.app.tatatu.core.cache.AudioVideoCache
import rs.highlande.app.tatatu.core.util.VideoPlayerHelper
import rs.highlande.app.tatatu.core.util.getVisiblePercentage

/**
 * TODO - Class description
 * @author mbaldrighi on 2019-08-08.
 */
abstract class ViewVisibilityTrackerListener <T : View> (
    private val listener: OnVisibilityChangeListener
) : RecyclerView.OnScrollListener() {

    private val views = mutableSetOf<T>()


    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (dy != 0) {

            val tmp = mutableMapOf<View, Float>()

            views.forEach {
                val percent = it.getVisiblePercentage()
                tmp[it] = percent
            }

            listener.onVisibilityChange(tmp)
        }
    }

    fun addView(view: T?) = view?.let {
        views.add(view)
    }

    fun removeView(view: T?) = views.remove(view)


    interface OnVisibilityChangeListener {
        fun onVisibilityChange(views: Map<View, Float>)
    }
}


const val PLAYABLE_THRESHOLD: Float = 0.4f


/**
 * Interface to be implemented by view holders that will manage video playing.
 * [VideoViewHolderManager] will usually be the delegate for this implementation.
 */
interface VideoViewHolder: VideoPlayerHelper.IsVideoPauseByUser {

    /**
     * Current [VideoObject] in use.
     */
    var videoObject: VideoObject?

    /**
     * @return True if the current class is playing a media, false otherwise
     */
    fun isPlaying(): Boolean

    /**
     * Pauses the current video.
     * @return The String(uid)-Long(currentPosition) [Pair] that serves for cache purposes.
     */
    fun pauseVideo(): Pair<String, Long>

    /**
     * Starts the current video.
     * @param cachedPosition the position to be sought prior the actual playing.
     */
    fun startVideo(cachedPosition: Long?)

    /**
     * Pauses the current video and releases the resources.
     */
    fun stopVideo()

    /**
     * @return The cache String-Long [Pair] for current video.
     */
    fun getCachePair(): Pair<String, Long>

    /**
     * @return The last position for current video.
     */
    fun getCurrentPosition(): Long

    /**
     * Reacts to click action on video
     */
    fun onClick()

    /**
     * Initializes ExoPlayer instance and prepares resources.
     * @param play Whether the video must be started automatically, as soon as it reaches the state [com.google.android.exoplayer2.Player.STATE_READY].
     */
    fun videoPlay(play: Boolean)

    /**
     * @return The [PlayerView] currently used by ViewHolder.
     */
    fun getVideoView(): PlayerView

    /**
     * @return The "play" icon currently used by ViewHolder.
     */
    fun getPlayIcon(): AppCompatImageView

    /**
     * Sets the [PlayerView] currently used by ViewHolder.
     * @param playerView
     */
    fun setVideoView(playerView: PlayerView)

    /**
     * Sets the "play" icon currently used by ViewHolder.
     * @param playIcon
     */
    fun setPlayIcon(playIcon: AppCompatImageView)

    /**
     * Default implementation for actions when ViewHolder is recycled.
     */
    fun onRecycle() {
        stopVideo()
        videoObject = null
    }

    /**
     * Implementation of how actually views are bound.
     */
    fun onBindVideoItem(item: VideoObject, hasVideoPlaying: Boolean)

    /**
     * @return whether the video has been paused by user through gesture detection.
     */
    fun hasVideoBeenPausedByUser(): Boolean

    override fun isVideoPauseByUser(isVideoPause: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}


/**
 * Interface to be implemented by all models that hold a playable video.
 */
interface VideoObject {

    /**
     * @return a unique identifier for the linked model.
     */
    fun getUniqueID(): String

    /**
     * @return the URLs to be played.
     */
    fun getVideoUrls(): List<String>?

    /**
     * @return the objects to be played (URL String or cached Uri).
     */
    fun getVideoObject(videoCache: AudioVideoCache): List<Any>?
}


/**
 * Class that serves as delegate of [VideoViewHolder] interface.
 */
class VideoViewHolderManager(
    context: Context
) : VideoViewHolder, KoinComponent {

    private val videoPlayer by lazy { VideoPlayerHelper(context = context) }

    private val videoCache: AudioVideoCache by inject()

    override var videoObject: VideoObject? = null
    private lateinit var playerView: PlayerView
    private lateinit var playIcon: AppCompatImageView


    override fun onBindVideoItem(item: VideoObject, hasVideoPlaying: Boolean) {
        // TODO: 2019-08-09    leave actual implementation to delegating class
    }

    override fun isPlaying() = videoPlayer.isPlaying()

    override fun pauseVideo(): Pair<String, Long> {
        try {
            videoPlayer.pause(playIcon)
            return getCachePair()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return "" to 0L
    }

    override fun startVideo(cachedPosition: Long?) = videoPlayer.start(playIcon, cachedPosition ?: 0)

    override fun stopVideo() {
        videoPlayer.stop()
    }

    override fun getCachePair(): Pair<String, Long> {
        return (videoObject?.getUniqueID() ?: "") to videoPlayer.getCurrentPosition()
    }

    override fun getCurrentPosition() = videoPlayer.getCurrentPosition()

    override fun onClick() = videoPlayer.onClick(playIcon)

    override fun videoPlay(play: Boolean) =
        videoPlayer.videoPlay(videoObject?.getVideoObject(videoCache)?.get(0), playIcon, play)


    override fun getVideoView() = playerView

    override fun getPlayIcon() = playIcon

    override fun setVideoView(playerView: PlayerView) {
        this.playerView = playerView
        videoPlayer.mExoPlayer = playerView
    }

    override fun setPlayIcon(playIcon: AppCompatImageView) {
        this.playIcon = playIcon
    }

    override fun hasVideoBeenPausedByUser() = videoPlayer.hasVideoBeenPausedByUser
}
