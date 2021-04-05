package rs.highlande.app.tatatu.core.util

/**
 * TODO - File description
 * @author mbaldrighi on 2019-07-05.
 */

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement

import org.json.JSONObject
import rs.highlande.app.tatatu.model.SuggestedPerson
import rs.highlande.app.tatatu.model.User
import java.io.Serializable


/**
 * Wrapper utility class providing generic methods to serialize and deserialize project classes using [Gson].
 *
 * @author mbaldrighi on 2019-06-05.
 */
object JsonHelper {

    fun getDefaultBuilder() = GsonBuilder()
        .registerTypeAdapter(User::class.java, User.UserDeserializer())!!
        .registerTypeAdapter(SuggestedPerson::class.java, SuggestedPerson.SuggestedDeserializer())!!


    fun <T: Any>serializeWithExpose(`object`: T): JsonElement {
        val gson = getDefaultBuilder().excludeFieldsWithoutExposeAnnotation().create()
        return gson.toJsonTree(`object`)
    }

    fun <T: Any>serializeToStringWithExpose(`object`: T): String {
        val gson = getDefaultBuilder().excludeFieldsWithoutExposeAnnotation().create()
        return gson.toJson(`object`)
    }

    fun <T: Any>serialize(`object`: T): JsonElement {
        return getDefaultBuilder().create().toJsonTree(`object`)
    }

    fun <T: Any>serializeToString(`object`: T): String {
        return getDefaultBuilder().create().toJson(`object`)
    }

    inline fun <reified T: Any>deserialize(json: JSONObject): T? {
        return deserialize(json.toString())
    }

    inline fun <reified T: Any>deserialize(json: JsonElement): T {
        return getDefaultBuilder().create().fromJson(json, T::class.java)
    }

    inline fun <reified T: Any>deserialize(jsonString: String): T? {
        return if (!jsonString.isBlank()) getDefaultBuilder().create().fromJson(jsonString, T::class.java) else null
    }

    interface JsonDeSerializer {

        val selfObject: Any

        fun serializeWithExpose(): JsonElement
        fun serializeToStringWithExpose(): String

        fun serialize(): JsonElement
        fun serializeToString(): String

        fun deserialize(json: JSONObject): JsonDeSerializer?

        fun deserialize(json: JsonElement): JsonDeSerializer?

        fun deserialize(jsonString: String): JsonDeSerializer?
    }


}


/**
 * Utility interface necessary to serialize entire objects to be supported by the [rs.highlande.app.tatatu.connection.webSocket.SocketRequest] Bundle vararg
 */
interface JsonToSerialize : Serializable {
    fun serializeToJsonObject(): JSONObject
}