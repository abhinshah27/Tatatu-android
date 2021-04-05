package rs.highlande.app.tatatu.feature.authentication.api

import android.os.Bundle
import io.reactivex.Observable
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import rs.highlande.app.tatatu.connection.OnServerMessageReceivedListener
import rs.highlande.app.tatatu.connection.http.*
import rs.highlande.app.tatatu.connection.webSocket.*
import rs.highlande.app.tatatu.core.api.BaseApi
import rs.highlande.app.tatatu.core.util.TTUAuthException
import rs.highlande.app.tatatu.feature.authentication.repository.AuthManager
import rs.highlande.app.tatatu.model.User

/**
 * Describes all [Retrofit] interfaces and methods to perform authentication-related comms with server.
 * @author mbaldrighi on 2019-07-25.
 */
class AuthApi : BaseApi() {


    interface UserAccessService {

        @GET(TOKEN_CHECK_USERNAME)
        fun checkUsername(
            @Path(PARAM_USERNAME) username: String
        ): Observable<HTTPResponse>


        @POST(TOKEN_USER_GATEWAY_CUSTOM)
        fun doLoginOrSignupCustom(@Body json: RequestBody): Observable<HTTPResponse>
        @POST(TOKEN_USER_GATEWAY_SOCIAL)
        fun doLoginOrSignupSocial(@Body json: RequestBody): Observable<HTTPResponse>
        @POST(TOKEN_USER_GATEWAY_SOCIAL_CONFIRM)
        fun doConfirmSocial(@Body json: RequestBody): Observable<HTTPResponse>


        @POST(TOKEN_BRIDGE_DO_ACTION)
        fun sendPasswordRecoveryEmail(@Body json: RequestBody): Observable<SocketResponse>

    }


    fun checkUsername(username: String): Observable<Pair<Boolean, TTUAuthException?>> {
        return getRetrofitServiceTTU<UserAccessService>().checkUsername(username).map {
            if (it.isError()) false to TTUAuthException(it.responseCode, null)
            else true to null
        }
    }


    fun doLoginOrSignup(jsonObject: JSONObject, authType: AuthManager.AuthType): Observable<Pair<User?, TTUAuthException?>> {

        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString())

        val userService = getRetrofitServiceTTU<UserAccessService>()
        return (
                if (authType == AuthManager.AuthType.CUSTOM)
                    userService.doLoginOrSignupCustom(body)
                else
                    userService.doLoginOrSignupSocial(body)
                )
            .map {
                if (it.isError()) null to TTUAuthException(it.responseCode, null)
                else {
                    if (it.data.length() == 0)
                        null to TTUAuthException(it.responseCode, null)
                    else
                        User.get(it.data) to null
                }
            }

    }


    fun doConfirmSocial(jsonObject: JSONObject): Observable<Pair<User?, TTUAuthException?>> {

        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString())

        return getRetrofitServiceTTU<UserAccessService>().doConfirmSocial(body).map {
            if (it.isError()) null to TTUAuthException(it.responseCode, null)
            else {
                if (it.data.length() == 0)
                    null to TTUAuthException(it.responseCode, null)
                else
                    User.get(it.data) to null
            }
        }

    }


    fun sendPasswordRecoveryEmail(email: String): Observable<Pair<Boolean, TTUAuthException?>> {

        val req = SocketRequest(Bundle().apply {
            putString("email", email)
        },
            callCode = SERVER_OP_FORGOT_PASSWORD,
            logTag = "FORGOT PWD call",
            caller = null
        )

        val body = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            req.body
        )

        return getRetrofitServiceTTU<UserAccessService>().sendPasswordRecoveryEmail(body).map {
            if (it.isError) false to TTUAuthException(it.error.first, null)
            else {
                if (it.jsonResponse?.length() == 0 || it.jsonResponse!![0] == null || it.jsonResponse!!.optJSONObject(0)?.length() == 0)
                    false to TTUAuthException(it.error.first, null)
                else
                    true to null
            }
        }

    }

    fun logout(token: String, caller: OnServerMessageReceivedListener): Observable<RequestStatus> {

        return tracker.callServer(SocketRequest(Bundle().apply {
            putString("deviceToken", token)
        },
            callCode = SERVER_OP_LOGOUT,
            logTag = "LOGOUT call",
            caller = caller
        ))

    }

}