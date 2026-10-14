package com.mira.volume

import android.app.Application
import android.content.res.Configuration
import com.mira.volume.config.AppSettings
import com.mira.volume.permissions.Permissions

/**
 * Exists to give [AppSettings] one guaranteed initialisation point.
 *
 * The theme and the language are read from three independent entry points — [MainActivity], the
 * [com.mira.volume.service.VolumeKeyService] accessibility service and the eyedropper's
 * foreground service — and any of them can be the first thing the system starts in this process. An
 * `Application` is the only place that reliably runs before all of them.
 */
class VolumeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        // Before anything can ask: the grant watchers have to be live from the moment the process
        // exists, so a switch flipped while we're in the background is already accounted for by the
        // time a screen is drawn. Idempotent, and it registers listeners rather than polling.
        Permissions.init(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // The user can change the device language while we're running. The activity is recreated for
        // that, but the accessibility service isn't, and it owns the overlay — so re-detect here,
        // where every component sees it. A manual language override is unaffected.
        AppSettings.refreshSystemLanguage()
    }
}
