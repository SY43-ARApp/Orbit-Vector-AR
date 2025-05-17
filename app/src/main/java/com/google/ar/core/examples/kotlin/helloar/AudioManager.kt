package com.google.ar.core.examples.kotlin.helloar

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

object AudioManager {
    private var bgPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()
    private var prefs: SharedPreferences? = null

    private const val PREFS_NAME = "audio_prefs"
    private const val KEY_MUSIC_ENABLED = "music_enabled"
    private const val KEY_SFX_ENABLED = "sfx_enabled"

    private val sfxResources = mapOf(
        "titletap" to R.raw.titletap
    )

    fun init(context: Context) {
        if (soundPool != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        sfxResources.forEach { (key, resId) ->
            val soundId = soundPool!!.load(context, resId, 1)
            soundMap[key] = soundId
        }
    }

    // --- MUSIC PREFERENCE ---
    fun isMusicEnabled(): Boolean {
        return prefs?.getBoolean(KEY_MUSIC_ENABLED, true) ?: true
    }

    fun setMusicEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_MUSIC_ENABLED, enabled)?.apply()
        if (!enabled) stopBackground()
    }

    // --- SFX PREFERENCE ---
    fun isSfxEnabled(): Boolean {
        return prefs?.getBoolean(KEY_SFX_ENABLED, true) ?: true
    }

    fun setSfxEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_SFX_ENABLED, enabled)?.apply()
    }

    // --- PLAYBACK ---
    fun playSfx(key: String, volume: Float = 1.0f) {
        if (!isSfxEnabled()) return
        val id = soundMap[key] ?: return
        soundPool?.play(id, volume, volume, 1, 0, 1f)
    }

    fun playBackground(context: Context, resId: Int, looping: Boolean = true) {
        if (!isMusicEnabled()) return
        stopBackground()
        bgPlayer = MediaPlayer.create(context, resId).apply {
            isLooping = looping
            start()
        }
    }

    fun stopBackground() {
        bgPlayer?.stop()
        bgPlayer?.release()
        bgPlayer = null
    }

    fun releaseAll() {
        stopBackground()
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}