/*
 * Copyright (c) 2023 Proton AG.
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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


@ExperimentalAnimationApi
fun defaultEnterSlideTransition(
    route: String? = null,
    towards: AnimatedContentTransitionScope.SlideDirection = AnimatedContentTransitionScope.SlideDirection.Left,
    duration: Duration = DEFAULT_ANIMATION_DURATION,
    condition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> Boolean = {
        initialState.destination.route == route && targetState.destination.route == route
    },
): (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?) = {
    if (condition()) {
        slideIntoContainer(towards, tween(duration.inWholeMilliseconds.toInt()))
    } else {
        null
    }
}

@ExperimentalAnimationApi
fun defaultExitSlideTransition(
    route: String? = null,
    towards: AnimatedContentTransitionScope.SlideDirection = AnimatedContentTransitionScope.SlideDirection.Left,
    duration: Duration = DEFAULT_ANIMATION_DURATION,
    condition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> Boolean = {
        initialState.destination.route == route && targetState.destination.route == route
    },
): (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?) = {
    if (condition()) {
        slideOutOfContainer(towards, tween(duration.inWholeMilliseconds.toInt()))
    } else {
        null
    }
}

@ExperimentalAnimationApi
fun defaultPopEnterSlideTransition(
    route: String? = null,
    towards: AnimatedContentTransitionScope.SlideDirection = AnimatedContentTransitionScope.SlideDirection.Right,
    duration: Duration = DEFAULT_ANIMATION_DURATION,
    condition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> Boolean = {
        initialState.destination.route == route && targetState.destination.route == route
    },
): (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?) = {
    if (condition()) {
        slideIntoContainer(towards, tween(duration.inWholeMilliseconds.toInt()))
    } else {
        null
    }
}

@ExperimentalAnimationApi
fun defaultPopExitSlideTransition(
    route: String? = null,
    towards: AnimatedContentTransitionScope.SlideDirection = AnimatedContentTransitionScope.SlideDirection.Right,
    duration: Duration = DEFAULT_ANIMATION_DURATION,
    condition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> Boolean = {
        initialState.destination.route == route && targetState.destination.route == route
    },
): (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?) = {
    if (condition()) {
        slideOutOfContainer(towards, tween(duration.inWholeMilliseconds.toInt()))
    } else {
        null
    }
}

private val DEFAULT_ANIMATION_DURATION = 500.milliseconds
