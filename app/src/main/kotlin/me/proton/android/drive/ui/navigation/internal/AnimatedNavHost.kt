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

package me.proton.android.drive.ui.navigation.internal

/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import androidx.navigation.createGraph
import androidx.navigation.get
import me.proton.android.drive.log.DriveLogTag
import me.proton.core.util.kotlin.CoreLogger

/**
 * Provides in place in the Compose hierarchy for self contained navigation to occur.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navHostController].
 *
 * The builder passed into this method is [remember]ed. This means that for this NavHost, the
 * contents of the builder cannot be changed.
 *
 * @param navHostController the navController for this host
 * @param startDestination the route for the start destination
 * @param modifier The modifier to be applied to the layout.
 * @param route the route for the graph
 * @param builder the builder used to construct the graph
 */
@Composable
@ExperimentalAnimationApi
fun AnimatedNavHost(
    navHostController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit,
) {
    navHostController.runIfNavControllerViewModelCanBeSet { navController ->
        AnimatedNavHost(
            navController,
            remember(route, startDestination, builder) {
                navController.createGraph(startDestination, route, builder)
            },
            modifier
        )
    }
}

@Suppress("ComposableNaming")
@Composable
private fun NavHostController.runIfNavControllerViewModelCanBeSet(
    block: @Composable (NavHostController) -> Unit
) {
    try {
        getViewModelStoreOwner(0)
    } catch (e: IllegalStateException) {
        CoreLogger.d(DriveLogTag.UI, e, "getViewModelStoreOwner failed (backQueue.size=${backQueue.size})")
        if (backQueue.isNotEmpty()) {
            return
        }
    } catch (e: IllegalArgumentException) {
        CoreLogger.d(DriveLogTag.UI, e, "getViewModelStoreOwner failed (backQueue.size=${backQueue.size})")
    }
    block(this)
}

/**
 * Provides in place in the Compose hierarchy for self contained navigation to occur.
 *
 * Once this is called, any Composable within the given [NavGraphBuilder] can be navigated to from
 * the provided [navController].
 *
 * The graph passed into this method is [remember]ed. This means that for this NavHost, the graph
 * cannot be changed.
 *
 * @param navController the navController for this host
 * @param graph the graph for this host
 * @param modifier The modifier to be applied to the layout.
 */
@ExperimentalAnimationApi
@Composable
fun AnimatedNavHost(
    navController: NavHostController,
    graph: NavGraph,
    modifier: Modifier = Modifier,
) {

    com.google.accompanist.navigation.animation.AnimatedNavHost(navController = navController, graph = graph, modifier = modifier)

    val modalBottomSheetNavigator = navController.navigatorProvider.get<Navigator<out NavDestination>>(
        ModalBottomSheetNavigator.NAME
    ) as? ModalBottomSheetNavigator ?: return

    ModalBottomSheetHost(modalBottomSheetNavigator)
}
