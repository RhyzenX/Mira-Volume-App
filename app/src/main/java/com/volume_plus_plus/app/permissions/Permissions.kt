package com.volume_plus_plus.app.permissions

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.volume_plus_plus.app.service.VolumeKeyService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Every grant the app asks the user for, as one snapshot. */
data class PermissionState(
    /** SYSTEM_ALERT_WINDOW — draw the volume panel over other apps. */
    val overlay: Boolean = false,
    /** Our [VolumeKeyService] is enabled, so the hardware volume keys reach us. */
    val accessibility: Boolean = false,
    /** Notification-policy access — switching the ringer to vibrate/silent from the panel. */
    val dnd: Boolean = false,
) {
    /** Everything the volume-key overlay needs before it can actually run. */
    val ready: Boolean get() = overlay && accessibility && dnd
}

/**
 * The live state of every grant, published as a flow the UI observes.
 *
 * ## Why this exists
 *
 * Each screen used to read the grants for itself, on resume. That is both late — a grant made while
 * the app is still on screen (from Quick Settings, a split-screen Settings app, or the accessibility
 * shortcut) went unnoticed until the app was backgrounded and reopened — and, for the accessibility
 * service, wrong: the check compared `ComponentName.flattenToString()` against the raw
 * ENABLED_ACCESSIBILITY_SERVICES string, which misses the equally-legal short form
 * (`pkg/.service.VolumeKeyService`) that plenty of builds store, so an enabled service kept being
 * reported as "still needs to be enabled".
 *
 * ## How it stays instant
 *
 * Nothing here polls as its primary source. Every grant has a platform notification behind it, and
 * they all land within a frame of the user flipping the switch — while we are still in the
 * background, so returning from Settings finds the state already correct:
 *
 *  - accessibility — the service reports its own connect/disconnect ([onServiceConnected]), a
 *    [ContentObserver] watches the two Secure settings behind it, and [AccessibilityManager] tells
 *    us when the enabled set changes;
 *  - draw-over-other-apps — an app-op, so [AppOpsManager.startWatchingMode] fires on the change
 *    (there is no broadcast for this one);
 *  - Do Not Disturb — the platform broadcasts ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED.
 *
 * [rememberPermissionState] adds a slow foreground poll on top, purely as a fallback for OEM builds
 * that route a grant somewhere the platform never notifies on.
 */
@SuppressLint("StaticFieldLeak") // Application context only — see [init].
object Permissions {

    private val _state = MutableStateFlow(PermissionState())
    val state: StateFlow<PermissionState> = _state.asStateFlow()

    private var appContext: Context? = null
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Whether [VolumeKeyService] is connected right now. The most authoritative answer there is —
     * the service lives in this process, so if it says it is running, it is running — and the one
     * signal that can't be defeated by an OEM storing the enabled list somewhere unusual.
     */
    @Volatile
    private var serviceConnected = false

    /** Held so they aren't collected: a ContentObserver is only weakly reachable once registered. */
    private var settingsObserver: ContentObserver? = null
    private var dndReceiver: BroadcastReceiver? = null
    private var opListener: AppOpsManager.OnOpChangedListener? = null

    /**
     * Start watching, and publish a first reading. Idempotent, so every entry point into the process
     * can call it; [com.volume_plus_plus.app.VolumeApp] does it first, which is what lets the very
     * first frame of any screen draw the real state instead of a default that flips a moment later.
     */
    @Synchronized
    fun init(context: Context) {
        if (appContext == null) {
            val app = context.applicationContext
            appContext = app
            watchAccessibility(app)
            watchOverlay(app)
            watchDnd(app)
        }
        refresh()
    }

    /** Re-read every grant and publish. Three cheap reads; safe to call from any thread. */
    fun refresh() {
        val app = appContext ?: return
        _state.value = PermissionState(
            overlay = Settings.canDrawOverlays(app),
            accessibility = isAccessibilityEnabled(app),
            dnd = hasDndAccess(app),
        )
    }

    /** [VolumeKeyService] reporting in, so the checklist ticks the moment the service comes up —
     *  before the user has even left the Settings screen they enabled it on. */
    fun onServiceConnected() {
        serviceConnected = true
        refresh()
    }

    /** …and reporting out again when it is disabled or the process is torn down. */
    fun onServiceDisconnected() {
        serviceConnected = false
        refresh()
    }

    // ── Sources ────────────────────────────────────────────────────────────────────────────────

    /**
     * True if our accessibility service is enabled, asked three ways so no single platform quirk can
     * produce a false "not enabled": the service's own report, the runtime list the platform binds
     * from, and finally the stored setting.
     */
    private fun isAccessibilityEnabled(app: Context): Boolean {
        if (serviceConnected) return true

        val expected = ComponentName(app, VolumeKeyService::class.java)
        val manager = app.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val running = runCatching {
            manager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        }.getOrNull().orEmpty()
            .any { ComponentName.unflattenFromString(it.id.orEmpty()) == expected }
        if (running) return true

        return storedServices(app).any { it == expected }
    }

    /**
     * The components listed in ENABLED_ACCESSIBILITY_SERVICES, parsed rather than string-matched:
     * the platform may store either the full form (`pkg/pkg.service.Foo`) or the short one
     * (`pkg/.service.Foo`), and [ComponentName.unflattenFromString] normalises both. Comparing
     * flattened strings — what this used to do — recognised only the full form.
     *
     * Read only while accessibility is globally on, since a stale list can outlive it being switched
     * off. A build that leaves that flag at 0 while services really do run can't reach here anyway:
     * its own service would have reported in above.
     */
    private fun storedServices(app: Context): List<ComponentName> {
        val resolver = app.contentResolver
        val enabled = runCatching {
            Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        }.getOrDefault(0)
        if (enabled == 0) return emptyList()
        val raw = runCatching {
            Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.getOrNull() ?: return emptyList()
        // ':' is the platform's separator; ';' turns up on a handful of OEM builds.
        return raw.split(':', ';').mapNotNull { ComponentName.unflattenFromString(it.trim()) }
    }

    private fun hasDndAccess(app: Context): Boolean {
        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return runCatching { manager.isNotificationPolicyAccessGranted }.getOrDefault(false)
    }

    // ── Watchers ───────────────────────────────────────────────────────────────────────────────

    private fun watchAccessibility(app: Context) {
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) = refresh()
        }
        settingsObserver = observer
        val resolver = app.contentResolver
        runCatching {
            resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
                false,
                observer,
            )
            resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_ENABLED),
                false,
                observer,
            )
        }
        val manager = app.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return
        runCatching {
            manager.addAccessibilityStateChangeListener { refresh() }
            // Android 13+ narrows that to the change we actually care about — the enabled set
            // itself, which also moves while accessibility as a whole stays on.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                manager.addAccessibilityServicesStateChangeListener { refresh() }
            }
        }
    }

    private fun watchOverlay(app: Context) {
        // Draw-over-other-apps is an app-op and has no broadcast; watching the op is the only way to
        // hear about it without polling.
        val appOps = app.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return
        val listener = AppOpsManager.OnOpChangedListener { _, _ -> refresh() }
        opListener = listener
        runCatching {
            appOps.startWatchingMode(
                AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                app.packageName,
                listener,
            )
        }
    }

    private fun watchDnd(app: Context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = refresh()
        }
        dndReceiver = receiver
        // Registered as exported, like the volume-changed receiver on the Volume tab: this is a
        // protected broadcast, so only the system can ever send it.
        runCatching {
            ContextCompat.registerReceiver(
                app,
                receiver,
                IntentFilter(NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
    }
}

/**
 * How often a screen re-measures the grants while it is in front of the user. Only a fallback —
 * [Permissions]' listeners are what make a grant appear instantly — so it is slow enough to cost
 * nothing and still quick enough that a build with no working notification feels immediate.
 */
private const val POLL_MS = 500L

/**
 * The live [PermissionState], for as long as this composable is in the tree: event-driven from
 * [Permissions], plus a slow re-measure while the screen is resumed as the belt-and-braces fallback.
 */
@Composable
fun rememberPermissionState(): PermissionState {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    // Idempotent, and done during composition rather than from an effect so the first frame already
    // carries the real state.
    remember(context) { Permissions.init(context) }
    val state by Permissions.state.collectAsStateWithLifecycle()
    LaunchedEffect(owner) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                Permissions.refresh()
                delay(POLL_MS)
            }
        }
    }
    return state
}
