package rs.highlande.app.tatatu.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseDiffUtilCallback
import rs.highlande.app.tatatu.core.util.getDateFromDB

/**
 * Holds reference of all properties related to a video object of Brightcove media content.
 * @author mbaldrighi on 2019-07-12.
 */
@Parcelize
class TTUVideo(

    var id: String = "",
    var description: String? = null,
    @SerializedName(value = "long_description")
    var longDescription: String? = null,
    var tags: List<String> = listOf(),
    @SerializedName(value = "custom_fields")
    var details: TTUVideoFields = TTUVideoFields(),
    @SerializedName(value = "account_id")
    var accountId: String = "",
    var name: String = "",
    @SerializedName(value = "reference_id")
    var referenceId: String? = null,
    var duration: Long = 0,
    var economics: String = "",
    @SerializedName(value = "published_at")
    private var publishedAt: String = "",
    @SerializedName(value = "updated_at")
    private var updatedAt: String = "",
    @SerializedName(value = "created_at")
    private var createdAt: String = "",
    var thumbnail: String = "",
    var poster: String = "",
    var link: String? = null,
    @SerializedName(value = "offline_enabled")
    var offlineEnabled: Boolean = false,
    @SerializedName(value = "ad_keys")
    var adKeys: List<String>? = listOf()

) : HomeDataObject, BaseDiffutilImpl<TTUVideo>, Parcelable {


    constructor(): this("")

    @IgnoredOnParcel var publicationDate = publishedAt.getDateFromDB()
    @IgnoredOnParcel var creationDate = createdAt.getDateFromDB()
    @IgnoredOnParcel var updateDate = updatedAt.getDateFromDB()


//    var id = ""
//
//    var description: String? = null
//    @SerializedName(value = "long_description")
//    var longDescription: String? = null
//
//    var tags = listOf<String>()
//
//    @SerializedName(value = "custom_fields")
//    var details = TTUVideoFields()
//
//    @SerializedName(value = "account_id")
//    var accountId = ""
//
//    var name = ""
//
//    @SerializedName(value = "reference_id")
//    var referenceId: String? = null
//
//    var duration = 0
//    var economics = ""
//
//    @SerializedName(value = "published_at")
//    private var publishedAt = ""
//        private set(value) {
//            publicationDate = value.getDateFromDB() ?: Date()
//            field = value
//        }
//    var publicationDate = Date()
//
//    @SerializedName(value = "updated_at")
//    private var updatedAt = ""
//        private set(value) {
//            updateDate = value.getDateFromDB() ?: Date()
//            field = value
//        }
//    var updateDate = Date()
//
//    @SerializedName(value = "created_at")
//    private var createdAt = ""
//        private set(value) {
//            creationDate = value.getDateFromDB() ?: Date()
//            field = value
//        }
//    var creationDate = Date()
//
//    var thumbnail = ""
//    var poster = ""
//    var link: String? = null
//
//    @SerializedName(value = "offline_enabled")
//    var offlineEnabled = false
//
//    @SerializedName(value = "ad_keys")
//    var adKeys = listOf<String>()


    // left out:
    /*
     * "poster_sources"
     * "cue_points"
     * "sources"
     * "text_tracks"
     * "thumbnail_sources"
     */


    override fun areItemsTheSame(other: TTUVideo): Boolean {
        return id == id
    }

    override fun areContentsTheSame(other: TTUVideo): Boolean {
        return poster == other.poster
    }


    class TTUVideoDiffCallback : BaseDiffUtilCallback<TTUVideo>() {
        override fun areItemsTheSame(oldItem: TTUVideo, newItem: TTUVideo): Boolean {
            return oldItem.areItemsTheSame(newItem)
        }

        override fun areContentsTheSame(oldItem: TTUVideo, newItem: TTUVideo): Boolean {
            return oldItem.areContentsTheSame(newItem)
        }
    }

}

@Parcelize
class TTUVideoFields(

    var genre: String = "",
    @SerializedName(value = "genre_2")
    var genre2: String = "",
    @SerializedName(value = "genre_3")
    var genre3: String = "",

    var director: String = "",

    var cast: String = "",

    var duration: String = "",

    @SerializedName(value = "original_language")
    var originalLanguage: String = "",

    @SerializedName(value = "subtitle_language")
    var subtitleLanguage: String = "",

    @SerializedName(value = "content_category")
    var category: String = "",

    var territory: String = "",

    @SerializedName(value = "geo_deny")
    var geoDeny: String = "",

    @SerializedName(value = "geo_allow")
    var geoAllow: String = "",

    var quality: String = "",

    @SerializedName(value = "publish_date")
    var publishDate: String = "",

    @SerializedName(value = "audio_language")
    var audioLanguage: String = "",

    @SerializedName(value = "rights_holder")
    var rightsHolder: String = "",

    @SerializedName(value = "geo_unrestricted")
    var geoUnrestricted: String = ""

) : Parcelable {

    constructor(): this("")

    fun getDetailsString(): String {

        // TODO: 2019-07-21    using also genre2 and genre3?

        return StringBuilder().let {
            if (!territory.isBlank())
               it.append(territory)
            if (!publishDate.isBlank()) {
                if (it.isNotEmpty())
                    it.append(", ")
                it.append(publishDate)
            }
            if (!genre.isBlank()) {
                if (it.isNotEmpty())
                   it.append(" | ")
                it.append(genre)
            }
            it.toString()
        }

    }

}


// TODO: 2019-07-12     TEXT TRACKS?
//{
//    "mime_type": null,
//    "account_id": "20318290001",
//    "default": true,
//    "sources": [
//    {
//        "src": "https://static.3playmedia.com/p/files/2240988/threeplay_transcripts/6418302.vtt?project_id=10127&format_id=51&refresh=1515241856"
//    }
//    ],
//    "src": "https://static.3playmedia.com/p/files/2240988/threeplay_transcripts/6418302.vtt?project_id=10127&format_id=51&refresh=1515241856",
//    "asset_id": null,
//    "label": "English",
//    "id": "13d66def-d004-4e75-b160-4ac693eff7d7",
//    "kind": "captions",
//    "srclang": "en"
//}