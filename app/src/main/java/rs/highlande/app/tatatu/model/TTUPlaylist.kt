package rs.highlande.app.tatatu.model

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * Holds reference of all properties related to a playlist object of Brightcove media content.
 * @author mbaldrighi on 2019-07-14.
 */
class TTUPlaylist {

    var id = ""

    var name = ""

    private var type = ""
        private set(value) {
            field = value
            typeEnum = StreamingContentType.toEnum(value)
        }
    var typeEnum: StreamingContentType? = null

    var description: String? = null

    @SerializedName(value = "account_id")
    var accountId = ""

    @SerializedName(value = "reference_id")
    var referenceId: String? = null

    @SerializedName(value = "updated_at")
    private var updatedAt = ""
    var updateDate = Date()

    @SerializedName(value = "created_at")
    private var createdAt = ""
    var creationDate = Date()

    @SerializedName(value = "count")
    var count: Int? = null

    var videos = mutableListOf<TTUVideo>()

}


enum class StreamingContentType(val value: String) {

    MOVIES("movies"),
    SERIES("series"),
    GAMES("games"),
    SPORTS("sports"),
    CELEBRITIES("celebrities"),
    FASHION("fashion");


    companion object {
        fun toEnum(value: String?): StreamingContentType? {
            value?.let {
                return when (it) {
                     MOVIES.value -> MOVIES
                     SERIES.value -> SERIES
                     SPORTS.value -> SPORTS
                     GAMES.value -> GAMES
                     CELEBRITIES.value -> CELEBRITIES
                     FASHION.value -> FASHION
                    else -> null
                }
            }

            return null
        }
    }

}



/**
 * Constant map containing all the possible IDs for playlists.
 */
var playlistIds = mutableMapOf<HomeNavigationData, List<String>>()
