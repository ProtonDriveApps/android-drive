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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import me.proton.android.drive.photos.presentation.component.CreateNewAlbum
import me.proton.android.drive.photos.presentation.state.PhotosItem
import me.proton.android.drive.photos.presentation.viewevent.CreateNewAlbumViewEvent
import me.proton.android.drive.photos.presentation.viewstate.CreateNewAlbumViewState
import me.proton.android.drive.ui.viewmodel.CreateNewAlbumViewModel
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.component.protonTextButtonColors
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.headlineSmallNorm
import me.proton.core.compose.theme.interactionNorm
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun CreateNewAlbumScreen(
    navigateBack: () -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
    navigateToPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<CreateNewAlbumViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(
        viewModel.initialViewState
    )
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val viewEvent = remember(lifecycle) {
        viewModel.viewEvent(
            navigateBack = navigateBack,
            navigateToAlbum = navigateToAlbum,
            navigateToPicker = navigateToPicker,
        )
    }
    val items = viewModel.photos.collectAsLazyPagingItems()
    CreateNewAlbumScreen(
        viewState = viewState,
        viewEvent = viewEvent,
        items = items,
        driveLinksFlow = viewModel.driveLinksMap,
        modifier = modifier,
    )
}

@Composable
fun CreateNewAlbumScreen(
    viewState: CreateNewAlbumViewState,
    viewEvent: CreateNewAlbumViewEvent,
    items: LazyPagingItems<PhotosItem.PhotoListing>,
    driveLinksFlow: Flow<Map<LinkId, DriveLink>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        TopAppBar(
            viewState = viewState,
            viewEvent = viewEvent,
        )
        CreateNewAlbum(
            viewState = viewState,
            viewEvent = viewEvent,
            items = items,
            driveLinksFlow = driveLinksFlow,
        )
    }
}

@Composable
fun TopAppBar(
    viewState: CreateNewAlbumViewState,
    viewEvent: CreateNewAlbumViewEvent,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    TopAppBar(
        navigationIcon = painterResource(id = CorePresentation.drawable.ic_arrow_back),
        onNavigationIcon = viewEvent.onBackPressed,
        title = "",
        modifier = modifier.statusBarsPadding(),
        actions = {
            ProtonTextButton(
                onClick = {
                    focusManager.clearFocus()
                    viewEvent.onDone()
                },
                enabled = viewState.isDoneEnabled,
                loading = viewState.isCreationInProgress,
                colors = ButtonDefaults.protonTextButtonColors(
                    backgroundColor = Color.Transparent
                )
            ) {
                Text(
                    text = stringResource(id = I18N.string.common_done_action),
                    style = ProtonTheme.typography.headlineSmallNorm,
                    color = ProtonTheme.colors.interactionNorm(viewState.isDoneEnabled),
                )
            }
        }
    )
}

@Preview
@Composable
private fun CreateNewAlbumScreenPreview() {
    ProtonTheme {
        CreateNewAlbumScreen(
            viewState = CreateNewAlbumViewState(
                isDoneEnabled = false,
                isCreationInProgress = true,
                isAlbumNameEnabled = true,
                isAddEnabled = true,
                isRemoveEnabled = true,
                name = emptyFlow(),
                hint = stringResource(I18N.string.albums_new_album_name_hint),
            ),
            viewEvent = object : CreateNewAlbumViewEvent {
                override val onLoadState: (CombinedLoadStates, Int) -> Unit = { _, _ -> }
            },
            items = flowOf(PagingData.empty<PhotosItem.PhotoListing>()).collectAsLazyPagingItems(),
            driveLinksFlow = emptyFlow()
        )
    }
}
