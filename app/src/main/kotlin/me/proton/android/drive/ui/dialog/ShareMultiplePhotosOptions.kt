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

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.photos.presentation.component.AlbumListItem
import me.proton.android.drive.photos.presentation.component.AlbumOptionsSection
import me.proton.android.drive.photos.presentation.state.AlbumsItem
import me.proton.android.drive.ui.viewevent.ShareMultiplePhotosOptionsViewEvent
import me.proton.android.drive.ui.viewmodel.ShareMultiplePhotosOptionsViewModel
import me.proton.android.drive.ui.viewstate.ShareMultiplePhotosOptionsViewState
import me.proton.core.drive.base.presentation.component.BottomSheetEntry
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.files.presentation.entry.OptionEntry
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.LinkId

@Composable
fun ShareMultiplePhotosOptions(
    runAction: RunAction,
    modifier: Modifier = Modifier,
    navigateToCreateNewAlbum: () -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
) {
    val viewModel = hiltViewModel<ShareMultiplePhotosOptionsViewModel>()
    val viewState = viewModel.initialViewState
    val viewEvent = remember {
        viewModel.viewEvent(
            runAction = runAction,
            navigateToCreateNewAlbum = navigateToCreateNewAlbum,
            navigateToAlbum = navigateToAlbum,
        )
    }
    ShareMultiplePhotosOptions(
        viewState = viewState,
        viewEvent = viewEvent,
        modifier = modifier
            .testTag(ShareMultiplePhotosOptionsTestTag.screen)
            .navigationBarsPadding(),
    )
}

@Composable
fun ShareMultiplePhotosOptions(
    viewState: ShareMultiplePhotosOptionsViewState,
    viewEvent: ShareMultiplePhotosOptionsViewEvent,
    modifier: Modifier = Modifier,
) {
    val sharedAlbums by viewState.sharedAlbums.collectAsStateWithLifecycle(emptyList())
    ShareMultiplePhotosOptions(
        shareOptionsSectionTitle = stringResource(viewState.shareOptionsSectionTitleResId),
        shareOptions = viewState.shareOptions,
        sharedAlbumsSectionTitle = stringResource(viewState.sharedAlbumsSectionTitleResId),
        sharedAlbums = sharedAlbums,
        modifier = modifier,
        onSharedAlbum = viewEvent.onSharedAlbum,
        onScroll = viewEvent.onScroll,
    )
}

@Composable
fun ShareMultiplePhotosOptions(
    shareOptionsSectionTitle: String,
    shareOptions: List<OptionEntry<Unit>>,
    sharedAlbumsSectionTitle: String,
    sharedAlbums: List<AlbumsItem.Listing>,
    modifier: Modifier = Modifier,
    onSharedAlbum: (AlbumId) -> Unit,
    onScroll: (Set<LinkId>) -> Unit,
) {
    val listState = rememberLazyListState()
    val firstVisibleItemIndex by remember(
        listState
    ) { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleItemIndex, sharedAlbums) {
        onScroll(
            sharedAlbums
                .takeIf { list -> list.isNotEmpty() && list.size > firstVisibleItemIndex }
                ?.let { list ->
                    val sizeRange = IntRange(0, list.size - 1)
                    val fromIndex = (firstVisibleItemIndex - 10).coerceIn(sizeRange)
                    val toIndex = (firstVisibleItemIndex + 20).coerceIn(sizeRange)
                    list.subList(fromIndex, toIndex + 1)
                        .flatMap { albumListing ->
                            listOfNotNull(albumListing.id, albumListing.album?.coverLinkId)
                        }
                        .toSet()
                } ?: emptySet(),
        )
    }
    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        item(
            key = shareOptionsSectionTitle
        ) {
            AlbumOptionsSection(
                title = shareOptionsSectionTitle
            )
        }
        items(
            count = shareOptions.size,
            key = { shareOptions[it].label },
        ) {
            val option = shareOptions[it]
            BottomSheetEntry(
                leadingIcon = option.icon,
                trailingIcon = null,
                title = stringResource(option.label),
                onClick = { option.onClick(Unit) },
            )
        }
        if (sharedAlbums.isNotEmpty()) {
            item(
                key = sharedAlbumsSectionTitle
            ) {
                AlbumOptionsSection(
                    title = sharedAlbumsSectionTitle
                )
            }
            items(
                count = sharedAlbums.size,
                key = { "${sharedAlbums[it].id.shareId.id}.${sharedAlbums[it].id.id}" },
            ) {
                val albumsItem = sharedAlbums[it]
                AlbumListItem(
                    albumsItem = albumsItem,
                    onClick = onSharedAlbum,
                )
            }
        }
    }
}

object ShareMultiplePhotosOptionsTestTag {
    const val screen = "share multiple photos options screen"
}
