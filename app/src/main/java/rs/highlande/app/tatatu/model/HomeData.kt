package rs.highlande.app.tatatu.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseDiffUtilCallback
import rs.highlande.app.tatatu.core.util.JsonHelper

/**
 * Holds reference to the properties of an list to be showed in the Home screen.
 * @author mbaldrighi on 2019-07-01.
 */
@Parcelize
class HomeNavigationData(var homeType: HomeUIType? = null, var navigationData: String = "", var sectionTitle: String = "") : Parcelable {

    constructor() : this(null, "", "")

    fun isChannels(): Boolean {
        return homeType == HomeUIType.CHANNELS
    }

    fun isSocial(): Boolean {
        return (homeType == HomeUIType.MOMENTS) || (homeType == HomeUIType.POST) || (homeType == HomeUIType.SUGGESTED)
    }

    fun isStreaming(): Boolean {
        return !isChannels() && !isSocial()
    }

    fun isValid() = homeType != null


    companion object {
        fun get(json: JSONObject): HomeNavigationData? {
            return JsonHelper.deserialize(json) as? HomeNavigationData
        }
    }

    override fun equals(other: Any?): Boolean {
        return other != null && other is HomeNavigationData && sectionTitle == other.sectionTitle
    }

    override fun hashCode(): Int {
        var result = homeType?.hashCode() ?: 0
        result = 31 * result + sectionTitle.hashCode()
        return result
    }

}


/**
 * Enum enclosing the UI types for the [HomeNavigationData] elements.
 * @author mbaldrighi on 2019-07-01.
 */
enum class HomeUIType(val value: String) {
    @SerializedName(value = "channels")
    CHANNELS("channels"),
    @SerializedName(value = "moments")
    MOMENTS("moments"),
    @SerializedName(value = "post")
    POST("post"),
    @SerializedName(value = "suggested")
    SUGGESTED("suggested"),
    @SerializedName(value = "videos")
    VIDEOS("videos"),
    @SerializedName(value = "series")
    SERIES("series"),
    @SerializedName(value = "musicVideos")
    MUSIC_VIDEOS("musicVideos")
}


interface HomeDataObject


class HomeDiffCallback : BaseDiffUtilCallback<HomeDataObject>() {

    override fun areItemsTheSame(oldItem: HomeDataObject, newItem: HomeDataObject): Boolean {

        return when {
            (oldItem is StreamingCategory) && (newItem is StreamingCategory) -> oldItem.areItemsTheSame(newItem)
            (oldItem is Post && newItem is Post) -> oldItem.areItemsTheSame(newItem)
            (oldItem is SuggestedPerson && newItem is SuggestedPerson) -> oldItem.areItemsTheSame(newItem)
            (oldItem is TTUVideo && newItem is TTUVideo) -> oldItem.areItemsTheSame(newItem)
            else -> false
        }


    }

    override fun areContentsTheSame(oldItem: HomeDataObject, newItem: HomeDataObject): Boolean {
        return when {
            (oldItem is StreamingCategory) && (newItem is StreamingCategory) -> oldItem.areContentsTheSame(newItem)
            (oldItem is Post && newItem is Post) -> oldItem.areContentsTheSameForPreview(newItem)
            (oldItem is SuggestedPerson && newItem is SuggestedPerson) -> oldItem.areContentsTheSame(newItem)
            (oldItem is TTUVideo && newItem is TTUVideo) -> oldItem.areContentsTheSame(newItem)
            else -> false
        }
    }
}