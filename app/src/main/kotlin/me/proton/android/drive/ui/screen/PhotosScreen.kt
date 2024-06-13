/*
 * Copyright (c) 2023-2024 Proton AG.
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.presentation.component.BackupPermissions
import me.proton.android.drive.photos.presentation.component.Photos
import me.proton.android.drive.photos.presentation.component.PhotosStatesIndicator
import me.proton.android.drive.photos.presentation.state.PhotosItem
import me.proton.android.drive.photos.presentation.viewevent.PhotosViewEvent
import me.proton.android.drive.photos.presentation.viewstate.PhotosViewState
import me.proton.android.drive.ui.effect.HandleHomeEffect
import me.proton.android.drive.ui.effect.PhotosEffect
import me.proton.android.drive.ui.viewmodel.PhotosViewModel
import me.proton.android.drive.ui.viewstate.HomeScaffoldState
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.base.presentation.component.TopBarActions
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.base.presentation.effect.ListEffect
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.selection.domain.entity.SelectionId

@Composable
fun PhotosScreen(
    homeScaffoldState: HomeScaffoldState,
    modifier: Modifier = Modifier,
    navigateToPhotosPermissionRationale: () -> Unit,
    navigateToPhotosPreview: (fileId: FileId) -> Unit,
    navigateToPhotosOptions: (fileId: FileId) -> Unit,
    navigateToMultiplePhotosOptions: (selectionId: SelectionId) -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToPhotosIssues: (FolderId) -> Unit,
    navigateToPhotosUpsell: () -> Unit,
    navigateToBackupSettings: () -> Unit,
) {
    val viewModel = hiltViewModel<PhotosViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    val viewEvent = remember {
        viewModel.viewEvent(
            navigateToPreview = navigateToPhotosPreview,
            navigateToPhotosOptions = navigateToPhotosOptions,
            navigateToMultiplePhotosOptions = navigateToMultiplePhotosOptions,
            navigateToSubscription = navigateToSubscription,
            navigateToPhotosIssues = navigateToPhotosIssues,
            navigateToPhotosUpsell = navigateToPhotosUpsell,
            navigateToBackupSettings = navigateToBackupSettings,
        )
    }
    val photos = rememberFlowWithLifecycle(flow = viewModel.driveLinks)
    val listEffect = rememberFlowWithLifecycle(flow = viewModel.listEffect)

    LaunchedEffect(viewModel, LocalContext.current) {
        viewModel.photosEffect.onEach { effect ->
            when (effect) {
                PhotosEffect.ShowUpsell -> launch(Dispatchers.Main) {
                    viewEvent.onShowUpsell()
                }
            }
        }.launchIn(this)
    }

    viewModel.HandleHomeEffect(homeScaffoldState)

    PhotosScreen(
        viewState = viewState,
        viewEvent = viewEvent,
        homeScaffoldState = homeScaffoldState,
        photos = photos,
        listEffect = listEffect,
        driveLinksFlow = viewModel.driveLinksMap,
        modifier = modifier,
    )
    BackupPermissions(
        viewState = viewModel.backupPermissionsViewModel.initialViewState,
        viewEvent = viewModel.backupPermissionsViewModel.viewEvent(
            navigateToPhotosPermissionRationale
        ),
    )
}

@Composable
fun PhotosScreen(
    viewState: PhotosViewState,
    viewEvent: PhotosViewEvent,
    homeScaffoldState: HomeScaffoldState,
    photos: Flow<PagingData<PhotosItem>>,
    listEffect: Flow<ListEffect>,
    driveLinksFlow: Flow<Map<LinkId, DriveLink>>,
    modifier: Modifier = Modifier,
) {
    val selectedPhotos by rememberFlowWithLifecycle(flow = viewState.selected)
        .collectAsState(initial = emptySet())
    val inMultiselect = remember(selectedPhotos) { selectedPhotos.isNotEmpty() }

    BackHandler(enabled = inMultiselect) { viewEvent.onBack() }

    LaunchedEffect(viewState) {
        homeScaffoldState.topAppBar.value = {
            TopAppBar(
                viewState = viewState,
                viewEvent = viewEvent,
            ) {
                TopBarActions(actionFlow = viewState.topBarActions)
                AnimatedVisibility(
                    visible = viewState.showPhotosStateIndicator,
                ) {
                    IconButton(
                        modifier = modifier.clip(shape = CircleShape),
                        onClick = { viewEvent.onStatusClicked() }
                    ) {
                        viewState.backupStatusViewState?.let { backupStatusViewState ->
                            PhotosStatesIndicator(backupStatusViewState)
                        }
                    }
                }
            }
        }
    }

    Photos(
        viewState = viewState,
        viewEvent = viewEvent,
        photos = photos,
        listEffect = listEffect,
        driveLinksFlow = driveLinksFlow,
        selectedPhotos = selectedPhotos,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun TopAppBar(
    viewState: PhotosViewState,
    viewEvent: PhotosViewEvent,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        navigationIcon = if (viewState.navigationIconResId != 0) {
            painterResource(id = viewState.navigationIconResId)
        } else null,
        notificationDotVisible = viewState.notificationDotVisible,
        onNavigationIcon = viewEvent.onTopAppBarNavigation,
        title = viewState.title,
        actions = actions
    )
}
