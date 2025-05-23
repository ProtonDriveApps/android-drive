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

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.photos.presentation.component.Album
import me.proton.android.drive.ui.viewmodel.AlbumViewModel
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.base.presentation.component.RepeatOnLifecycleLaunchedEffect
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.selection.domain.entity.SelectionId

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun AlbumScreen(
    navigateToAlbumOptions: (AlbumId) -> Unit,
    navigateToPhotosOptions: (FileId, AlbumId?, SelectionId?) -> Unit,
    navigateToMultiplePhotosOptions: (selectionId: SelectionId, AlbumId?) -> Unit,
    navigateToPreview: (FileId, AlbumId) -> Unit,
    navigateToPicker: (AlbumId) -> Unit,
    navigateToShareViaInvitations: (AlbumId) -> Unit,
    navigateToManageAccess: (AlbumId) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<AlbumViewModel>()
    val viewState = viewModel.viewState.collectAsStateWithLifecycle(initialValue = null)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val viewEvent = remember(lifecycle) {
        viewModel.viewEvent(
            navigateToAlbumOptions = navigateToAlbumOptions,
            navigateToPhotosOptions = navigateToPhotosOptions,
            navigateToMultiplePhotosOptions = navigateToMultiplePhotosOptions,
            navigateToPreview = navigateToPreview,
            navigateToPicker = navigateToPicker,
            navigateToShareViaInvitations = navigateToShareViaInvitations,
            navigateToManageAccess = navigateToManageAccess,
            navigateBack = navigateBack,
            lifecycle = lifecycle,
        )
    }
    val photoItems = viewModel.driveLinks.collectAsLazyPagingItems()
    val listEffect = rememberFlowWithLifecycle(flow = viewModel.listEffect)

    viewState.value?.let { viewState ->
        val selectedPhotos by viewState.selected.collectAsStateWithLifecycle(initialValue = emptySet())
        val inMultiselect = remember(selectedPhotos) { selectedPhotos.isNotEmpty() }

        BackHandler(enabled = inMultiselect) { viewEvent.onBack() }
        RepeatOnLifecycleLaunchedEffect {
            viewModel.initializeSelectionInPickerMode()
        }
        Album(
            viewState = viewState,
            viewEvent = viewEvent,
            items = photoItems,
            listEffect = listEffect,
            driveLinksFlow = viewModel.driveLinksMap,
            selectedPhotos = selectedPhotos,
            isRefreshEnabled = viewState.isRefreshEnabled,
            isRefreshing = viewState.listContentState.isRefreshing,
            onRefresh = viewEvent.onRefresh,
            modifier = modifier.testTag(AlbumScreenTestTag.screen),
        )
    }
}

object AlbumScreenTestTag {
    const val screen = "album screen"
}
