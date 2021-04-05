package rs.highlande.app.tatatu.feature.voiceVideoCalls.respository.helper

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.SoundPool
import android.os.Build
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.TTUApp
import rs.highlande.app.tatatu.core.util.CommonTAG
import rs.highlande.app.tatatu.core.util.LogUtils
import rs.highlande.app.tatatu.core.util.vibrateForCalls


class SoundPoolManager private constructor(
    context: Context,
    private val usageType: VoiceVideoUsageType = VoiceVideoUsageType.OUTGOING
) : BaseSoundPoolManager(context, null, null), SoundPool.OnLoadCompleteListener {

    companion object {

        private var instance: SoundPoolManager? = null

        fun getInstance(context: Context, usageType: VoiceVideoUsageType): SoundPoolManager {
            if (instance == null) {
                instance = SoundPoolManager(context, usageType)
            }
            return instance as SoundPoolManager
        }
    }

    private var playingRingtone = false
    private var playingWaiting = false
    private var loadedWaiting = false
    private var loadedError = false
    private var playingCalled = false
    private var actualVolume: Float = 0.toFloat()
    private var maxVolume: Float = 0.toFloat()
    private var volume: Float = 0.toFloat()

    // using MediaPlayer instead of SoundPool until custom file is provided
    private var mediaPlayer: MediaPlayer? = null
    private val soundPool: SoundPool by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) SoundPool.Builder().setMaxStreams(
            1
        ).build()
        else SoundPool(1, AudioManager.STREAM_MUSIC, 0)
    }
    private var waitingToneId: Int = 0
    private var waitingToneStream: Int = 0
    private var errorSoundId: Int = 0

    init {
        // AudioManager audio settings for adjusting the volume
        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        val pair = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> true to true
            AudioManager.RINGER_MODE_SILENT -> false to false
            AudioManager.RINGER_MODE_VIBRATE -> false to true
            else -> true to true
        }
        TTUApp.canPlaySounds = pair.first
        TTUApp.canVibrate = pair.second

        actualVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        if (maxVolume != 0f) volume = actualVolume / maxVolume

        if (usageType == VoiceVideoUsageType.INCOMING) {
            MediaPlayer.create(
                context,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            )?.let {
                mediaPlayer = it
                it.isLooping = true
            }
        }

        soundPool.setOnLoadCompleteListener(this)
        waitingToneId = soundPool.load(context, R.raw.ring_tone, 1)
        errorSoundId = soundPool.load(context, R.raw.call_error, 1)
    }

    override fun onLoadComplete(soundPool: SoundPool, sampleId: Int, status: Int) {
        if (status == 0) {
            loadedWaiting = true

            if (sampleId == waitingToneId && usageType == VoiceVideoUsageType.OUTGOING && playingCalled) {
                playRinging(context)
                playingCalled = false
            }
        }
    }

    fun playRinging(context: Context) {
        LogUtils.e(CommonTAG,"CN->SoundPoolManager FN--> stopRinging() --> playingWaiting  $playingWaiting usageType  $usageType")

        if (usageType == VoiceVideoUsageType.OUTGOING) {
            if (loadedWaiting && !playingWaiting) {
                waitingToneStream = soundPool.play(waitingToneId, volume, volume, 1, -1, 1f)
                playingWaiting = true
            } else playingCalled = true
        } else {
            if (TTUApp.canVibrate) vibrateForCalls(context)
            LogUtils.e(CommonTAG,"CN->SoundPoolManager FN--> stopRinging() --> TTUApp.canVibrate  ${TTUApp.canVibrate} canPlaySounds ${TTUApp.canPlaySounds }")
            if (TTUApp.canPlaySounds && mediaPlayer != null) {
                if (mediaPlayer!!.isPlaying) mediaPlayer!!.pause()
                mediaPlayer!!.setOnPreparedListener { mp ->
                    mediaPlayer = mp
                    mediaPlayer!!.start()
                }
                playingRingtone = true
            }

        }
    }

    fun stopRinging() {
        LogUtils.e(CommonTAG,"CN->SoundPoolManager FN--> stopRinging() --> playingWaiting  $playingWaiting usageType  $usageType")

        if (playingWaiting && usageType == VoiceVideoUsageType.OUTGOING) {
            soundPool.stop(waitingToneStream)
            playingWaiting = false
        } else {
            vibrateForCalls(context, true)
            if (playingRingtone && mediaPlayer != null) {
                mediaPlayer!!.pause()
                playingRingtone = false
            }
        }
    }

    fun playError() {
        if (loadedError) {
            stopRinging()
            soundPool.play(errorSoundId, volume, volume, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.unload(waitingToneId)
        soundPool.unload(errorSoundId)
        soundPool.release()

        if (mediaPlayer != null) {
            if (mediaPlayer!!.isPlaying) mediaPlayer!!.pause()
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
            LogUtils.e("SPM", "release--> $mediaPlayer")
        }
        instance = null
    }
}
