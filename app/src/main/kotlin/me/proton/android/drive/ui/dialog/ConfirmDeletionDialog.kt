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

package me.proton.android.drive.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.ui.viewmodel.ConfirmDeletionDialogViewModel
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.files.presentation.component.confirmation.ConfirmDeletionDialogContent

@Composable
@ExperimentalCoroutinesApi
fun ConfirmDeletionDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<ConfirmDeletionDialogViewModel>()
    val viewEvent = remember(viewModel, onDismiss) { viewModel.viewEvent(onDismiss) }
    val flow = remember(viewModel) { viewModel.viewState }
    val viewState by rememberFlowWithLifecycle(flow).collectAsState(initial = viewModel.initialViewState)

    val name = viewState.name ?: return

    ConfirmDeletionDialogContent(
        modifier = modifier,
        name = name,
        onDismiss = onDismiss,
        onConfirm = viewEvent.onConfirm
    )
}
