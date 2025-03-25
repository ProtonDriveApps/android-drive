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

package me.proton.android.drive.ui.dialog

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.photos.presentation.component.ConfirmDeleteAlbumDialogContent
import me.proton.android.drive.photos.presentation.component.ConfirmDeleteAlbumWithChildrenDialogContent
import me.proton.android.drive.photos.presentation.viewstate.ConfirmDeleteAlbumDialogViewState
import me.proton.android.drive.ui.viewmodel.ConfirmDeleteAlbumDialogViewModel

@Composable
fun ConfirmDeleteAlbumDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<ConfirmDeleteAlbumDialogViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(
        initialValue = viewModel.initialViewState
    )
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    Crossfade(viewState.showDialog) { dialog ->
        when (dialog) {
            ConfirmDeleteAlbumDialogViewState.Dialog.WITHOUT_CHILDREN -> {
                val viewEvent = remember(lifecycle) {
                    viewModel.viewEvent(onDismiss)
                }
                ConfirmDeleteAlbumDialogContent(
                    viewState = viewState.withoutChildrenViewState,
                    viewEvent = viewEvent,
                    modifier = modifier
                        .testTag(ConfirmDeleteAlbumDialogTestTag.withoutChildrenDialog),
                )
            }
            ConfirmDeleteAlbumDialogViewState.Dialog.WITH_CHILDREN -> {
                val viewEvent = remember(lifecycle) {
                    viewModel.viewEventWithChildren(onDismiss)
                }
                ConfirmDeleteAlbumWithChildrenDialogContent(
                    viewState = viewState.withChildrenViewState,
                    viewEvent = viewEvent,
                    modifier = modifier
                        .testTag(ConfirmDeleteAlbumDialogTestTag.withChildrenDialog),
                )
            }
        }
    }
}

object ConfirmDeleteAlbumDialogTestTag {
    const val withoutChildrenDialog = "without children dialog"
    const val withChildrenDialog = "with children dialog"
}
