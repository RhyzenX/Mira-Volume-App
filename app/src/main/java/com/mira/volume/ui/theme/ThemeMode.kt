package com.mira.volume.ui.theme

/**
 * How the app UI picks between the light and dark colour schemes. The set offered in the picker is
 * [com.mira.volume.config.AppConfig.SUPPORTED_THEMES]; display names come from
 * [com.mira.volume.i18n.label].
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    /** Follow the device's light/dark setting. */
    SYSTEM,
}
