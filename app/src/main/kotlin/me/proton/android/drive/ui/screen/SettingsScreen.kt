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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.ui.viewmodel.SettingsViewModel
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.settings.presentation.Settings

@Composable
fun SettingsScreen(
    navigateBack: () -> Unit,
    navigateToAccountSettings: () -> Unit,
    navigateToAppAccess: () -> Unit,
    navigateToAutoLockDurations: () -> Unit,
    navigateToPhotosBackup: () -> Unit,
    navigateToDefaultHomeTab: () -> Unit,
    navigateToLog: () -> Unit,
    navigateToSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val settingsViewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = null)
    val snackbarHostState = remember { ProtonSnackbarHostState() }

    LaunchedEffect(key1 = viewModel) {
        viewModel.errorMessage.onEach { message ->
            snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, message)
        }.launchIn(this)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .testTag(SettingsScreenTestTag.screen)
    ) {
        settingsViewState?.let { viewState ->
            Settings(
                viewState = viewState,
                viewEvent = viewModel.viewEvent(
                    navigateBack = navigateBack,
                    navigateToAccountSettings = navigateToAccountSettings,
                    navigateToAppAccess = navigateToAppAccess,
                    navigateToAutoLockDurations = navigateToAutoLockDurations,
                    navigateToPhotosBackup = navigateToPhotosBackup,
                    navigateToDefaultHomeTab = navigateToDefaultHomeTab,
                    navigateToLog = navigateToLog,
                    navigateToSignOut = navigateToSignOut
                ),
            )

            ProtonSnackbarHost(
                modifier = Modifier.align(Alignment.BottomCenter),
                hostState = snackbarHostState,
            )
        }
    }
}

object SettingsScreenTestTag {
    const val screen = "settings screen"
}
