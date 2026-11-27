/*
 * Copyright (c) 2026 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.android.drive.ui.navigation.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

fun tabSlideDirection(from: String?, to: String?, tabRoutes: List<String>): SlideDirection? {
    val fromIndex = tabRoutes.indexOf(from)
    val toIndex = tabRoutes.indexOf(to)
    return when {
        fromIndex < 0 || toIndex < 0 || fromIndex == toIndex -> null
        toIndex > fromIndex -> SlideDirection.Left
        else -> SlideDirection.Right
    }
}

fun tabEnterTransition(
    tabRoutes: () -> List<String>,
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    when (val towards = tabSlideDirection(tabRoutes())) {
        null -> fadeIn(tween(FALLBACK_FADE_DURATION_MS))
        else -> slideIntoContainer(towards, tween(TAB_SLIDE_DURATION_MS, easing = FastOutSlowInEasing))
    }
}

fun tabExitTransition(
    tabRoutes: () -> List<String>,
): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    when (val towards = tabSlideDirection(tabRoutes())) {
        null -> fadeOut(tween(FALLBACK_FADE_DURATION_MS))
        else -> slideOutOfContainer(towards, tween(TAB_SLIDE_DURATION_MS, easing = FastOutSlowInEasing))
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabSlideDirection(
    tabRoutes: List<String>,
): SlideDirection? = tabSlideDirection(initialState.destination.route, targetState.destination.route, tabRoutes)

private const val TAB_SLIDE_DURATION_MS = 300
private const val FALLBACK_FADE_DURATION_MS = 700
