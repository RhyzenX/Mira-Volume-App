package com.volume_plus_plus.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.volume_plus_plus.app.overlay.PanelLauncher

/**
 * Closes the Quick Settings shade, then raises the volume panel.
 *
 * The panel is a `TYPE_APPLICATION_OVERLAY` window, which the platform draws *below* the shade — put
 * on screen while Quick Settings is still open it would simply be hidden behind it. Starting an
 * activity is what collapses the shade, so the tile starts this one, exactly as the eyedropper's
 * notification action starts [PickTrampolineActivity]. It draws nothing (translucent theme, no
 * content view) and finishes before it can be seen.
 */
class PanelTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!PanelLauncher.show(this)) {
            // No draw-over-other-apps means there is no panel to raise, and a tile that silently did
            // nothing would look broken — send them to the one screen that can fix it.
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(intent) }
        }
        finish()
        // No slide-in/out: this activity is machinery, and animating it would be a visible flicker.
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
