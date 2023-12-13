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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.photos.presentation.component.BackupIssues
import me.proton.android.drive.ui.viewmodel.BackupIssuesViewModel
import me.proton.core.compose.flow.rememberFlowWithLifecycle

@Composable
fun BackupIssuesScreen(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    navigateToSkipIssues: () -> Unit,
) {
    val viewModel = hiltViewModel<BackupIssuesViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    val viewEvent = remember {
        viewModel.viewEvent(
            navigateBack = navigateBack,
            navigateToSkipIssues = navigateToSkipIssues,
        )
    }

    BackupIssues(
        medias = viewState.medias,
        onBack = viewEvent.onBack,
        onSkip = viewEvent.onSkip,
        onRetryAll = viewEvent.onRetryAll,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    )
}
