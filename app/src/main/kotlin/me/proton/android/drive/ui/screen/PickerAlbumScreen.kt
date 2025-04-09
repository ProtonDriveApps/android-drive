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

package me.proton.android.drive.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.ui.viewmodel.PickerPhotosAndAlbumsViewModel
import me.proton.core.drive.base.presentation.extension.shadow

@Composable
fun PickerAlbumScreen(
    navigateBack: () -> Unit,
    onAddToAlbumDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<PickerPhotosAndAlbumsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(initialValue = null)
    val lifecycle = LocalLifecycleOwner.current
    val viewEvent = remember(lifecycle) {
        viewModel.viewEvent(
            navigateBack = navigateBack,
            onAddToAlbumDone = onAddToAlbumDone,
        )
    }
    viewState?.let { viewState ->
        PickerAlbumScreen(
            addToAlbumTitle = viewState.addToAlbumButtonTitle,
            isAddToAlbumButtonEnabled = viewState.isAddToAlbumButtonEnabled,
            isAddToAlbumInProgress = viewState.isAddingInProgress,
            isResetButtonEnabled = viewState.isResetButtonEnabled,
            onBack = viewEvent.onBackPressed,
            onReset = viewEvent.onReset,
            onAddToAlbum = viewEvent.onAddToAlbum,
            modifier = modifier.navigationBarsPadding(),
        )
    }
}

@Composable
fun PickerAlbumScreen(
    addToAlbumTitle: String,
    isAddToAlbumButtonEnabled: Boolean,
    isAddToAlbumInProgress: Boolean,
    isResetButtonEnabled: Boolean,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onAddToAlbum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        AlbumScreen(
            navigateToAlbumOptions = {},
            navigateToPhotosOptions = { _, _ -> },
            navigateToMultiplePhotosOptions = { _, _ -> },
            navigateToPreview = { _, _ -> },
            navigateToPicker = { _ -> },
            navigateBack = onBack,
        )
        BottomActions(
            addToAlbumTitle = addToAlbumTitle,
            onReset = onReset,
            onAddToAlbum = onAddToAlbum,
            isAddToAlbumButtonEnabled = isAddToAlbumButtonEnabled,
            isAddToAlbumInProgress = isAddToAlbumInProgress,
            isResetButtonEnabled = isResetButtonEnabled,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .shadow(),
        )
    }
}
