package com.vibecode.ide.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import com.vibecode.ide.ui.theme.Motion

/**
 * Modifier that shrinks a composable slightly while pressed, giving every
 * tappable surface (cards, chips, tabs) a satisfying, physical feel.
 */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    var pressed by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> pressed = true
                is PressInteraction.Release, is PressInteraction.Cancel -> pressed = false
            }
        }
    }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, animationSpec = Motion.responsive(), label = "pressScale")
    return this.scale(scale)
}
