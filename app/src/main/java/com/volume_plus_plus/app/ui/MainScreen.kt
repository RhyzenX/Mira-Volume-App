package com.volume_plus_plus.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.volume_plus_plus.app.R
import com.volume_plus_plus.app.config.AppConfig
import com.volume_plus_plus.app.data.MixPrefs
import com.volume_plus_plus.app.i18n.Language
import com.volume_plus_plus.app.i18n.Strings
import com.volume_plus_plus.app.i18n.label
import com.volume_plus_plus.app.i18n.strings
import com.volume_plus_plus.app.ui.theme.ThemeMode

private enum class Tab(@DrawableRes val icon: Int) {
    HOME(R.drawable.ic_nav_volume),
    VOLUME(R.drawable.ic_nav_volume),
    MIXING(R.drawable.ic_nav_mixing),
    OVERLAY(R.drawable.ic_nav_overlay),
}

private fun Tab.label(s: Strings): String = when (this) {
    Tab.HOME -> "Home"
    Tab.VOLUME -> s.tabVolume
    Tab.MIXING -> s.tabMixing
    Tab.OVERLAY -> s.tabOverlay
}

/**
 * App root: a bottom navigation bar switching between the [VolumePanelScreen] (default landing,
 * works with no privileges) and the Shizuku-backed [MixAudioScreen]. Owns the single shared
 * scaffold + snackbar host that both tabs draw into, plus the top bar's language and theme pickers.
 *
 * [language] is the user's manual override, or null while the app follows the device's own language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    prefs: MixPrefs,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    language: Language?,
    onLanguageChange: (Language?) -> Unit,
) {
    val s = strings()
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var themePickerExpanded by remember { mutableStateOf(false) }
    var languagePickerExpanded by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    // One-shot request from the Mixing tab: send the user to Overlay and spotlight the switch that
    // is blocking mixing. Deliberately not saved across process death — it only makes sense as the
    // immediate follow-up to that button press.
    var highlightSystemVolumeSwitch by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (tab != Tab.HOME) {
                TopAppBar(
                    title = { Text(AppConfig.APP_NAME) },
                    actions = {
                        LanguageToggle(
                            current = language,
                            onSelect = onLanguageChange,
                            expanded = languagePickerExpanded,
                            onExpandedChange = { languagePickerExpanded = it },
                        )
                        ThemeToggle(
                            current = themeMode,
                            onSelect = onThemeModeChange,
                            expanded = themePickerExpanded,
                            onExpandedChange = { themePickerExpanded = it },
                        )
                    },
                )
            }
        },
        bottomBar = {
            if (tab != Tab.HOME) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Tab.entries.forEach { entry ->
                        NavigationBarItem(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            icon = {
                                Icon(
                                    painter = painterResource(entry.icon),
                                    contentDescription = entry.label(s),
                                )
                            },
                            label = { Text(entry.label(s)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.HOME -> MiraHomeScreen(
                contentPadding = padding,
                onVolumeClick = { tab = Tab.VOLUME },
                onMixClick = { tab = Tab.MIXING },
                onOverlayClick = { tab = Tab.OVERLAY },
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                language = language,
                onLanguageChange = onLanguageChange,
                onBoostClick = { tab = Tab.OVERLAY },
                onDndClick = { tab = Tab.OVERLAY },
            )
            Tab.VOLUME -> VolumePanelScreen(contentPadding = padding, snackbar = snackbar)
            Tab.MIXING -> MixAudioScreen(
                contentPadding = padding,
                snackbar = snackbar,
                prefs = prefs,
                onOpenOverlaySettings = {
                    highlightSystemVolumeSwitch = true
                    tab = Tab.OVERLAY
                },
            )
            Tab.OVERLAY -> OverlayScreen(
                contentPadding = padding,
                snackbar = snackbar,
                highlightSystemVolumeSwitch = highlightSystemVolumeSwitch,
                onHighlightShown = { highlightSystemVolumeSwitch = false },
            )
        }
    }
}

/** Top-bar action: an icon button opening a Light / Dark / System default picker, with a check on
 *  the active mode. Applying a choice re-themes the whole app immediately. */
@Composable
private fun ThemeToggle(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val s = strings()
    IconButton(onClick = { onExpandedChange(true) }) {
        Icon(
            painter = painterResource(R.drawable.ic_theme),
            contentDescription = s.theme,
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
        AppConfig.SUPPORTED_THEMES.forEach { mode ->
            DropdownMenuItem(
                text = { Text(mode.label(s)) },
                onClick = {
                    onSelect(mode)
                    onExpandedChange(false)
                },
                trailingIcon = {
                    if (mode == current) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = s.selected,
                        )
                    }
                },
            )
        }
    }
}

/**
 * Top-bar action: the language picker. Leads with "System default" — the automatic behaviour, and
 * where every install starts — then every language in [AppConfig.SUPPORTED_LANGUAGES] under its own
 * endonym, so an entry is readable to the person looking for it whatever the app is showing now.
 * Applying a choice re-renders the whole app immediately, overlay included.
 */
@Composable
private fun LanguageToggle(
    current: Language?,
    onSelect: (Language?) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val s = strings()
    IconButton(onClick = { onExpandedChange(true) }) {
        Icon(
            painter = painterResource(R.drawable.ic_language),
            contentDescription = s.language,
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
        // null = follow the device, listed first as the default.
        val options: List<Language?> = listOf(null) + AppConfig.SUPPORTED_LANGUAGES
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option?.label() ?: s.languageSystem) },
                onClick = {
                    onSelect(option)
                    onExpandedChange(false)
                },
                trailingIcon = {
                    if (option == current) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = s.selected,
                        )
                    }
                },
            )
        }
    }
}
