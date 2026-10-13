package com.volume_plus_plus.app.overlay

import android.content.Context
import android.os.Build

/**
 * The device's own Material You colours, resolved into the handful of Material 3 roles the overlay
 * actually paints with. Backs the colour editor's "use system colours" action, which fills a whole
 * panel's palette from the wallpaper-derived scheme in one tap instead of dialling each swatch.
 *
 * Android 12 (API 31) is where the framework started publishing the generated tonal palettes as
 * `@android:color/system_*` resources; there is nothing of the sort below it, which is why
 * [systemPalette] returns null there and the editor hides the action (see [LiveEditSession]) rather
 * than inventing a palette that isn't the system's.
 *
 * The tonal resources themselves are theme-independent — the *role* picks which tone to use — so a
 * light and a dark reading of the same device differ only in the tones chosen here.
 */
data class SystemPalette(
    /** The scheme's main colour: slider fills, the accent, filled buttons. */
    val primary: Int,
    /** Content that sits *on* a [primary]-filled control (the media note, the active mode icon). */
    val onPrimary: Int,
    /** The tonal, lower-emphasis form of [primary] — the output picker's slider. */
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    /** The tonal secondary surface: tune footers, tonal DONE pills, the "connect a device" card. */
    val secondaryContainer: Int,
    val onSecondaryContainer: Int,
    /** The panel/sheet background. */
    val surface: Int,
    /** A surface raised off [surface] — the output card inside a sheet, the picker's own card. */
    val surfaceContainer: Int,
    /** The neutral surface a control sits on: slider tracks. */
    val surfaceVariant: Int,
    /** Body text on [surface]. */
    val onSurface: Int,
    /** Icons and lower-emphasis content on [surface] / [surfaceVariant]. */
    val onSurfaceVariant: Int,
)

/** Whether this device publishes a system palette at all — Android 12 (API 31) introduced it. */
fun systemPaletteAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Read the device's current Material You palette for a [dark] or light reading of the scheme, or
 * null on a device that has none (pre-Android 12).
 *
 * The tone-per-role pairs below are the platform's own Material You mapping (the one
 * `Theme.DeviceDefault.DayNight` uses), so a filled panel lands on the same colours the system's own
 * surfaces do rather than on an approximation of them.
 */
fun systemPalette(context: Context, dark: Boolean): SystemPalette? {
    // Kept in one guarded body deliberately: the android.R.color.system_* ids only resolve from
    // API 31, so the lookups have to stay behind this check rather than move to a helper.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    fun tone(light: Int, night: Int): Int = context.getColor(if (dark) night else light)
    return SystemPalette(
        primary = tone(android.R.color.system_accent1_600, android.R.color.system_accent1_200),
        onPrimary = tone(android.R.color.system_accent1_0, android.R.color.system_accent1_800),
        primaryContainer = tone(android.R.color.system_accent1_100, android.R.color.system_accent1_700),
        onPrimaryContainer = tone(android.R.color.system_accent1_900, android.R.color.system_accent1_100),
        secondaryContainer = tone(android.R.color.system_accent2_100, android.R.color.system_accent2_700),
        onSecondaryContainer = tone(android.R.color.system_accent2_900, android.R.color.system_accent2_100),
        surface = tone(android.R.color.system_neutral1_10, android.R.color.system_neutral1_900),
        surfaceContainer = tone(android.R.color.system_neutral1_100, android.R.color.system_neutral1_800),
        surfaceVariant = tone(android.R.color.system_neutral2_100, android.R.color.system_neutral2_700),
        onSurface = tone(android.R.color.system_neutral1_900, android.R.color.system_neutral1_100),
        onSurfaceVariant = tone(android.R.color.system_neutral2_700, android.R.color.system_neutral2_200),
    )
}

/**
 * Every colour [version] + [component] exposes, filled from this palette — exactly the swatches
 * [visibleColors] offers, so the fill never writes an override for an element that panel doesn't
 * draw.
 */
fun SystemPalette.fillFor(version: OverlayVersion, component: PanelComponent): Map<EditableColor, Int> {
    // The Android 15 sheet's DONE pill is a filled primary button; the 13/14 sheet's is tonal.
    val android15Sheet = component == PanelComponent.EXPANDED && version.iconSet?.usesOutputSheet == true
    return visibleColors(version, component).associateWith { colorFor(it, android15Sheet) }
}

/**
 * One editable element's colour in this palette, by the role it plays on the panel — which is why
 * the two in-fill icons take [onPrimary] rather than a tint of the icon colour, and the swatches
 * that alias the output picker's fields take that modal's own roles.
 */
fun SystemPalette.colorFor(c: EditableColor, android15Sheet: Boolean): Int = when (c) {
    EditableColor.BACKGROUND -> surface
    EditableColor.PROGRESS -> primary
    EditableColor.TRACK -> surfaceVariant
    EditableColor.ICON -> onSurfaceVariant
    EditableColor.ACCENT -> primary
    EditableColor.TEXT -> onSurface
    EditableColor.SECONDARY -> secondaryContainer
    // Both ride *inside* a primary-filled control — the music note on the media slider's fill, the
    // selected mode's icon on its accent disc — so they take the palette's own on-primary tone
    // instead of the renderer's black/white auto-contrast fallback.
    EditableColor.MEDIA_ICON -> onPrimary
    EditableColor.MODE_ICON -> onPrimary
    EditableColor.OVERFLOW -> primary
    // The trailing dot sits at the far end of an Android 15 pill — over the bare track until the
    // level reaches it — so it takes the fill colour, the way that sheet's light art draws it.
    EditableColor.DOT -> primary
    EditableColor.OUTPUT_SURFACE -> surfaceContainer
    EditableColor.DONE_BG -> if (android15Sheet) primary else secondaryContainer
    EditableColor.DONE_TEXT -> if (android15Sheet) onPrimary else onSecondaryContainer
    EditableColor.TITLE -> onSurface
    // The Media output picker: a raised card carrying a tonal slider, a tonal "connect a device"
    // surface and a filled DONE pill. Its icon/text/dot all read over either the tonal slider or
    // that connect surface, so they share the on-container tone that stays legible against both.
    EditableColor.OUTPUT_CARD -> surfaceContainer
    EditableColor.OUTPUT_SLIDER -> primaryContainer
    EditableColor.OUTPUT_SLIDER_TRACK -> surfaceVariant
    EditableColor.OUTPUT_PICKER_ICON -> onPrimaryContainer
    EditableColor.OUTPUT_PICKER_TEXT -> onPrimaryContainer
    EditableColor.OUTPUT_PICKER_DOT -> onPrimaryContainer
    EditableColor.OUTPUT_CONNECT -> secondaryContainer
    EditableColor.OUTPUT_DONE -> primary
    EditableColor.OUTPUT_DONE_TEXT -> onPrimary
}
