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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.presentation.component.Albums
import me.proton.android.drive.photos.presentation.component.BackupPermissions
import me.proton.android.drive.photos.presentation.component.Photos
import me.proton.android.drive.photos.presentation.component.PhotosStatesIndicator
import me.proton.android.drive.photos.presentation.state.PhotosItem
import me.proton.android.drive.photos.presentation.viewevent.PhotosViewEvent
import me.proton.android.drive.photos.presentation.viewstate.PhotosViewState
import me.proton.android.drive.ui.effect.HandleHomeEffect
import me.proton.android.drive.ui.effect.PhotosEffect
import me.proton.android.drive.ui.viewevent.PhotosAndAlbumsViewEvent
import me.proton.android.drive.ui.viewmodel.AlbumsViewModel
import me.proton.android.drive.ui.viewmodel.PhotosAndAlbumsViewModel
import me.proton.android.drive.ui.viewmodel.PhotosViewModel
import me.proton.android.drive.ui.viewstate.HomeScaffoldState
import me.proton.android.drive.ui.viewstate.PhotosAndAlbumsViewState
import me.proton.android.drive.ui.viewstate.PhotosAndAlbumsViewState.Tab
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.protonTextButtonColors
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.headlineSmallNorm
import me.proton.core.compose.theme.headlineSmallUnspecified
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.base.presentation.component.TopBarActions
import me.proton.core.drive.base.presentation.effect.ListEffect
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.i18n.R as I18N

@Composable
fun PhotosAndAlbumsScreen(
    homeScaffoldState: HomeScaffoldState,
    navigateToPhotosPermissionRationale: () -> Unit,
    navigateToPhotosPreview: (fileId: FileId) -> Unit,
    navigateToPhotosOptions: (fileId: FileId) -> Unit,
    navigateToMultiplePhotosOptions: (selectionId: SelectionId) -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToPhotosIssues: (FolderId) -> Unit,
    navigateToPhotosUpsell: () -> Unit,
    navigateToBackupSettings: () -> Unit,
    navigateToNotificationPermissionRationale: () -> Unit,
    navigateToCreateNewAlbum: () -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<PhotosAndAlbumsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(
        initialValue = viewModel.initialViewState
    )
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val viewEvent = remember(lifecycle) {
        viewModel.viewEvent()
    }

    Box(modifier) {
        val defaultTitle = @Composable { modifier: Modifier ->
            Tabs(
                modifier = modifier.offset(x = (-4).dp),
                viewState = viewState,
                viewEvent = viewEvent,
            )
        }
        when (viewState.selectedTab) {
            Tab.PHOTOS -> PhotosTab(
                homeScaffoldState = homeScaffoldState,
                navigateToPhotosPermissionRationale = navigateToPhotosPermissionRationale,
                navigateToPhotosPreview = navigateToPhotosPreview,
                navigateToPhotosOptions = navigateToPhotosOptions,
                navigateToMultiplePhotosOptions = navigateToMultiplePhotosOptions,
                navigateToSubscription = navigateToSubscription,
                navigateToPhotosIssues = navigateToPhotosIssues,
                navigateToPhotosUpsell = navigateToPhotosUpsell,
                navigateToBackupSettings = navigateToBackupSettings,
                navigateToNotificationPermissionRationale = navigateToNotificationPermissionRationale,
                defaultTitle = defaultTitle,
            )

            Tab.ALBUMS -> AlbumTab(
                homeScaffoldState = homeScaffoldState,
                navigateToCreateNewAlbum = navigateToCreateNewAlbum,
                navigateToAlbum = navigateToAlbum,
                defaultTitle = defaultTitle,
            )
        }
    }
}

@Composable
private fun AlbumTab(
    homeScaffoldState: HomeScaffoldState,
    navigateToCreateNewAlbum: () -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
    modifier: Modifier = Modifier,
    defaultTitle: @Composable (Modifier) -> Unit,
) {
    val viewModel = hiltViewModel<AlbumsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(
        initialValue = viewModel.initialViewState
    )
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val viewEvent = remember(lifecycle) {
        viewModel.viewEvent(
            navigateToCreateNewAlbum = navigateToCreateNewAlbum,
            navigateToAlbum = navigateToAlbum,
        )
    }

    viewModel.HandleHomeEffect(homeScaffoldState)

    LaunchedEffect(viewState) {
        homeScaffoldState.topAppBar.value = {
            TopAppBar(
                navigationIcon = if (viewState.navigationIconResId != 0) {
                    painterResource(id = viewState.navigationIconResId)
                } else null,
                onNavigationIcon = viewEvent.onTopAppBarNavigation,
                title = defaultTitle,
                notificationDotVisible = false,
                actions = {
                    TopBarActions(viewState.topBarActions)
                }
            )
        }
    }
    Albums(
        viewState = viewState,
        viewEvent = viewEvent,
        items = viewModel.albumItems,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun PhotosTab(
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
    navigateToNotificationPermissionRationale: () -> Unit,
    defaultTitle: @Composable (Modifier) -> Unit,
) {
    val viewModel = hiltViewModel<PhotosViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    val viewEvent = remember(lifecycle) {
        viewModel.viewEvent(
            navigateToPreview = navigateToPhotosPreview,
            navigateToPhotosOptions = navigateToPhotosOptions,
            navigateToMultiplePhotosOptions = navigateToMultiplePhotosOptions,
            navigateToSubscription = navigateToSubscription,
            navigateToPhotosIssues = navigateToPhotosIssues,
            navigateToPhotosUpsell = navigateToPhotosUpsell,
            navigateToBackupSettings = navigateToBackupSettings,
            lifecycle = lifecycle,
        )
    }

    viewModel.HandleHomeEffect(homeScaffoldState)

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

    PhotosTab(
        homeScaffoldState = homeScaffoldState,
        viewState = viewState,
        viewEvent = viewEvent,
        photos = photos,
        listEffect = listEffect,
        driveLinksFlow = viewModel.driveLinksMap,
        modifier = modifier,
        defaultTitle = defaultTitle,
    )
    BackupPermissions(
        viewState = viewModel.backupPermissionsViewModel.initialViewState,
        viewEvent = viewModel.backupPermissionsViewModel.viewEvent(
            navigateToPhotosPermissionRationale
        ),
        navigateToNotificationPermissionRationale = navigateToNotificationPermissionRationale,
    )
}

@Composable
private fun PhotosTab(
    homeScaffoldState: HomeScaffoldState,
    viewState: PhotosViewState,
    viewEvent: PhotosViewEvent,
    photos: Flow<PagingData<PhotosItem>>,
    listEffect: Flow<ListEffect>,
    driveLinksFlow: Flow<Map<LinkId, DriveLink>>,
    defaultTitle: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedPhotos by rememberFlowWithLifecycle(flow = viewState.selected)
        .collectAsState(initial = emptySet())
    val inMultiselect = remember(selectedPhotos) { selectedPhotos.isNotEmpty() }

    BackHandler(enabled = inMultiselect) { viewEvent.onBack() }

    LaunchedEffect(viewState) {
        homeScaffoldState.topAppBar.value = {
            TopAppBar(
                navigationIcon = if (viewState.navigationIconResId != 0) {
                    painterResource(id = viewState.navigationIconResId)
                } else null,
                onNavigationIcon = viewEvent.onTopAppBarNavigation,
                title = if (viewState.inMultiselect) {
                    { Title(viewState.title, false) }
                } else {
                    defaultTitle
                },
                notificationDotVisible = false,
                actions = {
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
            )
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
private fun Tabs(
    viewState: PhotosAndAlbumsViewState,
    viewEvent: PhotosAndAlbumsViewEvent,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabItem(
            modifier = Modifier.padding(end = ProtonDimens.SmallSpacing),
            tab = Tab.PHOTOS,
            text = I18N.string.photos_title,
            viewState = viewState,
            viewEvent = viewEvent,
        )
        VerticalDivider(
            modifier = Modifier.padding(vertical = ProtonDimens.DefaultSpacing),
            color = ProtonTheme.colors.separatorNorm,
        )
        TabItem(
            modifier = Modifier.padding(start = ProtonDimens.SmallSpacing),
            tab = Tab.ALBUMS,
            text = I18N.string.albums_title,
            viewState = viewState,
            viewEvent = viewEvent,
        )
    }
}

@Composable
private fun TabItem(
    tab: Tab,
    text: Int,
    viewState: PhotosAndAlbumsViewState,
    viewEvent: PhotosAndAlbumsViewEvent,
    modifier: Modifier = Modifier,
) {
    TabItem(
        modifier = modifier,
        text = text,
        selected = viewState.selectedTab == tab,
        onClick = { viewEvent.onSelectTab(tab) },
    )
}

@Composable
private fun TabItem(
    text: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProtonButton(
        modifier = modifier,
        onClick = onClick,
        elevation = null,
        shape = ProtonTheme.shapes.small,
        border = null,
        colors = ButtonDefaults.protonTextButtonColors(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = stringResource(id = text),
            style = if (selected) {
                ProtonTheme.typography.headlineSmallNorm
            } else {
                ProtonTheme.typography.headlineSmallUnspecified.copy(color = ProtonTheme.colors.textHint)
            }
        )
    }
}

@Preview
@Composable
fun TabsPreview() {
    ProtonTheme {
        Tabs(
            modifier = Modifier.height(ProtonDimens.DefaultButtonMinHeight),
            viewState = PhotosAndAlbumsViewState(selectedTab = Tab.PHOTOS),
            viewEvent = object : PhotosAndAlbumsViewEvent {}
        )
    }
}
