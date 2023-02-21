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

package me.proton.android.drive.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.ui.viewmodel.LauncherViewModel
import me.proton.android.drive.ui.viewstate.LauncherViewState
import me.proton.android.drive.ui.viewstate.PrimaryAccountState
import me.proton.core.compose.component.DeferredCircularProgressIndicator
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.domain.entity.UserId

@Composable
@ExperimentalCoroutinesApi
fun LauncherScreen(
    navigateToHomeScreen: (userId: UserId) -> Unit,
    navigateToWelcome: (userId: UserId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val launcherViewModel = hiltViewModel<LauncherViewModel>()
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Launcher(launcherViewModel, navigateToHomeScreen, navigateToWelcome)
    }
}

@Composable
@ExperimentalCoroutinesApi
internal fun Launcher(
    viewModel: LauncherViewModel,
    navigateToHomeScreen: (userId: UserId) -> Unit,
    navigateToWelcome: (userId: UserId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasShownWelcomeFlow = remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        hasShownWelcomeFlow.value = viewModel.hasShownWelcomeFlow()
    }
    hasShownWelcomeFlow.value?.let { hasShown ->
        Launcher(viewModel, hasShown, navigateToHomeScreen, navigateToWelcome, modifier)
    }
}

@Composable
@ExperimentalCoroutinesApi
internal fun Launcher(
    viewModel: LauncherViewModel,
    hasShown: Boolean,
    navigateToHomeScreen: (userId: UserId) -> Unit,
    navigateToWelcome: (userId: UserId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewState by rememberFlowWithLifecycle(viewModel.viewState)
        .collectAsState(initial = LauncherViewState.initialValue)
    Launcher(viewState, hasShown, navigateToHomeScreen, navigateToWelcome, modifier)
}

@Composable
@ExperimentalCoroutinesApi
internal fun Launcher(
    viewState: LauncherViewState,
    hasShown: Boolean,
    navigateToHomeScreen: (userId: UserId) -> Unit,
    navigateToWelcome: (userId: UserId) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hasShown) {
        Launcher(viewState, navigateToHomeScreen, modifier)
    } else {
        val state = viewState.primaryAccountState
        LaunchedEffect(state) {
            if (state is PrimaryAccountState.SignedIn) {
                navigateToWelcome(state.userId)
            }
        }
    }
}

@Composable
internal fun Launcher(
    viewState: LauncherViewState,
    navigateToHomeScreen: (userId: UserId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = viewState.primaryAccountState
    LaunchedEffect(state) {
        if (state is PrimaryAccountState.SignedIn) {
            navigateToHomeScreen(state.userId)
        }
    }
    DeferredCircularProgressIndicator(modifier)
}
