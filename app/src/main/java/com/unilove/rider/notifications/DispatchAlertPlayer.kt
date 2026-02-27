package com.unilove.rider.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import com.unilove.rider.model.RingtoneOption

object DispatchAlertPlayer {
  private const val LOOP_INTERVAL_MS = 2500L

  private val mainHandler = Handler(Looper.getMainLooper())
  private var ringtone: Ringtone? = null
  private var ringtoneUri: String? = null
  private var toneGenerator: ToneGenerator? = null
  private var isLooping = false
  private var selectedRingtone: RingtoneOption = RingtoneOption.PREMIUM_CHIME
  private var selectedNotificationToneUri: String? = null
  private var appContext: Context? = null
  private var autoStopRunnable: Runnable? = null

  private val loopRunnable = object : Runnable {
    override fun run() {
      if (!isLooping) return
      playCycle()
      mainHandler.postDelayed(this, LOOP_INTERVAL_MS)
    }
  }

  @Synchronized
  fun playOneShot(
    context: Context,
    ringtoneOption: RingtoneOption = selectedRingtone,
    notificationToneUri: String? = selectedNotificationToneUri,
  ) {
    appContext = context.applicationContext
    selectedRingtone = ringtoneOption
    selectedNotificationToneUri = notificationToneUri
    playCycle()
    mainHandler.postDelayed({ ringtone?.stop() }, 1900L)
  }

  @Synchronized
  fun startLoop(
    context: Context,
    ringtoneOption: RingtoneOption = selectedRingtone,
    notificationToneUri: String? = selectedNotificationToneUri,
    maxDurationMs: Long = 45000L,
  ) {
    appContext = context.applicationContext
    selectedRingtone = ringtoneOption
    selectedNotificationToneUri = notificationToneUri
    if (isLooping) return
    isLooping = true
    mainHandler.post(loopRunnable)

    autoStopRunnable?.let(mainHandler::removeCallbacks)
    autoStopRunnable = Runnable { stop() }.also { stopper ->
      mainHandler.postDelayed(stopper, maxDurationMs.coerceAtLeast(5000L))
    }
  }

  @Synchronized
  fun stop() {
    isLooping = false
    mainHandler.removeCallbacks(loopRunnable)
    autoStopRunnable?.let(mainHandler::removeCallbacks)
    autoStopRunnable = null
    ringtone?.stop()
  }

  @Synchronized
  private fun ensureGeneratorLocked(streamType: Int = AudioManager.STREAM_NOTIFICATION) {
    if (toneGenerator == null) {
      toneGenerator = ToneGenerator(streamType, 100)
    }
  }

  private fun playCycle() {
    val context = appContext ?: return
    if (playNativeTone(context, selectedNotificationToneUri)) {
      return
    }
    ensureGeneratorLocked()
    playPattern(selectedRingtone)
  }

  private fun playNativeTone(context: Context, toneUri: String?): Boolean {
    val targetUri = DispatchToneDefaults.resolvePreferredOrDefault(context, toneUri)
      ?: DispatchToneDefaults.systemFallbackUri()
      ?: return false

    val cached = ringtone
    if (cached == null || ringtoneUri != targetUri.toString()) {
      ringtone = runCatching { RingtoneManager.getRingtone(context, targetUri) }.getOrNull()
      ringtoneUri = targetUri.toString()
    }

    val current = ringtone ?: return false
    runCatching {
      current.audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
      current.stop()
      current.play()
    }.onFailure { return false }

    return true
  }

  @Synchronized
  private fun playPattern(option: RingtoneOption) {
    val generator = toneGenerator ?: return
    when (option) {
      RingtoneOption.PREMIUM_CHIME -> {
        generator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 260)
        mainHandler.postDelayed({ generator.startTone(ToneGenerator.TONE_PROP_BEEP2, 180) }, 200)
        mainHandler.postDelayed({ generator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 260) }, 420)
      }

      RingtoneOption.EXECUTIVE_BELL -> {
        generator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 280)
        mainHandler.postDelayed({ generator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 280) }, 320)
      }

      RingtoneOption.CRISP_ALERT -> {
        generator.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
        mainHandler.postDelayed({ generator.startTone(ToneGenerator.TONE_PROP_BEEP2, 200) }, 180)
        mainHandler.postDelayed({ generator.startTone(ToneGenerator.TONE_PROP_ACK, 220) }, 360)
      }
    }
  }
}
