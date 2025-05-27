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

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.photos.presentation.component.AlbumListItem
import me.proton.android.drive.photos.presentation.state.AlbumsItem
import me.proton.android.drive.ui.viewevent.AddToAlbumsOptionsViewEvent
import me.proton.android.drive.ui.viewmodel.AddToAlbumsOptionsViewModel
import me.proton.android.drive.ui.viewstate.AddToAlbumsOptionsViewState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.component.BottomSheetEntry
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.files.presentation.entry.OptionEntry
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.LinkId

@Composable
fun AddToAlbumsOptions(
    runAction: RunAction,
    modifier: Modifier = Modifier,
    navigateToCreateNewAlbum: () -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
) {
    val viewModel = hiltViewModel<AddToAlbumsOptionsViewModel>()
    val viewState = viewModel.initialViewState
    val viewEvent = remember {
        viewModel.viewEvent(
            runAction = runAction,
            navigateToCreateNewAlbum = navigateToCreateNewAlbum,
            navigateToAlbum = navigateToAlbum,
        )
    }
    AddToAlbumsOptions(
        viewState = viewState,
        viewEvent = viewEvent,
        modifier = modifier
            .testTag(AddToAlbumsOptionsTestTag.screen)
            .navigationBarsPadding(),
    )
}

@Composable
fun AddToAlbumsOptions(
    viewState: AddToAlbumsOptionsViewState,
    viewEvent: AddToAlbumsOptionsViewEvent,
    modifier: Modifier = Modifier,
) {
    val albums by viewState.albums.collectAsStateWithLifecycle(emptyList())
    AddToAlbumsOptions(
        options = viewState.options,
        albums = albums,
        modifier = modifier,
        onAlbum = viewEvent.onAlbum,
        onScroll = viewEvent.onScroll,
    )
}

@Composable
fun AddToAlbumsOptions(
    options: List<OptionEntry<Unit>>,
    albums: List<AlbumsItem.Listing>,
    modifier: Modifier = Modifier,
    onAlbum: (AlbumId) -> Unit,
    onScroll: (Set<LinkId>) -> Unit,
) {
    val listState = rememberLazyListState()
    val firstVisibleItemIndex by remember(
        listState
    ) { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleItemIndex, albums) {
        onScroll(
            albums
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
        items(
            count = options.size,
            key = { options[it].label },
        ) {
            val option = options[it]
            BottomSheetEntry(
                leadingIcon = option.icon,
                trailingIcon = null,
                title = stringResource(option.label),
                onClick = { option.onClick(Unit) },
            )
        }
        if (albums.isNotEmpty()) {
            item(
                key = "divider"
            ) {
                Divider(
                    color = ProtonTheme.colors.separatorNorm,
                    modifier = Modifier.padding(bottom = ProtonDimens.SmallSpacing)
                )
            }
            items(
                count = albums.size,
                key = { "${albums[it].id.shareId.id}.${albums[it].id.id}" },
            ) {
                val albumsItem = albums[it]
                AlbumListItem(
                    albumsItem = albumsItem,
                    onClick = onAlbum,
                )
            }
        }
    }
}

object AddToAlbumsOptionsTestTag {
    const val screen = "add to albums options screen"
}
