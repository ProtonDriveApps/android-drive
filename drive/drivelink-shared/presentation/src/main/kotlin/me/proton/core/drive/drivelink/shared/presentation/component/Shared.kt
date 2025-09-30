/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.drivelink.shared.presentation.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import me.proton.core.drive.base.presentation.component.ProtonPullToRefresh
import me.proton.core.drive.base.presentation.component.list.ListEmpty
import me.proton.core.drive.base.presentation.component.list.ListError
import me.proton.core.drive.base.presentation.component.list.ListLoading
import me.proton.core.drive.base.presentation.effect.HandleListEffect
import me.proton.core.drive.base.presentation.effect.ListEffect
import me.proton.core.drive.base.presentation.extension.onContent
import me.proton.core.drive.base.presentation.extension.onEmpty
import me.proton.core.drive.base.presentation.extension.onError
import me.proton.core.drive.base.presentation.extension.onLoading
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.shared.presentation.entity.SharedItem
import me.proton.core.drive.drivelink.shared.presentation.viewevent.SharedViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewstate.SharedViewState
import me.proton.core.drive.files.presentation.component.files.FilesListItem
import me.proton.core.drive.files.presentation.component.files.FilesListItemPlaceholder
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.core.drive.files.presentation.extension.driveLinkSemantics
import me.proton.core.drive.link.domain.entity.LinkId

@Composable
fun Shared(
    viewState: SharedViewState,
    viewEvent: SharedViewEvent,
    sharedItems: Flow<PagingData<SharedItem>>,
    listEffect: Flow<ListEffect>,
    driveLinksFlow: Flow<Map<String, DriveLink>>,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = {},
) {
    val items = sharedItems.collectAsLazyPagingItems()
    LaunchedEffect(items) {
        snapshotFlow { items.loadState }
            .distinctUntilChanged()
            .collect { loadState ->
                viewEvent.onLoadState(loadState, items.itemCount)
            }
    }
    listEffect.HandleListEffect(items = items)
    viewState.listContentState
        .onEmpty { state ->
            SharedEmpty(
                imageResId = state.imageResId,
                titleResInt = state.titleId,
                descriptionResId = state.descriptionResId,
                actionResId = null,
                isRefreshEnabled = viewState.isRefreshEnabled,
                isRefreshing = viewState.listContentState.isRefreshing,
                modifier = modifier,
                onAction = {},
                onRefresh = viewEvent.onRefresh,
                headerContent = headerContent,
            )
        }
        .onLoading {
            SharedLoading(
                modifier = modifier,
                headerContent = headerContent,
            )
        }
        .onError { state ->
            SharedError(
                message = state.message,
                actionResId = state.actionResId,
                isRefreshEnabled = viewState.isRefreshEnabled,
                isRefreshing = viewState.listContentState.isRefreshing,
                modifier = modifier,
                onAction = viewEvent.onErrorAction,
                onRefresh = viewEvent.onRefresh,
                headerContent = headerContent,
            )
        }
        .onContent {
            SharedContent(
                items = items,
                driveLinksFlow = driveLinksFlow,
                isRefreshEnabled = viewState.isRefreshEnabled,
                isRefreshing = viewState.listContentState.isRefreshing,
                modifier = modifier,
                onDriveLink = viewEvent.onDriveLink,
                onScroll = viewEvent.onScroll,
                onRefresh = viewEvent.onRefresh,
                onMoreOptions = viewEvent.onMoreOptions,
                onRenderThumbnail = viewEvent.onRenderThumbnail,
                headerContent = headerContent,
            )
        }
}

@Composable
private fun SharedContent(
    items: LazyPagingItems<SharedItem>,
    driveLinksFlow: Flow<Map<String, DriveLink>>,
    isRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    onDriveLink: (DriveLink) -> Unit,
    onScroll: (Set<LinkId>) -> Unit,
    onRefresh: () -> Unit,
    onMoreOptions: (DriveLink) -> Unit,
    onRenderThumbnail: (DriveLink) -> Unit,
    headerContent: @Composable () -> Unit = {},
) {
    ProtonPullToRefresh(
        isPullToRefreshEnabled = isRefreshEnabled,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        SharedContent(
            items = items,
            driveLinksFlow = driveLinksFlow,
            modifier = modifier,
            onDriveLink = onDriveLink,
            onScroll = onScroll,
            onMoreOptions = onMoreOptions,
            onRenderThumbnail = onRenderThumbnail,
            headerContent = headerContent
        )
    }
}

@Composable
private fun SharedContent(
    items: LazyPagingItems<SharedItem>,
    driveLinksFlow: Flow<Map<String, DriveLink>>,
    modifier: Modifier = Modifier,
    onDriveLink: (DriveLink) -> Unit,
    onScroll: (Set<LinkId>) -> Unit,
    onMoreOptions: (DriveLink) -> Unit,
    onRenderThumbnail: (DriveLink) -> Unit,
    headerContent: @Composable () -> Unit = {},
) {
    val state = items.rememberLazyListState()
    val driveLinksMap by driveLinksFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
    val firstVisibleItemIndex by remember(state) { derivedStateOf { state.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleItemIndex, items.itemSnapshotList.items) {
        onScroll(
            items.itemSnapshotList.items
                .takeIf { list -> list.isNotEmpty() && list.size > firstVisibleItemIndex }
                ?.let { list ->
                    val sizeRange = IntRange(0, list.size - 1)
                    val fromIndex = (firstVisibleItemIndex - 10).coerceIn(sizeRange)
                    val toIndex = (firstVisibleItemIndex + 20).coerceIn(sizeRange)
                    list.subList(fromIndex, toIndex + 1)
                        .filterIsInstance<SharedItem.Listing>()
                        .map { sharedItem -> sharedItem.linkId }
                        .toSet()
                } ?: emptySet(),
        )
    }
    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxSize()
    ) {
        item {
            headerContent()
        }
        items(
            count = items.itemCount,
            key = items.itemKey { sharedItem ->
                when (sharedItem) {
                    is SharedItem.Listing -> sharedItem.linkId.shareId.id + sharedItem.linkId.id
                }
            },
        ) { index ->
            items[index]?.let { sharedItem ->
                when (sharedItem) {
                    is SharedItem.Listing -> SharedItem(
                        link =  driveLinksMap[sharedItem.linkId.id],
                        onDriveLink = onDriveLink,
                        onMoreOptions = onMoreOptions,
                        onRenderThumbnail = onRenderThumbnail,
                    )
                }
            }
        }
    }
}

@JvmName("NullableSharedItem")
@Composable
private fun SharedItem(
    link: DriveLink?,
    modifier: Modifier = Modifier,
    onDriveLink: (DriveLink) -> Unit,
    onMoreOptions: (DriveLink) -> Unit,
    onRenderThumbnail: (DriveLink) -> Unit,
) {
    if (link == null) {
        SharedPlaceholderItem(modifier)
    } else {
        SharedItem(
            link = link,
            onDriveLink = onDriveLink,
            onMoreOptions = onMoreOptions,
            modifier = modifier,
            onRenderThumbnail = onRenderThumbnail,
        )
    }
}

@Composable
private fun SharedItem(
    link: DriveLink,
    modifier: Modifier = Modifier,
    onDriveLink: (DriveLink) -> Unit,
    onMoreOptions: (DriveLink) -> Unit,
    onRenderThumbnail: (DriveLink) -> Unit,
) {
    FilesListItem(
        link = link,
        modifier = modifier.driveLinkSemantics(link, LayoutType.List),
        onClick = onDriveLink,
        onLongClick = {},
        onMoreOptionsClick = onMoreOptions,
        isClickEnabled = { _ -> true },
        isTextEnabled = {_, -> true},
        onRenderThumbnail = onRenderThumbnail,
    )
}

@Composable
private fun SharedPlaceholderItem(
    modifier: Modifier = Modifier,
) {
    FilesListItemPlaceholder(modifier)
}

@Composable
private fun SharedEmpty(
    @DrawableRes imageResId: Int,
    @StringRes titleResInt: Int,
    @StringRes descriptionResId: Int?,
    @StringRes actionResId: Int?,
    isRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
    onRefresh: () -> Unit,
    headerContent: @Composable () -> Unit,
) {
    ProtonPullToRefresh(
        isPullToRefreshEnabled = isRefreshEnabled,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        Column {
            headerContent()
            ListEmpty(
                imageResId = imageResId,
                titleResId = titleResInt,
                descriptionResId = descriptionResId,
                actionResId = actionResId,
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun SharedLoading(
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = {},
) {
    Column {
        headerContent()
        ListLoading(modifier = modifier.fillMaxSize())
    }
}

@Composable
private fun SharedError(
    message: String,
    @StringRes actionResId: Int?,
    isRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
    onRefresh: () -> Unit,
    headerContent: @Composable () -> Unit = {},
) {
    ProtonPullToRefresh(
        isPullToRefreshEnabled = isRefreshEnabled,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        Column {
            headerContent()
            ListError(
                message = message,
                actionResId = actionResId,
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun<T : Any> LazyPagingItems<T>.rememberLazyListState(): LazyListState {
    // After recreation, LazyPagingItems first return 0 items, then the cached items.
    // This behavior/issue is resetting the LazyListState scroll position.
    // Below is a workaround. More info: https://issuetracker.google.com/issues/177245496.
    return when (itemCount) {
        // Return a different LazyListState instance.
        0 -> remember(this) { LazyListState(0, 0) }
        // Return rememberLazyListState (normal case).
        else -> androidx.compose.foundation.lazy.rememberLazyListState()
    }
}
