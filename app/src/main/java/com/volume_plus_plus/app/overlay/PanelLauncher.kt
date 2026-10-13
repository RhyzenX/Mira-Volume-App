package com.volume_plus_plus.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

/**
 * Raises the volume panel from somewhere other than the volume keys — today, the Quick Settings
 * tile ([com.volume_plus_plus.app.service.VolumePanelTileService]).
 *
 * ## Why a singleton
 *
 * A `TileService` is bound around the tap and unbound again moments later, so a panel owned by the
 * tile would be torn down underneath itself. The panel has to outlive whatever asked for it, and
 * this object is what holds it.
 *
 * ## Which controller it uses
 *
 * Whenever the accessibility service is connected it lends its own controller here, and the tile
 * drives *that* one. That matters: the panel raised by the tile is then the same object the volume
 * keys raise, so a tap on the tile followed by a key press refreshes one panel instead of racing two
 * of them, and the per-app volumes the tile's panel sets are the ones the service keeps re-applying.
 *
 * Without the accessibility service there is no such controller, so one is built here instead. The
 * panel needs nothing but SYSTEM_ALERT_WINDOW to draw and to work — the keys are the only part that
 * needs accessibility — which is what makes the tile a genuine alternative for someone whose volume
 * buttons have failed rather than merely a shortcut. Its [AppVolumeController] is deliberately left
 * unstarted, as the Overlay tab's preview leaves its own: a per-app level still applies the moment
 * it is set, but with nothing else running there is no background loop to re-apply it later.
 */
// A controller holds a Context, and lint can't tell which one. Neither reference here can outlive
// its context: the controller built below is given the *application* context, and the one the
// accessibility service lends is dropped again in [detach] when that service unbinds.
@SuppressLint("StaticFieldLeak")
object PanelLauncher {

    /** The accessibility service's controller while it is connected; null when it isn't. */
    @Volatile
    private var serviceController: OverlayController? = null

    private var ownController: OverlayController? = null
    private var ownVolume: AppVolumeController? = null

    /**
     * Show the panel. Call on the main thread — it builds views. Returns false when overlay
     * permission is missing, which is the caller's cue to send the user off to grant it.
     */
    fun show(context: Context): Boolean {
        if (!Settings.canDrawOverlays(context)) return false
        controller(context).show()
        return true
    }

    /** Lend the accessibility service's controller for as long as it is connected. */
    fun attach(controller: OverlayController) {
        serviceController = controller
        // The service's controller supersedes anything built here, and two live controllers would
        // mean two panels; drop ours rather than leave it holding a window.
        releaseOwn()
    }

    /** The accessibility service is going away, so its controller is about to be destroyed. */
    fun detach() {
        serviceController = null
    }

    private fun controller(context: Context): OverlayController {
        serviceController?.let { return it }
        ownController?.let { return it }
        val app = context.applicationContext
        val volume = AppVolumeController(app)
        ownVolume = volume
        return OverlayController(app, volume).also { ownController = it }
    }

    private fun releaseOwn() {
        ownController?.destroy()
        ownController = null
        ownVolume?.destroy()
        ownVolume = null
    }
}
