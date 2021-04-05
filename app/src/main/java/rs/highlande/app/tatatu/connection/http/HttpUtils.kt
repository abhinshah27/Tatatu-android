package rs.highlande.app.tatatu.connection.http

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rs.highlande.app.tatatu.connection.http.HttpClientManager.defaultHttpClient
import rs.highlande.app.tatatu.connection.webSocket.SocketResponse
import rs.highlande.app.tatatu.core.util.JsonHelper
import rs.highlande.app.tatatu.core.util.LogUtils
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit


/**
 * File holding constants, classes and methods related to TTU Android app HTTP communication.
 * @author mbaldrighi on 2019-07-12.
 */


/**
 * It retrieves an instance of a [Retrofit] of type [T], calling a specific URL ([BASE_URL]) pointing to the Brightcove
 * servers for TTU.
 */
inline fun <reified T : Any>getRetrofitService(): T {
    val retrofit = Retrofit.Builder()
        .client(defaultHttpClient)
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()

   return retrofit.create(T::class.java)
}


/**
 * It retrieves an instance of a [Retrofit] of type [T], calling a specific URL ([BASE_URL_TTU]) pointing to the HL
 * servers for TTU.
 */
inline fun <reified T : Any>getRetrofitServiceTTU(): T {
    val retrofit = Retrofit.Builder()
        .client(defaultHttpClient)
        .baseUrl(BASE_URL_TTU)
        .addConverterFactory(getCustomConverterFactory())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()

    return retrofit.create(T::class.java)
}

object HttpClientManager {

    val defaultHttpClient: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
        .build()

}

/**
 * Generic class implementing a basic logic of response from server.
 */
open class HTTPResponse {

    enum class Status(val value: String) {
        @SerializedName(value = "") NONE(""),
        @SerializedName(value = "OK") OK("OK"),
        @SerializedName(value = "KO") KO("KO")
    }

    /**
     * Typo handled with SerializedName annotation.
     */
    @SerializedName("reponseCode") var responseCode = -1
    var responseStatus = Status.NONE
    var responseError = ""
    var data: JSONObject = JSONObject()


    fun isValid() = responseStatus != Status.NONE && responseCode > -1

    fun isError() = responseStatus == Status.KO

//    fun getException() : TTUException? = if (isError()) TTUException(responseCode, null) else null

}


/**
 * Class implementing [JsonDeserializer] of type [HTTPResponse] that allows to leave [HTTPResponse.data] as [JSONObject].
 */
class HTTPResponseSerializer : JsonDeserializer<HTTPResponse> {

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): HTTPResponse {
        if (json != null) {

            LogUtils.d("Retrofit HTTPResponse", JsonHelper.serializeToString(json))

            return JsonHelper.deserialize<HTTPResponse>(json).apply {
                data = JSONObject(json.asJsonObject.get("data").toString())
            }
        }

        return HTTPResponse()
    }
}

/**
 * Class implementing [JsonDeserializer] of type [HTTPResponse] that allows to leave [HTTPResponse.data] as [JSONObject].
 */
class HTTPBridgeResponseSerializer : JsonDeserializer<SocketResponse> {

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): SocketResponse {
        if (json != null) {

            val string = JsonHelper.serializeToString(json)

            LogUtils.d("Retrofit HTTPResponse", string)

            return SocketResponse(string)
        }

        return SocketResponse(null)
    }
}


/**
 * Method to retrieve a custom [GsonConverterFactory] to parse an [HTTPResponse] as per requirements, using
 * [HTTPResponseSerializer] and [HTTPBridgeResponseSerializer].
 * @return The wanted instance of [GsonConverterFactory].
 */
fun getCustomConverterFactory(): GsonConverterFactory {

    return GsonConverterFactory.create(
        GsonBuilder()
            .registerTypeAdapter(HTTPResponse::class.java, HTTPResponseSerializer())
            .registerTypeAdapter(SocketResponse::class.java, HTTPBridgeResponseSerializer())
            .create()
    )

}