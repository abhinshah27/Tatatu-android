package rs.highlande.app.tatatu.feature.authentication.repository

import org.json.JSONObject
import java.util.*

/**
 * Contains all the models related to Auth process.
 * @author mbaldrighi on 2019-07-24.
 */



data class SignupUserInfo(
    var fullName: String = "",
    var email: String = "",
    var password: String = "",
    var confirmPassword: String = ""
)


data class AuthenticationData(
    var authType: AuthManager.AuthType = AuthManager.AuthType.CUSTOM,
    var is18Years: Boolean = false,
    var termsSigned: Boolean = false,
    var sharePersonalData: Boolean = false,
    var marketingCommunication: Boolean = false,
    var inviterToken: String = "",
    var invitationId: String = ""
) {

    val language: String = Locale.getDefault().language ?: "en"


    val auth0Parameters: () -> MutableMap<String, Any> = {
        mutableMapOf(
            "is18years" to is18Years.toString(),
            "termsCondSigned" to termsSigned.toString(),
            "sharePersonalData" to sharePersonalData.toString(),
            "marketingCommunication" to marketingCommunication.toString(),
            "lang" to language,
            "inviterToken" to inviterToken,
            "invitationId" to invitationId,
            "isMobile" to "true"
        )
    }

    fun hasPendingInvitation() = inviterToken.isNotBlank() || invitationId.isNotBlank()

}


class AccountingData {

    val permissions = AuthenticationData()
    val signupUserInfo = SignupUserInfo()
    var userName = ""

    val getDataForTatatuSignup: (Boolean, String) -> JSONObject = { isLogin, token ->

        val json = JSONObject()

        when (permissions.authType) {

            AuthManager.AuthType.CUSTOM, AuthManager.AuthType.FACEBOOK, AuthManager.AuthType.GOOGLE -> {

                json.apply {
                    put("isLogin", isLogin)
                    put("userName", userName)
                    put("isAgreeTermsPrivacy", permissions.termsSigned.toString())
                    put("termsPrivacyLanguage", permissions.language)
                    put("agreeSharePersonalData", permissions.sharePersonalData.toString())
                    put("inviterToken", permissions.inviterToken)
                    put("invitationId", permissions.invitationId)
                    put("userToken", token)
                    put("authenticationMode", permissions.authType.value)
                }

                json
            }
        }
    }


    val registrationDataForAuth0: () -> Map<String, Any> = {
        val parameters = permissions.auth0Parameters()
        if (permissions.authType == AuthManager.AuthType.CUSTOM) {
            parameters["name"] = signupUserInfo.fullName
            mapOf<String, Any>("user_metadata" to parameters)
        }
        else parameters
    }

}