package com.volume_plus_plus.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Pacing for a held − / + button, matched to the held-key ramp in VolumeKeyService: a first step at
// once, a pause, then repeats that shorten from REPEAT_START_MS towards REPEAT_MIN_MS so a long
// hold accelerates. Kept in step with that file so both controls feel the same.
private const val FIRST_REPEAT_MS = 250L
private const val REPEAT_START_MS = 150L
private const val REPEAT_STEP_MS = 12L
private const val REPEAT_MIN_MS = 40L

/** Screen title + one-line subtitle, shared by all three tabs. */
@Composable
fun ScreenHeader(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Tonal explainer block — the one "contained" element in the flat design language. */
@Composable
fun InfoCard(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(14.dp),
        )
    }
}

/**
 * The app's slider: thick rounded track with a bar-shaped thumb, in the style of the Android 15
 * system volume panel. [steps] is kept so values still snap to real volume indices, but the tick
 * dots are hidden for a clean look.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        thumbColor = MaterialTheme.colorScheme.primary,
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        colors = colors,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interaction,
                colors = colors,
                enabled = enabled,
                thumbSize = DpSize(width = 5.dp, height = 40.dp),
            )
        },
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                colors = colors,
                enabled = enabled,
                thumbTrackGapSize = 5.dp,
                trackInsideCornerSize = 4.dp,
                drawStopIndicator = null,
                drawTick = { _, _ -> },
                modifier = Modifier.height(32.dp),
            )
        },
    )
}

/**
 * One of the − / + buttons flanking a slider: a single exact step per tap, and, held down, a repeat
 * on the same accelerating ramp a held hardware volume key gives (see
 * [com.volume_plus_plus.app.service.VolumeKeyService]) so every way of nudging a value feels like
 * the same control.
 *
 * A touch press drives the stepping itself rather than waiting for the click, so the first step
 * lands immediately like a real key press. The click that follows on release would otherwise add a
 * second step, so it is suppressed — but only when a press actually preceded it, because a click
 * with no press behind it is how a screen reader activates the button, and that one must still step.
 */
@Composable
fun StepButton(
    @DrawableRes icon: Int,
    description: String,
    enabled: Boolean,
    onStep: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var handledByPress by remember { mutableStateOf(false) }

    // Keyed on `enabled` as well, so reaching either end of the range cancels a running repeat
    // instead of spinning against a value that can't move any further.
    LaunchedEffect(pressed, enabled) {
        if (!pressed) return@LaunchedEffect
        if (!enabled) {
            // Ran into the end of the range mid-hold. No click can follow a disabled button, so the
            // flag has to be cleared here or it would swallow the next screen-reader activation.
            handledByPress = false
            return@LaunchedEffect
        }
        handledByPress = true
        onStep()
        delay(FIRST_REPEAT_MS)
        var gap = REPEAT_START_MS
        while (true) {
            onStep()
            delay(gap)
            gap = (gap - REPEAT_STEP_MS).coerceAtLeast(REPEAT_MIN_MS)
        }
    }

    IconButton(
        onClick = {
            if (!handledByPress) onStep()
            // Cleared here rather than when the press ends: the two are racing coroutines, and
            // clearing it first would let this click through as an extra step at the end of a hold.
            handledByPress = false
        },
        enabled = enabled,
        interactionSource = interaction,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(20.dp),
        )
    }
}
