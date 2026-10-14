package com.mira.volume.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Mira's fixed brand palette. Success remains green to distinguish completed
// actions from the violet brand accent.
private val SuccessOnLight = Color(0xFF236B3A)
private val SuccessOnDark = Color(0xFF9FE0AD)

/** Green for a completed checklist step, with contrast selected for the active theme. */
val successColor: Color
    @Composable get() =
        if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
            SuccessOnDark
        } else {
            SuccessOnLight
        }

// Mira light palette: soft lavender accents, readable dark text, pale surfaces.
val PrimaryLight = Color(0xFF6240A5)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEBDDFF)
val OnPrimaryContainerLight = Color(0xFF26005A)
val SecondaryLight = Color(0xFF665A72)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFF0DDF7)
val OnSecondaryContainerLight = Color(0xFF21172B)
val TertiaryLight = Color(0xFF8A416F)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFD8EB)
val OnTertiaryContainerLight = Color(0xFF390729)
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)
val BackgroundLight = Color(0xFFFFF7FF)
val OnBackgroundLight = Color(0xFF201A24)
val SurfaceLight = Color(0xFFFFF7FF)
val OnSurfaceLight = Color(0xFF201A24)
val SurfaceVariantLight = Color(0xFFE9DFEF)
val OnSurfaceVariantLight = Color(0xFF4B4452)
val OutlineLight = Color(0xFF7D7484)
val OutlineVariantLight = Color(0xFFCEC3D5)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF9EEFF)
val SurfaceContainerLight = Color(0xFFF3E7FA)
val SurfaceContainerHighLight = Color(0xFFEDE0F5)
val SurfaceContainerHighestLight = Color(0xFFE7DAEF)
val InversePrimaryLight = Color(0xFFD0BCFF)
val InverseSurfaceLight = Color(0xFF352E3A)
val InverseOnSurfaceLight = Color(0xFFFAEDFF)

// Mira dark palette: charcoal-plum surfaces with lavender and rose accents.
val PrimaryDark = Color(0xFFCFB7FF)
val OnPrimaryDark = Color(0xFF351064)
val PrimaryContainerDark = Color(0xFF4D2B85)
val OnPrimaryContainerDark = Color(0xFFEBDDFF)
val SecondaryDark = Color(0xFFD3C2DE)
val OnSecondaryDark = Color(0xFF382C42)
val SecondaryContainerDark = Color(0xFF4F4259)
val OnSecondaryContainerDark = Color(0xFFF0DDF7)
val TertiaryDark = Color(0xFFFFB0D8)
val OnTertiaryDark = Color(0xFF571D42)
val TertiaryContainerDark = Color(0xFF713258)
val OnTertiaryContainerDark = Color(0xFFFFD8EB)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)
val BackgroundDark = Color(0xFF17131F)
val OnBackgroundDark = Color(0xFFECE2F3)
val SurfaceDark = Color(0xFF17131F)
val OnSurfaceDark = Color(0xFFECE2F3)
val SurfaceVariantDark = Color(0xFF494150)
val OnSurfaceVariantDark = Color(0xFFD0C3D9)
val OutlineDark = Color(0xFF988BA3)
val OutlineVariantDark = Color(0xFF494150)
val SurfaceContainerLowestDark = Color(0xFF110D18)
val SurfaceContainerLowDark = Color(0xFF201A29)
val SurfaceContainerDark = Color(0xFF261C33)
val SurfaceContainerHighDark = Color(0xFF30243E)
val SurfaceContainerHighestDark = Color(0xFF3B2E49)
val InversePrimaryDark = Color(0xFF6240A5)
val InverseSurfaceDark = Color(0xFFECE2F3)
val InverseOnSurfaceDark = Color(0xFF352E3A)
