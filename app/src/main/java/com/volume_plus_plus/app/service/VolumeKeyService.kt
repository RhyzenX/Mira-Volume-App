package com.volume_plus_plus.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.Vibrator
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.volume_plus_plus.app.overlay.AppVolumeController
import com.volume_plus_plus.app.overlay.MediaBooster
import com.volume_plus_plus.app.overlay.OverlayController
import com.volume_plus_plus.app.overlay.PanelLauncher
import com.volume_plus_plus.app.overlay.StepHaptics
import com.volume_plus_plus.app.data.OverlayPrefs
import com.volume_plus_plus.app.permissions.Permissions
import com.volume_plus_plus.app.privileged.PrivilegedManager

/**
 * Captures the hardware volume keys and, instead of the stock system panel, raises our own
 * [OverlayController] panel with per-app sliders. Requires the user to enable this accessibility
 * service; key filtering is declared in res/xml/volume_key_service.xml.
 *
 * If overlay permission (SYSTEM_ALERT_WINDOW) isn't granted we don't consume the keys, so the
 * normal system volume UI keeps working until the user finishes setup.
 */
class VolumeKeyService : AccessibilityService() {

    private var overlay: OverlayController? = null
    /** Remembers and re-applies per-app volumes across playback sessions, independent of the panel. */
    private var appVolume: AppVolumeController? = null
    private val prefs by lazy { OverlayPrefs(this) }
    private val vibrator: Vibrator by lazy { StepHaptics.vibrator(this) }

    private val handler = Handler(Looper.getMainLooper())
    /** Direction of the currently-held volume key (+1 up, -1 down, 0 = none). */
    private var heldDirection = 0
    /** Current gap between repeats; shrinks as the key is held so the ramp speeds up. */
    private var repeatDelay = REPEAT_START_MS
    /**
     * `uptimeMillis` of the most recent key-down of the current hold — the first press and every
     * system auto-repeat that follows it. [repeat] measures staleness against this so a hold whose
     * release we never hear about can still end itself. See [releaseHold].
     */
    private var lastKeyDownAt = 0L
    /**
     * Whether this hold has seen a system auto-repeat yet. The staleness check is only trustworthy
     * once we know this device actually emits them, so it stays disarmed until the first one
     * arrives — on a device that sends none, a legitimate hold must not be cut short.
     */
    private var sawAutoRepeat = false
    /** `uptimeMillis` of the current hold's first press, for the absolute ceiling in [repeat] and
     *  for deciding whether a power press arrived soon enough to mean "that was a chord". */
    private var holdStartedAt = 0L

    /** Power-key state, maintained only on builds that deliver it to us at all — see [onKeyEvent]. */
    private var powerHeld = false
    private var powerDownAt = 0L
    private var powerUpAt = 0L

    private val powerManager by lazy { getSystemService(POWER_SERVICE) as PowerManager }

    /**
     * Whether a volume key arriving now is the second half of a power chord rather than a volume
     * adjustment. Always false on a build that never delivers power keys, which is most of them.
     */
    private fun powerChordActive(): Boolean {
        val now = SystemClock.uptimeMillis()
        // A power down whose release we never heard about would otherwise latch the volume keys off
        // for good. A chord is formed within 150 ms and has fired well before this; past it the user
        // is in the power menu, where the volume keys aren't ours to act on anyway.
        if (powerHeld && now - powerDownAt > MAX_POWER_HOLD_MS) powerHeld = false
        return powerHeld || (powerUpAt != 0L && now - powerUpAt < CHORD_TAIL_MS)
    }

    /** Keeps nudging the volume while a key stays down, accelerating like the stock panel. */
    private val repeat = object : Runnable {
        override fun run() {
            if (heldDirection == 0) return
            // The volume keys stop reaching an accessibility service the moment the display sleeps —
            // the platform handles them itself while non-interactive — so a key released after the
            // screen goes off never produces the ACTION_UP that would end this hold, which is the
            // commonest way a release goes missing. Reading the display state here rather than
            // waiting on an ACTION_SCREEN_OFF broadcast ends the hold within a single step instead
            // of a broadcast's latency, which on a slow device is many steps of over-travel.
            if (!powerManager.isInteractive) {
                releaseHold()
                return
            }
            // Nothing has confirmed the key is still down for far longer than the auto-repeat
            // interval, so the release was lost somewhere between the platform and us. Stop rather
            // than ramp on forever. The threshold is deliberately many times the ~50 ms auto-repeat
            // period: cutting a real hold short would be a worse, more visible fault than a fraction
            // of a second of over-travel in a case that should not arise at all.
            if (sawAutoRepeat && SystemClock.uptimeMillis() - lastKeyDownAt > STALE_HOLD_MS) {
                releaseHold()
                return
            }
            // Absolute ceiling, ungated. The check above can only ever fire on a device that emits
            // key auto-repeats to an accessibility service, and plenty don't — measured on Android 9,
            // where a one-second hold produced exactly one down and one up. On those devices a lost
            // release (a key chord swallowing the rest of the gesture, say) would ramp the volume to
            // an end and pin the panel there for ever, with nothing able to notice. Ten seconds is
            // ~250 steps at the fastest cadence, far past any stream's range, so a genuine hold is
            // only ever cut long after the volume stopped moving.
            if (SystemClock.uptimeMillis() - holdStartedAt > MAX_HOLD_MS) {
                releaseHold()
                return
            }
            // Deliberately kept running once the volume is pinned at an end of its range: the step
            // does nothing, but the panel stays on screen for as long as the key is held, which is
            // what the stock panels these skins reproduce do.
            // Flagged as a repeat so the booster can refuse it: a held key must never climb past
            // 100% into boost, only deliberate presses may. See [MediaBooster.handleKey].
            if (overlay?.adjustAndShow(heldDirection, fromAutoRepeat = true) == true &&
                prefs.isHoldStepHapticsEnabled()
            ) {
                tick()
            }
            repeatDelay = (repeatDelay - REPEAT_STEP_MS).coerceAtLeast(REPEAT_MIN_MS)
            handler.postDelayed(this, repeatDelay)
        }
    }

    /**
     * End the current hold and drop any pending repeat.
     *
     * Every path that stops driving the volume goes through here — the key's release, an early
     * return that means we are no longer in charge of the keys, the screen going off, a reconnect —
     * because the hold is a latch. Whatever fails to clear it leaves the volume ramping to maximum
     * with nothing held, which is what made a single tap occasionally behave like a stuck key.
     */
    private fun releaseHold() {
        heldDirection = 0
        sawAutoRepeat = false
        handler.removeCallbacks(repeat)
    }

    /** Forget the power key too. Only for a connect/disconnect — a plain [releaseHold] must not do
     *  this, since the power key can legitimately still be down after the volume key has gone. */
    private fun forgetPowerKey() {
        powerHeld = false
        powerDownAt = 0L
        powerUpAt = 0L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // The setup checklist's most authoritative answer about this service: we are connected, so
        // it is enabled, whatever the platform happens to have written into Secure settings. Told
        // first, so the tick appears while the user is still standing on the Settings screen.
        Permissions.onServiceConnected()
        // Ensure the privileged link is alive even if the main UI was never opened this process.
        PrivilegedManager.init(this)
        // A reconnect must not inherit the previous connection's boost, for the same reason it must
        // not inherit its hold.
        MediaBooster.init(this)
        MediaBooster.reset()
        // A repeat connect must not inherit the previous connection's hold: the old panel is about to
        // be torn down, but heldDirection and any queued repeat would outlive it and go on driving
        // the volume through the newly built one.
        releaseHold()
        forgetPowerKey()
        // Guard against a repeat connect leaking the previous instances (a live playback callback and
        // poll loop): tear them down before rebuilding.
        overlay?.destroy()
        appVolume?.destroy()
        val volume = AppVolumeController(this).also { it.start() }
        appVolume = volume
        // Lent to the Quick Settings tile so a tile tap and a key press raise the same panel, backed
        // by the same per-app volume state, rather than two independent ones.
        overlay = OverlayController(this, volume).also { PanelLauncher.attach(it) }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Power + a volume key is a system gesture (screenshot, and OEM chords besides), never a
        // volume adjustment. Track the power key so the volume half can be ignored.
        //
        // Whether this ever runs is up to the platform. Stock Android clears ACTION_PASS_TO_USER for
        // KEYCODE_POWER in interceptKeyBeforeQueueing, and accessibility key filtering sits behind
        // that gate, so power events simply never arrive — measured on Android 9, where injecting a
        // power press changed the display state without producing a single onKeyEvent call. On such
        // a build [powerHeld] stays false for ever and none of this does anything, which is exactly
        // right: those builds also suppress the volume key themselves, so there is nothing to fix.
        // It exists for the OEM builds that *do* pass both keys through — the ones where this bug
        // was reported (issue 17).
        if (event.keyCode == KeyEvent.KEYCODE_POWER) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) {
                    powerHeld = true
                    powerDownAt = SystemClock.uptimeMillis()
                    // The volume key came first and we already stepped it: that press was the front
                    // half of a chord, not a volume adjustment, so put it back.
                    if (heldDirection != 0 &&
                        SystemClock.uptimeMillis() - holdStartedAt <= CHORD_WINDOW_MS
                    ) {
                        releaseHold()
                        overlay?.undoHold()
                    }
                }

                KeyEvent.ACTION_UP -> {
                    powerHeld = false
                    powerUpAt = SystemClock.uptimeMillis()
                }
            }
            // Never consumed. Swallowing power would break screen-off, the power menu and the very
            // screenshot chord this exists to protect — we are observing it, not intercepting it.
            return super.onKeyEvent(event)
        }

        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!isVolumeKey) return super.onKeyEvent(event)

        // The user asked for Android's built-in volume control, so don't consume the keys — the
        // system panel handles them exactly as it would without this app. Read live (the setting can
        // be flipped while the service stays connected); a held repeat is dropped below with the key.
        if (prefs.isSystemVolumePanelEnabled()) {
            releaseHold()
            return super.onKeyEvent(event)
        }

        // Without draw-over-other-apps we can't show our panel — leave the system UI in charge. Any
        // hold in flight ends here too: this returns before the ACTION_UP bookkeeping below, so a
        // permission that goes away mid-press would otherwise latch the hold on for good.
        if (!Settings.canDrawOverlays(this)) {
            releaseHold()
            return super.onKeyEvent(event)
        }

        // The power key is down, so this is the volume half of a system chord. Consume it and do
        // nothing — no step, no panel. Consuming rather than passing through matches stock, which
        // suppresses the volume action for a chord itself. Both directions, because Power + Volume
        // Up is an OEM chord on several devices.
        if (powerChordActive()) {
            releaseHold()
            return true
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                // Every down — the first press and the auto-repeats after it — is fresh proof that
                // the key is still physically held, which is what [repeat]'s staleness check reads.
                lastKeyDownAt = SystemClock.uptimeMillis()
                // Drive the repeat ourselves off the first press; ignore the system's auto-repeat
                // downs (repeatCount > 0) so the two don't both fire and double the step.
                if (event.repeatCount == 0) {
                    heldDirection = if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) 1 else -1
                    repeatDelay = REPEAT_START_MS
                    sawAutoRepeat = false
                    holdStartedAt = SystemClock.uptimeMillis()
                    overlay?.markHoldStart()
                    overlay?.adjustAndShow(heldDirection)
                    handler.removeCallbacks(repeat)
                    handler.postDelayed(repeat, FIRST_REPEAT_MS)
                } else {
                    sawAutoRepeat = true
                }
            }

            KeyEvent.ACTION_UP -> releaseHold()
        }
        // Consume both down and up so the system panel never appears.
        return true
    }

    /**
     * Track which app is in the foreground so [OverlayController] can surface a currently-active app
     * that's playing audio at the top of its per-app sliders. Window-state-changed events carry the
     * package without needing to inspect window content, and this works uniformly on Android 7–15.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString()
        if (!pkg.isNullOrEmpty() && pkg != packageName) {
            overlay?.foregroundPackage = pkg
        }
    }

    override fun onInterrupt() { /* not used */ }

    /** A build that tears the service down without unbinding it first still has to stop claiming
     *  the service is connected. */
    override fun onDestroy() {
        Permissions.onServiceDisconnected()
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // Symmetrical with onServiceConnected: the checklist must untick the moment the service is
        // switched off, not on the next time something happens to re-read the setting.
        Permissions.onServiceDisconnected()
        releaseHold()
        forgetPowerKey()
        // Safety-critical: the gain lives in the audio server, not in this process, so a boost left
        // applied would outlast the service being disabled with no UI anywhere left to undo it.
        MediaBooster.shutdown()
        // Stop lending this controller before it's destroyed, so a tile tap builds a fresh one
        // instead of showing a panel through a torn-down object.
        PanelLauncher.detach()
        overlay?.destroy()
        overlay = null
        appVolume?.destroy()
        appVolume = null
        return super.onUnbind(intent)
    }

    /** One step tick. Shared with the Overlay tab's intensity slider, so what the user feels while
     *  setting the level up is exactly what a held key gives them. */
    private fun tick() = StepHaptics.play(vibrator, prefs.getHoldStepHapticIntensity())

    private companion object {
        /** Delay before a held key starts repeating (ms). Short so a hold responds almost at once. */
        const val FIRST_REPEAT_MS = 250L
        /** Repeat pacing: starts at [REPEAT_START_MS] and shrinks by [REPEAT_STEP_MS] each step down
         *  to [REPEAT_MIN_MS], so a long hold accelerates. Small steps keep the ramp smooth. */
        const val REPEAT_START_MS = 150L
        const val REPEAT_STEP_MS = 12L
        const val REPEAT_MIN_MS = 40L
        /** How long a hold may go without any fresh key-down before we treat the release as lost.
         *  Android auto-repeats roughly every 50 ms, so this leaves an order of magnitude of slack. */
        const val STALE_HOLD_MS = 500L
        /** Ceiling on a single hold, independent of whether this device emits auto-repeats. */
        const val MAX_HOLD_MS = 10_000L
        /** How long after our first volume step a power press still means "that was a chord". This
         *  is the platform's own number — PhoneWindowManager only forms a screenshot chord when the
         *  two downs land within SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS of each other — so we never
         *  undo a press the platform would not itself have treated as one. */
        const val CHORD_WINDOW_MS = 150L
        /** How long after the power key comes up we keep ignoring volume keys, covering the skew
         *  between releasing the two halves. Short, because "tap power to wake, then immediately
         *  turn the volume down" is a real gesture that must not be swallowed. */
        const val CHORD_TAIL_MS = 200L
        /** Ceiling on a power hold we never saw released, so a lost up can't disable the volume
         *  keys permanently. */
        const val MAX_POWER_HOLD_MS = 2000L
    }
}
