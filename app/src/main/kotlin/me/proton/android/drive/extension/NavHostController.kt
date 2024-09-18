/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.core.drive.base.domain.util.coRunCatching

fun NavHostController.runFromRoute(route: String, block: () -> Unit) = takeIf {
    currentDestination?.route == route
}?.let {
    block()
}

@Composable
fun NavHostController.isCurrentDestination(route: String) = currentBackStack.map { entries ->
    entries.lastOrNull()?.destination?.route == route
}.collectAsState(initial = false)

val NavHostController.graphFlow: Flow<NavGraph?> get() = flow {
    while (true) {
        coRunCatching { this@graphFlow.graph }
            .getOrNull()
            .let { graph ->
                emit(graph)
                if (graph == null) delay(50) else return@flow
            }
    }
}

suspend fun NavHostController.ensureNavGraphSet(): NavHostController = graphFlow
    .distinctUntilChanged()
    .filterNotNull()
    .first()
    .let { this }
