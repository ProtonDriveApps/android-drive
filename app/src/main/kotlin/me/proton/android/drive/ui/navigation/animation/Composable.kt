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

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@ExperimentalAnimationApi
fun NavGraphBuilder.slideComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition:
    (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? = defaultEnterSlideTransition(route),
    exitTransition:
    (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? = defaultExitSlideTransition(route),
    popEnterTransition:
    (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? = defaultPopEnterSlideTransition(route),
    popExitTransition:
    (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? = defaultPopExitSlideTransition(route),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) = composable(
    route = route,
    arguments = arguments,
    deepLinks = deepLinks,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    popEnterTransition = popEnterTransition,
    popExitTransition = popExitTransition,
    content = content,
)

@ExperimentalAnimationApi
fun defaultEnterSlideTransition(
    route: String? = null,
    towards: AnimatedContentScope.SlideDirection = AnimatedContentScope.SlideDirection.Left,
    duration: Duration = DEFAULT_ANIMATION_DURATION,
    condition: AnimatedContentScope<NavBackStackEntry>.() -> Boolean = {
        initialState.destination.route == route && targetState.destination.route == route
    },
): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?) = {
    if (condition()) {
        slideIntoContainer(towards, tween(duration.inWholeMilliseconds.toInt()))
    } else {
        null
    }
}

@ExperimentalAnimationApi
fun defaultExitSlideTransition(
    route: String? = null,
    towards: AnimatedContentScope.SlideDirection = AnimatedContentScope.SlideDirection.Left,
    duration: Duration = DEFAULT_ANIMATION_DURATION,
    condition: AnimatedContentScope<NavBackStackEntry>.() -> Boolean = {
        initialState.destination.route == route && targetState.destination.route == route
    },
): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?) = {
    if (condition()) {
        slideOutOfContainer(towards, tween(duration.inWholeMilliseconds.toInt()))
    } else {
        null
    }
}

@ExperimentalAnimationApi
fun defaultPopEnterSlideTransition(
    route: String? = null,
    towards: AnimatedContentScope.SlideDirection = AnimatedContentScope.SlideDirection.Right,
    duration: Duration = DEFAULT_ANIMATION_DURATION,
    condition: AnimatedContentScope<NavBackStackEntry>.() -> Boolean = {
        initialState.destination.route == route && targetState.destination.route == route
    },
): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?) = {
    if (condition()) {
        slideIntoContainer(towards, tween(duration.inWholeMilliseconds.toInt()))
    } else {
        null
    }
}

@ExperimentalAnimationApi
fun defaultPopExitSlideTransition(
    route: String? = null,
    towards: AnimatedContentScope.SlideDirection = AnimatedContentScope.SlideDirection.Right,
    duration: Duration = DEFAULT_ANIMATION_DURATION,
    condition: AnimatedContentScope<NavBackStackEntry>.() -> Boolean = {
        initialState.destination.route == route && targetState.destination.route == route
    },
): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?) = {
    if (condition()) {
        slideOutOfContainer(towards, tween(duration.inWholeMilliseconds.toInt()))
    } else {
        null
    }
}

private val DEFAULT_ANIMATION_DURATION = 500.milliseconds
