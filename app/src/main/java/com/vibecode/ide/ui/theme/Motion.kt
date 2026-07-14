package com.vibecode.ide.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Centralized motion spec so every animation across the app (panel slides, tab
 * transitions, dialogs, chat bubbles) feels consistent and alive rather than
 * mechanical. Two families are offered: eased `tween`s for layout/size changes,
 * and springs for anything that should feel touch-responsive (taps, drags,
 * entrances).
 */
object Motion {
    const val DURATION_FAST = 120
    const val DURATION_MEDIUM = 220
    const val DURATION_SLOW = 380

    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val StandardEasing: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    fun <T> fast(): FiniteAnimationSpec<T> = tween(DURATION_FAST, easing = StandardEasing)
    fun <T> medium(): FiniteAnimationSpec<T> = tween(DURATION_MEDIUM, easing = StandardEasing)
    fun <T> slow(): FiniteAnimationSpec<T> = tween(DURATION_SLOW, easing = EmphasizedEasing)
    fun <T> panel(): FiniteAnimationSpec<T> = tween(DURATION_MEDIUM, easing = EmphasizedEasing)

    /** Snappy, slightly bouncy spring for taps, cards, chips — feels alive without wobbling. */
    fun <T> responsive(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Softer spring for larger surfaces (sheets, panels) entering/leaving the screen. */
    fun <T> surface(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow,
    )

    /** Near-critically-damped spring for tight, precise motion (drag handles, gutters). */
    fun <T> precise(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
}
