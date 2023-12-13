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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.photos.presentation.component.ConfirmSkipIssuesDialogContent
import me.proton.android.drive.ui.viewmodel.ConfirmSkipIssuesDialogViewModel

@Composable
@ExperimentalCoroutinesApi
fun ConfirmSkipIssuesDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val viewModel = hiltViewModel<ConfirmSkipIssuesDialogViewModel>()
    val viewEvent = remember(viewModel, onConfirm) { viewModel.viewEvent(onConfirm) }

    ConfirmSkipIssuesDialogContent(
        modifier = modifier,
        onDismiss = onDismiss,
        onConfirm = viewEvent.onConfirm
    )
}
