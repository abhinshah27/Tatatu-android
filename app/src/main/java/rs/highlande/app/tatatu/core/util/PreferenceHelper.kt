package rs.highlande.app.tatatu.core.util

import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import rs.highlande.app.tatatu.core.util.PreferenceHelper.Companion.PREF_FILE
import rs.highlande.app.tatatu.core.util.PreferenceHelper.Companion.PREF_FILE_TMP
import rs.highlande.app.tatatu.model.User

/**
 * Class holding all logic for SharedPreferences.
 * Two different files are present:
 * 1. [PREF_FILE] survives logout
 * 2. [PREF_FILE_TMP] get cleared upon logout action.
 */
class PreferenceHelper(context: Context) {

    /**
     * [SharedPreferences] instance linked surviving the logout process.
     */
    private val sharedPreferences: SharedPreferences

    /**
     * [SharedPreferences] instance linked NOT surviving the logout process.
     */
    private val tmpSharedPreferences: SharedPreferences

    init {
        sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        tmpSharedPreferences = context.getSharedPreferences(PREF_FILE_TMP, Context.MODE_PRIVATE)
    }


    /**
     * Currently clears [SharedPreferences] during "logout"/"delete account" process.
     */
    fun clear() {
        tmpSharedPreferences.edit().clear().apply()
    }


    fun getTheme() = sharedPreferences.getInt(PROPERTY_THEME, UiModeManager.MODE_NIGHT_NO)
    fun setTheme(theme: Int) {
        sharedPreferences.edit().putInt(PROPERTY_THEME, theme).apply()
    }

    fun getUserId() = tmpSharedPreferences.getString(PROPERTY_USER_ID, "") ?: ""
    private fun setUserId(userId: String) = tmpSharedPreferences.edit().putString(PROPERTY_USER_ID, userId).apply()
    fun getUserTTUId() = tmpSharedPreferences.getString(PROPERTY_USER_TTU_ID, "") ?: ""
    fun setUserTTUId(userTTUId: String) = tmpSharedPreferences.edit().putString(PROPERTY_USER_TTU_ID, userTTUId).apply()

    fun setOnBoard(save: Boolean) = sharedPreferences.edit().putBoolean(PROPERTY_ON_BOARDING, save).apply()
    fun getOnBoard() = sharedPreferences.getBoolean(PROPERTY_ON_BOARDING, false)

    fun setCurrency(mCurrency: String) = sharedPreferences.edit().putString(PROPERTY_OF_WALLET_CURRENCY, mCurrency).apply()
    fun getCurrency() = sharedPreferences.getString(PROPERTY_OF_WALLET_CURRENCY, DEFAULT_CURRENCY)

    fun storeUser(user: User) {

        LogUtils.d(logTag, "TESTSTOREUSER: storing user with uid: ${user.uid}")

        setUserId(user.uid)
        setUserTTUId((user.TTUId))

        val editor = tmpSharedPreferences.edit()
        editor.putString(PROPERTY_USER, JsonHelper.serializeToString(user)).apply()
        LogUtils.d(logTag, "USER Stored -> ${tmpSharedPreferences.getString(PROPERTY_USER, "")}")
    }

    fun removeUser() {
        tmpSharedPreferences.edit().clear().apply()
    }

    fun getUser(): User? {
        val user = tmpSharedPreferences.getString(PROPERTY_USER, "")
        return if (!user.isNullOrBlank()) User.get(JSONObject(user)) else null
    }

    fun rememberUser(remember: Boolean) {
        sharedPreferences.edit().putBoolean(PROPERTY_USER_REMEMBER, remember).apply()
    }
    fun isRememberUser(): Boolean = sharedPreferences.getBoolean(PROPERTY_USER_REMEMBER, false)

    fun storeAuthString(auth: String) {
        sharedPreferences.edit().putString(PROPERTY_USER_REMEMBER_AUTH, auth).apply()
    }
    fun getAuthString(): String = sharedPreferences.getString(PROPERTY_USER_REMEMBER_AUTH, "") ?: ""

    fun getRefreshToken(): String = tmpSharedPreferences.getString(PROPERTY_REFRESH_TOKEN, "") ?: ""
    fun storeRefreshToken(refreshToken: String) = tmpSharedPreferences.edit().putString(PROPERTY_REFRESH_TOKEN, refreshToken).apply()


    fun storeDeviceToken(deviceToken: String) {
        if (deviceToken.isNotBlank()) {
            val editor = tmpSharedPreferences.edit()
            editor.putString(PROPERTY_DEVICE_TOKEN, deviceToken).apply()
        }
    }

    fun getDeviceToken() = tmpSharedPreferences.getString(PROPERTY_DEVICE_TOKEN, "")

    fun hasDeviceTokenChanged(deviceToken: String): Boolean {
        if (deviceToken.isNotBlank()) {
            val storedDeviceToken = tmpSharedPreferences.getString(PROPERTY_DEVICE_TOKEN, "")
            return storedDeviceToken.isNullOrBlank() || storedDeviceToken != deviceToken
        }
        return false
    }

    fun getCallsToken() = tmpSharedPreferences.getString(PROPERTY_CALLS_TOKEN, "")
    fun storeCallsToken(callsToken: String) {
        if (callsToken.isNotBlank()) {
            val editor = tmpSharedPreferences.edit()
            editor.putString(PROPERTY_CALLS_TOKEN, callsToken).apply()
        }
    }



    companion object {

        val logTag = PreferenceHelper::class.java.simpleName

        const val PREF_FILE = "tatatu_prefs"
        const val PREF_FILE_TMP = "tatatu_prefs_tmp"

        const val PROPERTY_THEME = "PROPERTY_THEME"
        const val PROPERTY_USER_ID = "PROPERTY_USER_ID"
        const val PROPERTY_USER_TTU_ID = "PROPERTY_USER_TTU_ID"
        const val PROPERTY_USER = "PROPERTY_USER"
        const val PROPERTY_USER_REMEMBER = "PROPERTY_USER_REMEMBER"
        const val PROPERTY_USER_REMEMBER_AUTH = "PROPERTY_USER_REMEMBER_AUTH"

        const val PROPERTY_ON_BOARDING = "PROPERTY_ON_BOARDING"

        const val PROPERTY_OF_WALLET_CURRENCY= "PROPERTY_OF_WALLET_CURRENCY"
        const val DEFAULT_CURRENCY= "USD"

        const val PROPERTY_REFRESH_TOKEN = "PROPERTY_REFRESH_TOKEN"

        const val PROPERTY_DEVICE_TOKEN = "PROPERTY_DEVICE_TOKEN"

        const val PROPERTY_CALLS_TOKEN = "PROPERTY_CALLS_TOKEN"
    }

}