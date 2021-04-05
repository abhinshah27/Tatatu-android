package rs.highlande.app.tatatu.connection

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.util.*

/**
 * TODO - File description
 * @author mbaldrighi on 2019-07-11.
 */


/**
 * Retrieves device's [Settings.Secure.ANDROID_ID]
 *
 * @param context the activity/application's [Context] to access [android.content.ContentResolver].
 */
@SuppressLint("HardwareIds")
fun getSecureID(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}

/**
 * Generates a random [UUID].
 */
fun getNewIdOperation(): String {
    return UUID.randomUUID().toString()
}