package rs.highlande.app.tatatu.model

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import org.json.JSONObject
import rs.highlande.app.tatatu.core.ui.recyclerView.BaseDiffUtilCallback
import rs.highlande.app.tatatu.core.util.JsonHelper
import rs.highlande.app.tatatu.feature.commonView.UnFollowable
import java.lang.reflect.Type

/**
 * Class holding the information of a suggested person to follow.
 * @author mbaldrighi on 2019-06-28.
 */
class SuggestedPerson : MainUserInfo(), HomeDataObject, UnFollowable {

    var mediaURL: String? = null
    var type: MediaType = MediaType.IMAGE

    override fun equals(other: Any?): Boolean {
        return if (other is SuggestedPerson)
            return uid == other.uid
        else super.equals(other)
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    companion object {

        fun get(json: JSONObject): SuggestedPerson? {
            return JsonHelper.deserialize(json)
        }

    }

    fun isVideo() = type == MediaType.VIDEO


    override fun areItemsTheSame(other: MainUserInfo): Boolean {
        return uid == other.uid
    }

    override fun areContentsTheSame(other: MainUserInfo): Boolean {
        return picture == other.picture &&
                accountType == other.accountType &&
                name == other.name &&
                username == other.username
    }

    /**
     * Child class of [BaseDiffUtilCallback] implementing DiffUtil for a [SuggestedPerson].
     * @author mbaldrighi on 2019-07-16.
     */
    class SuggestedDiffCallback : BaseDiffUtilCallback<SuggestedPerson>() {

        override fun areItemsTheSame(oldItem: SuggestedPerson, newItem: SuggestedPerson): Boolean {
            return oldItem.areItemsTheSame(newItem)
        }

        override fun areContentsTheSame(oldItem: SuggestedPerson, newItem: SuggestedPerson): Boolean {
            return oldItem.areContentsTheSame(newItem)
        }
    }


    /**
     * Class implementing [JsonDeserializer] of type [User] that deserializes the [MainUserInfo] interface.
     */
    class SuggestedDeserializer : JsonDeserializer<SuggestedPerson> {

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): SuggestedPerson {
            if (json != null) {

                return Gson().fromJson(json, SuggestedPerson::class.java).apply {
                    json.asJsonObject.apply {
                        if (has("MainUserInfo")) {
                            val mainInfo = get("MainUserInfo").asJsonObject
                            uid = mainInfo.get("uid").asString
                            username = mainInfo.get("username").asString
                            name = mainInfo. get("name").asString
                            picture = mainInfo.get("picture").asString
                            verified = mainInfo.get("verified").asBoolean
                            isPublic = mainInfo.get("isPublic").asBoolean
                            accountType = AccountType.toEnum(mainInfo.get("accountType").asString)
                            TTUId = mainInfo.get("TTUId").asString
                        }
                        if (has("FeedData")) {
                            val feedData = get("FeedData").asJsonObject
                            mediaURL = feedData.get("mediaURL").asString
                            type = MediaType.toEnum(feedData.get("type").asString)
                        }
                    }
                }

            }

            return SuggestedPerson()
        }

    }



}