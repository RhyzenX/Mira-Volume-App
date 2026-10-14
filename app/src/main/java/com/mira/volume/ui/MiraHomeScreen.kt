package com.mira.volume.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mira.volume.R
import com.mira.volume.config.AppConfig
import com.mira.volume.i18n.Language
import com.mira.volume.i18n.label
import com.mira.volume.i18n.strings
import com.mira.volume.ui.theme.ThemeMode

// Home colors follow the active Mira Light/Dark Material theme.
private val MiraCanvas: Color
    @Composable get() = MaterialTheme.colorScheme.background
private val MiraInk: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground
private val MiraMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val MiraViolet: Color
    @Composable get() = MaterialTheme.colorScheme.primary
private val MiraPink: Color
    @Composable get() = MaterialTheme.colorScheme.tertiary
private val MiraBlue: Color
    @Composable get() = MaterialTheme.colorScheme.secondary
private val MiraLavenderTint: Color
    @Composable get() = MaterialTheme.colorScheme.primaryContainer
private val MiraPinkTint: Color
    @Composable get() = MaterialTheme.colorScheme.tertiaryContainer
private val MiraBlueTint: Color
    @Composable get() = MaterialTheme.colorScheme.secondaryContainer

@Composable
fun MiraHomeScreen(
    contentPadding: PaddingValues,
    onVolumeClick: () -> Unit,
    onMixClick: () -> Unit,
    onOverlayClick: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    language: Language?,
    onLanguageChange: (Language?) -> Unit,
    onBoostClick: () -> Unit,
    onDndClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiraCanvas)
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "M I R A",
                color = MiraViolet,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text("🦋", fontSize = 23.sp)
        }

        Spacer(Modifier.height(12.dp))

        // Focused violet-and-pink halo overlaid behind the hero without
        // adding a separate fixed-height layer to the scrolling content.
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier.matchParentSize(),
            ) {
                val violetAlpha = if (isDark) 0.58f else 0.28f
                val pinkAlpha = if (isDark) 0.34f else 0.18f
                val left = androidx.compose.ui.geometry.Offset(size.width * 0.28f, size.height * 0.52f)
                val right = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.48f)

                // Broad feathered light underneath a tighter violet core.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9B5CFF).copy(alpha = violetAlpha),
                            Color(0xFF9B5CFF).copy(alpha = violetAlpha * 0.32f),
                            Color.Transparent,
                        ),
                        center = left,
                        radius = size.width * 0.34f,
                    ),
                    radius = size.width * 0.34f,
                    center = left,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF72D2).copy(alpha = pinkAlpha),
                            Color(0xFFFF72D2).copy(alpha = pinkAlpha * 0.28f),
                            Color.Transparent,
                        ),
                        center = right,
                        radius = size.width * 0.25f,
                    ),
                    radius = size.width * 0.25f,
                    center = right,
                )
            }

            // Existing hero banner.
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF21103F),
                            Color(0xFF482078),
                            Color(0xFF34206D),
                        )
                    )
                )
                .padding(start = 18.dp, top = 20.dp, end = 10.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1.15f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "YOUR SOUND, YOUR RULES",
                    color = Color(0xFFE7D9FF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Mira",
                    color = Color(0xFFFFF8FF),
                    fontSize = 36.sp,
                    lineHeight = 39.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "Make every sound yours.",
                    color = Color(0xFFE0D6F7),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = Color(0xFF4A2A77),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color(0xFFBE91FF)),
                ) {
                    Text(
                        "✦ AUDIO CONTROL ✦",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        color = Color(0xFFF1E6FF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            Spacer(Modifier.width(5.dp))

            Image(
                painter = painterResource(R.drawable.mira_icon),
                contentDescription = "Mira butterfly artwork",
                modifier = Modifier
                    .weight(0.85f)
                    .aspectRatio(0.92f)
                    .clip(RoundedCornerShape(18.dp)),
            )
        }
        }

        Spacer(Modifier.height(26.dp))

        SectionHeading("YOUR CONTROLS")

        Spacer(Modifier.height(12.dp))

        FeatureCard(
            title = "Volume Control",
            subtitle = "Fine-tune every sound stream",
            symbol = "♫",
            accent = MiraViolet,
            tint = MiraLavenderTint,
            onClick = onVolumeClick,
        )
        Spacer(Modifier.height(10.dp))
        FeatureCard(
            title = "Mix Audio",
            subtitle = "Control sound for individual apps",
            symbol = "☷",
            accent = MiraPink,
            tint = MiraPinkTint,
            onClick = onMixClick,
        )
        Spacer(Modifier.height(10.dp))
        FeatureCard(
            title = "Floating Overlay",
            subtitle = "Your controls, always within reach",
            symbol = "◇",
            accent = MiraBlue,
            tint = MiraBlueTint,
            onClick = onOverlayClick,
        )

        Spacer(Modifier.height(25.dp))

        SectionHeading("QUICK ACCESS")

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickAction(
                modifier = Modifier.weight(1f),
                symbol = "✦",
                title = "Boost",
                subtitle = "Audio",
                accent = MiraViolet,
                tint = MiraLavenderTint,
                onClick = onBoostClick,
            )
            QuickAction(
                modifier = Modifier.weight(1f),
                symbol = "☾",
                title = "DND",
                subtitle = "Sound mode",
                accent = MiraPink,
                tint = MiraPinkTint,
                onClick = onDndClick,
            )
            HomeThemeAction(
                modifier = Modifier.weight(1f),
                current = themeMode,
                onSelect = onThemeModeChange,
            )
            HomeLanguageAction(
                modifier = Modifier.weight(1f),
                current = language,
                onSelect = onLanguageChange,
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Every little detail, tuned just for you. 💜",
            modifier = Modifier.fillMaxWidth(),
            color = MiraMuted,
            fontSize = 11.sp,
            letterSpacing = 0.2.sp,
            textAlign = TextAlign.Center,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "✧   🦋   ✧",
            modifier = Modifier.fillMaxWidth(),
            color = MiraViolet,
            fontSize = 19.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionHeading(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = MiraInk,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFFD5C5F5)),
        )
        Spacer(Modifier.width(8.dp))
        Text("✦", color = MiraViolet, fontSize = 17.sp)
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    symbol: String,
    accent: Color,
    tint: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = tint),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.58f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    symbol,
                    color = accent,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = MiraInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    color = MiraMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
            Text(
                "›",
                color = accent,
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
            )
        }
    }
}


@Composable
private fun HomeThemeAction(
    modifier: Modifier,
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val s = strings()

    Box(modifier = modifier) {
        QuickAction(
            modifier = Modifier.fillMaxWidth(),
            symbol = "◐",
            title = "Theme",
            subtitle = "Appearance",
            accent = MiraBlue,
            tint = MiraBlueTint,
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AppConfig.SUPPORTED_THEMES.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label(s)) },
                    onClick = {
                        onSelect(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeLanguageAction(
    modifier: Modifier,
    current: Language?,
    onSelect: (Language?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val s = strings()
    val options: List<Language?> = listOf(null) + AppConfig.SUPPORTED_LANGUAGES

    Box(modifier = modifier) {
        QuickAction(
            modifier = Modifier.fillMaxWidth(),
            symbol = "◎",
            title = "Language",
            subtitle = "Display",
            accent = MiraViolet,
            tint = MiraLavenderTint,
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option?.label() ?: s.languageSystem) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier = Modifier,
    symbol: String,
    title: String,
    subtitle: String,
    accent: Color,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            symbol,
            color = accent,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            title,
            color = MiraInk,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            subtitle,
            color = MiraMuted,
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
