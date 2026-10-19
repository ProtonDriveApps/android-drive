/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.photos.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.photos.presentation.viewmodel.EnableBackupForBucketsDialogViewModel
import me.proton.core.drive.base.presentation.component.RunAction

@Composable
@ExperimentalCoroutinesApi
fun EnableBackupForBucketsDialog(
    modifier: Modifier = Modifier,
    runAction: RunAction,
) {
    val viewModel = hiltViewModel<EnableBackupForBucketsDialogViewModel>()
    val viewEvent = remember(viewModel) {
        viewModel.viewEvent(runAction)
    }
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(
        initialValue = viewModel.initialViewState
    )

    EnableBackupForBucketsDialogContent(
        modifier = modifier,
        viewState = viewState,
        onDismiss = viewEvent.onDismiss,
        onToggleBucket = viewEvent.onToggleBucket,
        onSave = viewEvent.onSave,
    )
}
