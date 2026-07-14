package com.vibecode.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.vibecode.ide.ui.theme.AuroraCyan
import com.vibecode.ide.ui.theme.AuroraViolet
import com.vibecode.ide.ui.theme.VoidBlack

/**
 * Signature background used behind hero / empty-state surfaces: two soft
 * radial glows (violet top-left, cyan bottom-right) fading into the void.
 * Cheap to draw (no blur passes) so it stays smooth on low-end devices.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    Box(
        modifier
            .fillMaxSize()
            .background(VoidBlack)
            .background(
                Brush.radialGradient(
                    colors = listOf(AuroraViolet.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(0.05f, 0f),
                    radius = 900f,
                ),
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(AuroraCyan.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(1200f, 1600f),
                    radius = 1100f,
                ),
            ),
        content = content,
    )
}

/** A small reusable brush for text/icons that want the brand gradient treatment. */
val AuroraGradientBrush = Brush.linearGradient(listOf(AuroraViolet, AuroraCyan))
