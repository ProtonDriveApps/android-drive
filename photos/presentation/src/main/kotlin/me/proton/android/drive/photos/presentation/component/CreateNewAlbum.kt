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

package me.proton.android.drive.photos.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import me.proton.android.drive.photos.presentation.state.PhotosItem
import me.proton.android.drive.photos.presentation.viewevent.CreateNewAlbumViewEvent
import me.proton.android.drive.photos.presentation.viewstate.CreateNewAlbumViewState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.textNorm
import me.proton.core.drive.base.presentation.component.ProtonIconTextButton
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.presentation.R as CorePresentation
import me.proton.core.drive.i18n.R as I18N

@Composable
fun CreateNewAlbum(
    viewState: CreateNewAlbumViewState,
    viewEvent: CreateNewAlbumViewEvent,
    items: LazyPagingItems<PhotosItem.PhotoListing>,
    driveLinksFlow: Flow<Map<LinkId, DriveLink>>,
    modifier: Modifier = Modifier,
) {
    val albumName by viewState.name.collectAsStateWithLifecycle(
        initialValue = null
    )
    val driveLinksMap by driveLinksFlow.collectAsStateWithLifecycle(emptyMap())
    LaunchedEffect(items) {
        snapshotFlow { items.loadState }
            .distinctUntilChanged()
            .collect { loadState ->
                viewEvent.onLoadState(loadState, items.itemCount)
            }
    }
    albumName?.let { name ->
        CreateNewAlbum(
            name = name,
            hint = viewState.hint,
            items = items,
            driveLinksMap = driveLinksMap,
            isNameEnabled = viewState.isAlbumNameEnabled,
            isAddEnabled = viewState.isAddEnabled,
            isRemoveEnabled = viewState.isRemoveEnabled,
            modifier = modifier,
            onScroll = viewEvent.onScroll,
            onRemove = viewEvent.onRemove,
            onValueChanged = viewEvent.onNameChanged,
            onAdd = viewEvent.onAdd,
        )
    }
}

@Composable
fun CreateNewAlbum(
    name: String,
    items: LazyPagingItems<PhotosItem.PhotoListing>,
    driveLinksMap: Map<LinkId, DriveLink>,
    isNameEnabled: Boolean,
    isAddEnabled: Boolean,
    isRemoveEnabled: Boolean,
    modifier: Modifier = Modifier,
    hint: String? = null,
    onScroll: (Set<LinkId>) -> Unit,
    onRemove: (DriveLink.File) -> Unit,
    onValueChanged: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val state = remember(name) {
        mutableStateOf(
            TextFieldValue(
                text = name,
            )
        )
    }
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        AlbumName(
            textFieldValue = state.value,
            hint = hint,
            isEnabled = isNameEnabled,
        ) { textField ->
            if (textField.text != state.value.text) {
                onValueChanged(textField.text)
            }
            state.value = textField
        }
        Actions(
            modifier = Modifier
                .padding(
                    vertical = ProtonDimens.MediumSpacing,
                    horizontal = ProtonDimens.DefaultSpacing,
                ),
            onAdd = onAdd,
            addEnabled = isAddEnabled,
        )
        PhotosToAddToAlbum(
            items = items,
            driveLinksMap = driveLinksMap,
            isRemoveEnabled = isRemoveEnabled,
            onScroll = onScroll,
            onRemove = onRemove,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun AlbumName(
    textFieldValue: TextFieldValue,
    modifier: Modifier = Modifier,
    hint: String? = null,
    maxLines: Int = MaxLines,
    isEnabled: Boolean,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onValueChanged: (TextFieldValue) -> Unit,
) {
    TextField(
        value = textFieldValue,
        placeholder = {
            hint?.let {
                Text(
                    text = hint,
                    style = ProtonTheme.typography.hero,
                    color = ProtonTheme.colors.textHint,
                )
            }
        },
        onValueChange = onValueChanged,
        maxLines = maxLines,
        modifier = modifier
            .focusRequester(focusRequester),
        textStyle = ProtonTheme.typography.hero,
        colors = TextFieldDefaults.protonTextFieldColors(),
        enabled = isEnabled,
    )
}

@Composable
fun Actions(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    addEnabled: Boolean = true,
) {
    Row(
        modifier = modifier
    ) {
        ProtonIconTextButton(
            iconPainter = painterResource(CorePresentation.drawable.ic_proton_plus_circle_filled),
            title = stringResource(I18N.string.common_add_action),
            enabled = addEnabled,
            onClick = onAdd,
        )
    }
}

@Composable
fun PhotosToAddToAlbum(
    items: LazyPagingItems<PhotosItem.PhotoListing>,
    driveLinksMap: Map<LinkId, DriveLink>,
    isRemoveEnabled: Boolean,
    onScroll: (Set<LinkId>) -> Unit,
    onRemove: (DriveLink.File) -> Unit,
    modifier: Modifier = Modifier,

) {
    val gridState = rememberLazyGridState()
    val firstVisibleItemIndex by remember(gridState) { derivedStateOf { gridState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleItemIndex, items.itemSnapshotList.items) {
        onScroll(
            items.itemSnapshotList.items
                .takeIf { list -> list.isNotEmpty() && list.size > firstVisibleItemIndex }
                ?.let { list ->
                    val sizeRange = IntRange(0, list.size - 1)
                    val fromIndex = (firstVisibleItemIndex - 10).coerceIn(sizeRange)
                    val toIndex = (firstVisibleItemIndex + 20).coerceIn(sizeRange)
                    list.subList(fromIndex, toIndex + 1)
                        .map { photoListing -> photoListing.id }
                        .toSet()
                } ?: emptySet(),
        )
    }
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = PhotosGridCells(minSize = minCoverSize, minCount = 3),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        state = gridState,
    ) {
        items(
            count = items.itemCount,
            span = { _ -> GridItemSpan(1) },
            key = items.itemKey { photoItem -> photoItem.id.id },
        ) { index ->
            items[index]?.let { item ->
                AddToAlbumItem(
                    driveLink = driveLinksMap[item.id] as? DriveLink.File,
                    index = index,
                    isSelected = false,
                    inMultiselect = false,
                    isRemoveEnabled = isRemoveEnabled,
                    onClick = {},
                    onLongClick = {},
                    onRemove = onRemove,
                )
            }
        }
    }
}

@Composable
fun AddToAlbumItem(
    driveLink: DriveLink.File?,
    index: Int,
    isSelected: Boolean,
    inMultiselect: Boolean,
    isRemoveEnabled: Boolean,
    onClick: (DriveLink) -> Unit,
    onLongClick: (DriveLink) -> Unit,
    onRemove: (DriveLink.File) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        MediaItem(
            modifier = Modifier
                .padding(all = ProtonDimens.DefaultSpacing)
                .clip(ProtonTheme.shapes.small),
            link = driveLink,
            index = index,
            isSelected = isSelected,
            inMultiselect = inMultiselect,
            onClick = onClick,
            onLongClick = onLongClick,
            onRenderThumbnail = {},
        )
        driveLink?.let {
            IconButton(
                modifier = Modifier
                    .testTag(CreateNewAlbumTestTag.removePhotoButton)
                    .offset(x = (8).dp, y = (-8).dp)
                    .clip(shape = CircleShape)
                    .background(Color.Transparent)
                    .align(Alignment.TopEnd),
                onClick = { onRemove(driveLink) },
                enabled = isRemoveEnabled,
            ) {
                RemoveIcon()
            }
        }
    }
}

@Composable
fun RemoveIcon(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ProtonDimens.DefaultIconSize)
            .clip(CircleShape)
            .background(ProtonTheme.colors.backgroundNorm)
    ) {
        Box(
            modifier = Modifier
                .size(ProtonDimens.SmallIconSize)
                .clip(CircleShape)
                .background(Color.White)
                .align(Alignment.Center)
        )
        Icon(
            painter = painterResource(id = CorePresentation.drawable.ic_proton_cross_circle_filled),
            contentDescription = null,
            tint = ProtonTheme.colors.interactionNorm,
            modifier = Modifier
                .align(Alignment.Center)
        )
    }
}

@Composable
fun TextFieldDefaults.protonTextFieldColors(): TextFieldColors =
    textFieldColors(
        textColor = ProtonTheme.colors.textNorm,
        disabledTextColor = ProtonTheme.colors.textNorm(enabled = false),
        backgroundColor = Color.Transparent,
        focusedLabelColor = Color.Transparent,
        unfocusedLabelColor = Color.Transparent,
        disabledLabelColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        placeholderColor = ProtonTheme.colors.textHint,
        errorLabelColor = ProtonTheme.colors.notificationError,
    )

private const val MaxLines = 4

@Preview
@Composable
private fun EmptyAlbumNamePreview() {
    ProtonTheme {
        AlbumName(
            textFieldValue = TextFieldValue(
                text = "",
            ),
            hint = stringResource(I18N.string.albums_new_album_name_hint),
            isEnabled = true,
            onValueChanged = { _ -> },
        )
    }
}

@Preview
@Composable
private fun MyAlbumNamePreview() {
    ProtonTheme {
        AlbumName(
            textFieldValue = TextFieldValue(
                text = "My album",
            ),
            hint = stringResource(I18N.string.albums_new_album_name_hint),
            isEnabled = true,
            onValueChanged = { _ -> },
        )
    }
}

object CreateNewAlbumTestTag {
    val removePhotoButton = "remove photo button"
}
