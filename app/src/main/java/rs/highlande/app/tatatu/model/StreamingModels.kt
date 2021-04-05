package rs.highlande.app.tatatu.model

/**
 * Class holding the information of a streaming category to filter home.
 * @author mbaldrighi on 2019-07-01.
 */
data class StreamingCategory(
    val label: String,
    val dataString: String
) : HomeDataObject, BaseDiffutilImpl<StreamingCategory> {



    override fun areItemsTheSame(other: StreamingCategory): Boolean {
        return label == other.label
    }

    override fun areContentsTheSame(other: StreamingCategory): Boolean {
        return label == other.label
    }
}

/**
 * Class holding the information of a generic video to be streamed.
 * @author mbaldrighi on 2019-07-02.
 */
open class StreamingVideo(
    val streamingID: String,
    val posterURL: String = ""
) : HomeDataObject

/**
 * Class holding the information of a video of [HomeUIType.SERIES] type to be streamed.
 * @author mbaldrighi on 2019-07-02.
 */
class StreamingSeries(
    streamingID: String,
    posterURL: String,
    val poster2URL: String = ""
) : StreamingVideo(streamingID, posterURL)

/**
 * Class holding the information of a video of [HomeUIType.MUSIC_VIDEOS] type to be streamed.
 * @author mbaldrighi on 2019-07-02.
 */
class StreamingMusicVideos(
    streamingID: String, posterURL: String, val label: String
) : StreamingVideo(streamingID, posterURL)