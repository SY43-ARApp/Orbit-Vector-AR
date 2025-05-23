package com.sy43.orbitvectorar.kotlin.game

import com.sy43.orbitvectorar.R
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

object AudioManager {
    private var bgPlayer: MediaPlayer? = null
    private var nextBgPlayer: MediaPlayer? = null 
    private var currentLoopingResId: Int? = null
    private var isContinuousLoopActive: Boolean = false

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()
    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    private const val PREFS_NAME = "audio_prefs"
    private const val KEY_MUSIC_ENABLED = "music_enabled"
    private const val KEY_SFX_ENABLED = "sfx_enabled"

    private val sfxResources = mapOf(
        "titletap" to R.raw.titletap,
        "tap" to R.raw.tap,
        "victory" to R.raw.victory,
        "gameover" to R.raw.gameover,
        "arrowhit" to R.raw.arrowhit,
        "arrow" to R.raw.arrow,
    )

    fun init(context: Context) {
        if (soundPool != null) return
        appContext = context.applicationContext
        prefs = appContext!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
            val soundId = soundPool!!.load(appContext, resId, 1)
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

    private val backgroundMusicCompletionListener = object : MediaPlayer.OnCompletionListener {
        override fun onCompletion(completedPlayer: MediaPlayer) {
            completedPlayer.release()
            bgPlayer = nextBgPlayer
            nextBgPlayer = null

            if (isContinuousLoopActive && currentLoopingResId != null && isMusicEnabled() && appContext != null) {
                try {
                    val newNext = MediaPlayer.create(appContext!!, currentLoopingResId!!)

                    bgPlayer?.setNextMediaPlayer(newNext)
                    bgPlayer?.setOnCompletionListener(this)

                    nextBgPlayer = newNext
                } catch (e: Exception) {
                    stopBackgroundInternal(clearLoopState = true)
                }
            } else {
                bgPlayer?.setNextMediaPlayer(null)
                bgPlayer?.setOnCompletionListener { mp ->
                    mp.release()
                    if (bgPlayer == mp) {
                        bgPlayer = null
                    }
                    if (currentLoopingResId != null) { 
                        isContinuousLoopActive = false
                        currentLoopingResId = null
                    }
                }
            }
        }
    }


    fun playBackground(resId: Int, looping: Boolean = true) {
        if (!isMusicEnabled()) return

        stopBackgroundInternal(clearLoopState = true)

        val context = appContext ?: return

        currentLoopingResId = resId
        isContinuousLoopActive = looping

        try {
            bgPlayer = MediaPlayer.create(context, resId)
            if (looping) {
                nextBgPlayer = MediaPlayer.create(context, resId)
                bgPlayer?.setNextMediaPlayer(nextBgPlayer)
                bgPlayer?.setOnCompletionListener(backgroundMusicCompletionListener)
            } else {
                bgPlayer?.isLooping = false 
                bgPlayer?.setOnCompletionListener { mp ->
                    mp.release()
                    if (bgPlayer == mp) {
                        bgPlayer = null
                    }
                    if (currentLoopingResId == resId) {
                         currentLoopingResId = null
                    }
                }
            }
            bgPlayer?.start()
        } catch (e: Exception) {
            stopBackgroundInternal(clearLoopState = true) 
        }
    }

    private fun stopBackgroundInternal(clearLoopState: Boolean) {
        if (clearLoopState) {
            isContinuousLoopActive = false
            currentLoopingResId = null
        }

        bgPlayer?.apply {
            try {
                if (isPlaying) stop()
                setOnCompletionListener(null)
                setNextMediaPlayer(null)
                release()
            } catch (e: IllegalStateException) {  }
        }
        bgPlayer = null

        nextBgPlayer?.apply {
            try {
                release() 
            } catch (e: IllegalStateException) {  }
        }
        nextBgPlayer = null
    }

    fun stopBackground() {
        stopBackgroundInternal(clearLoopState = true)
    }

    fun releaseAll() {
        stopBackground()
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}