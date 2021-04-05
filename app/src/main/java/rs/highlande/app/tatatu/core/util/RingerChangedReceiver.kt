/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.core.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import rs.highlande.app.tatatu.TTUApp

/**
 * Child class of [BroadcastReceiver] needed to listen to changes between the device ringer modes:
 * [AudioManager.RINGER_MODE_NORMAL], [AudioManager.RINGER_MODE_SILENT], [AudioManager.RINGER_MODE_VIBRATE].
 *
 * @author mbaldrighi on 2019-07-10.
 */
class RingerChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        LogUtils.e("RingerChangedReceiver", "RingerChangedReceiver onReceive ")
        if (isInitialStickyBroadcast) {
            val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            parseRingerMode(audioManager?.ringerMode ?: -1)

            return
        }

        if (intent != null) {
            parseRingerMode(intent.extras?.getInt(AudioManager.EXTRA_RINGER_MODE) ?: -1)
        }
    }

    private fun parseRingerMode(mode: Int) {
        LogUtils.e("RingerChangedReceiver", "parseRingerMode $mode")
        val pair = when (mode) {
            AudioManager.RINGER_MODE_NORMAL -> true to true
            AudioManager.RINGER_MODE_SILENT -> false to false
            AudioManager.RINGER_MODE_VIBRATE -> false to true
            else -> true to true
        }

        TTUApp.canPlaySounds = pair.first
        TTUApp.canVibrate = pair.second

        LogUtils.e("RingerChangedReceiver", "parseRingerMode canPlaySounds --> ${TTUApp.canPlaySounds}")
        LogUtils.e("RingerChangedReceiver", "parseRingerMode canVibrate --> ${TTUApp.canVibrate}")
    }

}