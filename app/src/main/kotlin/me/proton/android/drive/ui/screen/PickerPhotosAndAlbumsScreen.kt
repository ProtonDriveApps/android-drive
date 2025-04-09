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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.photos.presentation.component.AddToAlbumButton
import me.proton.android.drive.ui.viewmodel.PhotosAndAlbumsViewModel
import me.proton.android.drive.ui.viewmodel.PickerPhotosAndAlbumsViewModel
import me.proton.android.drive.ui.viewstate.PhotosAndAlbumsViewState.Tab
import me.proton.android.drive.ui.viewstate.rememberHomeScaffoldState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.extension.shadow
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.base.presentation.component.TopAppBar as BaseTopAppBar
import me.proton.core.presentation.R as CorePresentation

@Composable
fun PickerPhotosAndAlbumsScreen(
    navigateToAlbum: (AlbumId, AlbumId?) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<PickerPhotosAndAlbumsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(initialValue = null)
    val lifecycle = LocalLifecycleOwner.current
    val viewEvent = remember(lifecycle) {
        viewModel.viewEvent(
            navigateBack = navigateBack,
            onAddToAlbumDone = navigateBack,
        )
    }
    viewState?.let { viewState ->
        PickerPhotosAndAlbumsScreen(
            addToAlbumTitle = viewState.addToAlbumButtonTitle,
            isAddToAlbumButtonEnabled = viewState.isAddToAlbumButtonEnabled,
            isAddToAlbumInProgress = viewState.isAddingInProgress,
            isResetButtonEnabled = viewState.isResetButtonEnabled,
            onTopAppBarNavigationIcon = viewEvent.onBackPressed,
            navigateToAlbum = { albumId: AlbumId ->
                navigateToAlbum(albumId, viewModel.destinationAlbumId)
            },
            onReset = viewEvent.onReset,
            onAddToAlbum = viewEvent.onAddToAlbum,
            modifier = modifier.navigationBarsPadding(),
        )
    }
}

@Composable
fun PickerPhotosAndAlbumsScreen(
    addToAlbumTitle: String,
    isAddToAlbumButtonEnabled: Boolean,
    isAddToAlbumInProgress: Boolean,
    isResetButtonEnabled: Boolean,
    onTopAppBarNavigationIcon: () -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
    onReset: () -> Unit,
    onAddToAlbum: () -> Unit,
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
    PickerPhotosAndAlbums(
        title = { titleModifier ->
            Tabs(
                viewState = viewState,
                viewEvent = viewEvent,
                modifier = titleModifier,
            )
        },
        selectedTab = viewState.selectedTab,
        onTopAppBarNavigationIcon = onTopAppBarNavigationIcon,
        navigateToAlbum = navigateToAlbum,
        onReset = onReset,
        onAddToAlbum = onAddToAlbum,
        addToAlbumTitle = addToAlbumTitle,
        isAddToAlbumButtonEnabled = isAddToAlbumButtonEnabled,
        isAddToAlbumInProgress = isAddToAlbumInProgress,
        isResetButtonEnabled = isResetButtonEnabled,
        modifier = modifier
            .testTag(PickerPhotosAndAlbumsScreenTestTag.screen),
    )
}

@Composable
fun PickerPhotosAndAlbums(
    title: @Composable (Modifier) -> Unit,
    addToAlbumTitle: String,
    isAddToAlbumButtonEnabled: Boolean,
    isAddToAlbumInProgress: Boolean,
    isResetButtonEnabled: Boolean,
    selectedTab: Tab,
    onTopAppBarNavigationIcon: () -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
    onReset: () -> Unit,
    onAddToAlbum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TopAppBar(
            title = title,
            onNavigationIcon = onTopAppBarNavigationIcon,
        )
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            val homeScaffoldState = rememberHomeScaffoldState().apply {
                drawerGesturesEnabled.value = false
                bottomNavigationEnabled.value = false
            }
            when (selectedTab) {
                Tab.PHOTOS -> PhotosTab(
                    homeScaffoldState = homeScaffoldState,
                    navigateToPhotosPermissionRationale = {},
                    navigateToPhotosPreview = {},
                    navigateToPhotosOptions = {},
                    navigateToMultiplePhotosOptions = {},
                    navigateToSubscription = {},
                    navigateToPhotosIssues = {},
                    navigateToPhotosUpsell = {},
                    navigateToBackupSettings = {},
                    navigateToNotificationPermissionRationale = {},
                    defaultTitle = {},
                )

                Tab.ALBUMS -> AlbumsTab(
                    homeScaffoldState = homeScaffoldState,
                    navigateToCreateNewAlbum = {},
                    navigateToAlbum = navigateToAlbum,
                    defaultTitle = {},
                )
            }
            BottomActions(
                addToAlbumTitle = addToAlbumTitle,
                isAddToAlbumButtonEnabled = isAddToAlbumButtonEnabled,
                isAddToAlbumInProgress = isAddToAlbumInProgress,
                isResetButtonEnabled = isResetButtonEnabled,
                onReset = onReset,
                onAddToAlbum = onAddToAlbum,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .shadow(),
            )
        }
    }
}

@Composable
fun TopAppBar(
    title: @Composable (Modifier) -> Unit,
    onNavigationIcon: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseTopAppBar(
        navigationIcon = painterResource(CorePresentation.drawable.ic_proton_close),
        onNavigationIcon = onNavigationIcon,
        title = title,
        modifier = modifier.statusBarsPadding(),
    )
}

@Composable
fun BottomActions(
    addToAlbumTitle: String,
    isAddToAlbumButtonEnabled: Boolean,
    isAddToAlbumInProgress: Boolean,
    isResetButtonEnabled: Boolean,
    onReset: () -> Unit,
    onAddToAlbum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(all = ProtonDimens.DefaultSpacing)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(
            modifier = Modifier
                .testTag(PickerPhotosAndAlbumsScreenTestTag.resetButton)
                .border(1.dp, ProtonTheme.colors.separatorNorm, CircleShape)
                .background(ProtonTheme.colors.backgroundNorm, CircleShape),
            onClick = onReset,
            enabled = isResetButtonEnabled,
        ) {
            Icon(
                painter = painterResource(id = CorePresentation.drawable.ic_proton_close),
                contentDescription = null,
                modifier = modifier.padding(4.dp),
            )
        }
        Spacer(modifier = Modifier.size(32.dp))
        AddToAlbumButton(
            addToAlbumTitle = addToAlbumTitle,
            modifier = Modifier
                .testTag(PickerPhotosAndAlbumsScreenTestTag.addToAlbumButton)
                .widthIn(min = 200.dp),
            enabled = isAddToAlbumButtonEnabled,
            loading = isAddToAlbumInProgress,
            onClick = onAddToAlbum,
        )
    }
}

object PickerPhotosAndAlbumsScreenTestTag {
    const val screen = "picker photos and albums screen"
    const val resetButton = "reset button"
    const val addToAlbumButton = "add to album button"
}
