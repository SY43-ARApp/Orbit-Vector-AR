package com.google.ar.core.examples.kotlin.helloar.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var uuid: String
        get() = prefs.getString(KEY_UUID, null) ?: createAndSaveUUID()
        set(value) = prefs.edit().putString(KEY_UUID, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var planetUsed : String?
        get() = prefs.getString(KEY_PLANET_USED, null)
        set(value) = prefs.edit().putString(KEY_PLANET_USED, value).apply()

    var arrowUsed : String ?
        get() = prefs.getString(KEY_ARROW_USED, null)
        set(value) = prefs.edit().putString(KEY_ARROW_USED, value).apply()

    var moonUsed : String ?
        get() = prefs.getString(KEY_MOON_USED, null)
        set(value) = prefs.edit().putString(KEY_MOON_USED, value).apply()

    private fun createAndSaveUUID(): String {
        val newUUID = UUID.randomUUID().toString()
        uuid = newUUID
        return newUUID
    }

    companion object {
        private const val PREFS_NAME = "UserPrefs"
        private const val KEY_UUID = "uuid"
        private const val KEY_USERNAME = "username"
        private const val KEY_PLANET_USED = "planetUsed"
        private const val KEY_ARROW_USED = "arrowUsed"
        private const val KEY_MOON_USED = "moonUsed"
    }
}