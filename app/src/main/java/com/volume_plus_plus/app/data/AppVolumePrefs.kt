package com.volume_plus_plus.app.data

import android.content.Context

/**
 * Remembers the volume the user chose for each app, so a level survives the app closing, the
 * privileged service restarting and the device rebooting — not just the playback session that
 * [com.volume_plus_plus.app.overlay.AppVolumeController] tracks in memory.
 *
 * One float per package, keyed by package name, in the manner of
 * [OverlayCustomizationPrefs] (which keys a version's tweaks by the version's enum name). A plain
 * float needs no serialisation, so there is no JSON here.
 *
 * **Full volume stores nothing.** Setting an app back to 100% deletes its entry rather than writing
 * `1.0`, which is what makes "turn it back up to forget it" work with no extra screen, keeps an
 * untouched device's store genuinely empty, and matches the controller's own rule that only a
 * below-full level is ever enforced on a player.
 */
class AppVolumePrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("app_volumes", Context.MODE_PRIVATE)

    /** The remembered level for [packageName], or full if it has none. */
    fun get(packageName: String): Float =
        prefs.getFloat(packageName, DEFAULT_VOLUME).coerceIn(0f, DEFAULT_VOLUME)

    fun set(packageName: String, level: Float) {
        val clamped = level.coerceIn(0f, DEFAULT_VOLUME)
        prefs.edit().apply {
            if (clamped >= DEFAULT_VOLUME) remove(packageName) else putFloat(packageName, clamped)
        }.apply()
    }

    private companion object {
        const val DEFAULT_VOLUME = 1f
    }
}
