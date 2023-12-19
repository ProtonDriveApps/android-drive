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

package me.proton.android.drive.photos.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import me.proton.android.drive.photos.presentation.state.PhotosItem
import me.proton.android.drive.photos.presentation.ui.component.PhotosEmptyWithBackupTurnedOff
import me.proton.android.drive.photos.presentation.viewevent.PhotosViewEvent
import me.proton.android.drive.photos.presentation.viewstate.PhotosViewState
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.files.presentation.component.onContent
import me.proton.core.drive.files.presentation.component.onEmpty
import me.proton.core.drive.files.presentation.component.onError
import me.proton.core.drive.files.presentation.component.onLoading
import me.proton.core.drive.files.presentation.state.HandleListEffect
import me.proton.core.drive.files.presentation.state.ListEffect
import me.proton.core.drive.link.domain.entity.LinkId

@Composable
fun Photos(
    viewState: PhotosViewState,
    viewEvent: PhotosViewEvent,
    photos: Flow<PagingData<PhotosItem>>,
    listEffect: Flow<ListEffect>,
    driveLinksFlow: Flow<Map<LinkId, DriveLink>>,
    selectedPhotos: Set<LinkId>,
    modifier: Modifier = Modifier,
) {
    val items = photos.collectAsLazyPagingItems()
    LaunchedEffect(items) {
        snapshotFlow { items.loadState }
            .distinctUntilChanged()
            .collect { loadState ->
                viewEvent.onLoadState(loadState, items.itemCount)
            }
    }
    listEffect.HandleListEffect(items = items)

    if (viewState.backupStatusViewState == null) {
        PhotosLoading(modifier)
    } else {
        viewState.listContentState
            .onEmpty { state ->
                if (viewState.showEmptyList == true) {
                    PhotosEmpty(
                        state = state,
                        viewState = viewState.backupStatusViewState,
                        showPhotosStateBanner = viewState.showPhotosStateBanner,
                        onEnable = viewEvent.onEnable,
                        onPermissions = viewEvent.onPermissions,
                        onRetry = viewEvent.onRetry,
                        onResolve = viewEvent.onResolve,
                        onGetStorage = viewEvent.onGetStorage,
                        onResolveMissingFolder = viewEvent.onResolveMissingFolder,
                    )
                } else {
                    PhotosEmptyWithBackupTurnedOff(
                        modifier = modifier,
                        onEnable = viewEvent.onEnable,
                    )
                }
            }
            .onLoading { PhotosLoading(modifier) }
            .onError { state ->
                PhotosError(
                    message = state.message,
                    actionResId = state.actionResId,
                    onAction = viewEvent.onErrorAction,
                )
            }
            .onContent {
                PhotosContent(
                    items = items,
                    driveLinksFlow = driveLinksFlow,
                    viewState = viewState.backupStatusViewState,
                    showPhotosStateBanner = viewState.showPhotosStateBanner,
                    selectedPhotos = selectedPhotos,
                    onClick = viewEvent.onDriveLink,
                    onLongClick = viewEvent.onSelectDriveLink,
                    onEnable = viewEvent.onEnable,
                    onPermissions = viewEvent.onPermissions,
                    onRetry = viewEvent.onRetry,
                    onResolve = viewEvent.onResolve,
                    onScroll = viewEvent.onScroll,
                    onGetStorage = viewEvent.onGetStorage,
                    onResolveMissingFolder = viewEvent.onResolveMissingFolder,
                    isRefreshEnabled = viewState.isRefreshEnabled,
                    isRefreshing = viewState.listContentState.isRefreshing,
                    onRefresh = viewEvent.onRefresh,
                )
            }
    }
}
