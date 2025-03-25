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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import me.proton.android.drive.photos.presentation.extension.rememberLazyGridState
import me.proton.android.drive.photos.presentation.extension.thumbnailPainter
import me.proton.android.drive.photos.presentation.state.PhotosItem
import me.proton.android.drive.photos.presentation.viewevent.AlbumViewEvent
import me.proton.android.drive.photos.presentation.viewstate.AlbumViewState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.component.Deferred
import me.proton.core.drive.base.presentation.component.ExpandableLazyVerticalGrid
import me.proton.core.drive.base.presentation.component.IllustratedMessage
import me.proton.core.drive.base.presentation.component.ProtonPullToRefresh
import me.proton.core.drive.base.presentation.component.ThemelessStatusBarScreen
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.base.presentation.component.TopBarActions
import me.proton.core.drive.base.presentation.component.defaultMinMaxGridHeight
import me.proton.core.drive.base.presentation.component.list.ListError
import me.proton.core.drive.base.presentation.component.rememberExpandableLazyVerticalGridState
import me.proton.core.drive.base.presentation.effect.HandleListEffect
import me.proton.core.drive.base.presentation.effect.ListEffect
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

@Composable
fun Album(
    viewState: AlbumViewState,
    viewEvent: AlbumViewEvent,
    items: LazyPagingItems<PhotosItem.PhotoListing>,
    listEffect: Flow<ListEffect>,
    driveLinksFlow: Flow<Map<LinkId, DriveLink>>,
    isRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberExpandableLazyVerticalGridState()
    ProtonPullToRefresh(
        isPullToRefreshEnabled = isRefreshEnabled && state.value == ExpandableLazyVerticalGrid.State.COLLAPSED,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        Album(
            viewState = viewState,
            viewEvent = viewEvent,
            items = items,
            listEffect = listEffect,
            driveLinksFlow = driveLinksFlow,
            state = state,
            modifier = modifier,
        )
    }
}

@Composable
fun Album(
    viewState: AlbumViewState,
    viewEvent: AlbumViewEvent,
    items: LazyPagingItems<PhotosItem.PhotoListing>,
    listEffect: Flow<ListEffect>,
    driveLinksFlow: Flow<Map<LinkId, DriveLink>>,
    state: MutableState<ExpandableLazyVerticalGrid.State>,
    modifier: Modifier = Modifier,
) {
    val driveLinksMap by driveLinksFlow.collectAsStateWithLifecycle(emptyMap())
    LaunchedEffect(items) {
        snapshotFlow { items.loadState }
            .distinctUntilChanged()
            .collect { loadState ->
                viewEvent.onLoadState(loadState, items.itemCount)
            }
    }
    listEffect.HandleListEffect(items = items)
    Album(
        name = viewState.name,
        details = viewState.details,
        coverLinkId = viewState.coverLinkId,
        items = items,
        driveLinksMap = driveLinksMap,
        state = state,
        listContentState = viewState.listContentState,
        actionFlow = viewState.topBarActions,
        onBack = viewEvent.onBackPressed,
        onScroll = viewEvent.onScroll,
        onErrorAction = viewEvent.onErrorAction,
        modifier = modifier,
    )
}

@Composable
fun Album(
    name: String,
    details: String,
    coverLinkId: FileId?,
    items: LazyPagingItems<PhotosItem.PhotoListing>,
    driveLinksMap: Map<LinkId, DriveLink>,
    state: MutableState<ExpandableLazyVerticalGrid.State>,
    listContentState: ListContentState,
    actionFlow: Flow<Set<Action>>,
    onBack: () -> Unit,
    onScroll: (Set<LinkId>) -> Unit,
    onErrorAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = items.rememberLazyGridState()
    val firstVisibleItemIndex by remember(gridState) { derivedStateOf { gridState.firstVisibleItemIndex } }
    LaunchedEffect(items.itemSnapshotList.items) {
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
    Album(
        name = name,
        details = details,
        coverLinkId = coverLinkId,
        items = items,
        driveLinksMap = driveLinksMap,
        state = state,
        listContentState = listContentState,
        gridState = gridState,
        actionFlow = actionFlow,
        onBack = onBack,
        onErrorAction = onErrorAction,
        modifier = modifier,
    )
}

@Composable
fun Album(
    name: String,
    details: String,
    coverLinkId: FileId?,
    items: LazyPagingItems<PhotosItem.PhotoListing>,
    driveLinksMap: Map<LinkId, DriveLink>,
    state: MutableState<ExpandableLazyVerticalGrid.State>,
    listContentState: ListContentState,
    gridState: LazyGridState,
    actionFlow: Flow<Set<Action>>,
    onBack: () -> Unit,
    onErrorAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (topComposableHeight, overlapThreshold) = if (isLandscape) {
        topComposableHeightLandscape to 50.dp
    } else {
        topComposableHeightPortrait to 75.dp
    }
    val (minGridHeight, maxGridHeight) = defaultMinMaxGridHeight(
        topComposableHeight = topComposableHeight,
        overlapThreshold = overlapThreshold,
    )
    ExpandableLazyVerticalGrid(
        columns = PhotosGridCells(minSize = 128.dp, minCount = 3),
        minGridHeight = minGridHeight,
        maxGridHeight = maxGridHeight,
        contentPadding = PaddingValues(ProtonDimens.SmallSpacing),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing),
        state = state,
        gridState = gridState,
        lazyVerticalGridModifier = Modifier
            .fillMaxWidth()
            .background(
                ProtonTheme.colors.backgroundNorm,
                RoundedCornerShape(ProtonDimens.ExtraLargeCornerRadius)
            )
            .padding(all = ProtonDimens.SmallSpacing),
        topComposable = {
            AlbumTopBar(
                coverLink = coverLinkId?.let { driveLinksMap[coverLinkId] },
                topComposableHeight = topComposableHeight,
                onBack = onBack,
                actions = { TopBarActions(actionFlow = actionFlow) }
            )
        },
        modifier = modifier
            .fillMaxSize(),
    ) {
        item(
            span = { GridItemSpan(maxLineSpan) },
        ) {
            AlbumListHeader(
                name = name,
                details = details,
                modifier = Modifier.padding(
                    vertical = ProtonDimens.DefaultSpacing,
                    horizontal = ProtonDimens.ExtraSmallSpacing,
                )
            )
        }
        when (listContentState) {
            ListContentState.Loading -> let {
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Deferred {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
            is ListContentState.Empty -> let {
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    AlbumEmpty(
                        imageResId = listContentState.imageResId,
                        titleResId = listContentState.titleId,
                        descriptionResId = listContentState.descriptionResId,
                    )
                }
            }
            is ListContentState.Error -> let {
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    AlbumError(
                        message = listContentState.message,
                        actionResId = listContentState.actionResId,
                        onAction = onErrorAction,

                    )
                }
            }
            is ListContentState.Content -> let {
                items(
                    count = items.itemCount,
                    span = { GridItemSpan(1) },
                    key = items.itemKey { photoItem -> photoItem.id.id }
                ) { index ->
                    items[index]?.let { item ->
                        val selected = false//selectedPhotos.contains(item.id)
                        MediaItem(
                            modifier = Modifier
                                .clip(ProtonTheme.shapes.small),
                            link = driveLinksMap[item.id],
                            index = index,
                            isSelected = selected,
                            inMultiselect = false,//selected || selectedPhotos.isNotEmpty(),
                            onClick = {}, //onClick,
                            onLongClick = {}, //onLongClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BoxScope.AlbumTopBar(
    coverLink: DriveLink?,
    topComposableHeight: Dp,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    ThemelessStatusBarScreen(useDarkIcons = false)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(topComposableHeight)
            .align(Alignment.TopCenter)
    ) {
        val coverPainter = coverLink?.thumbnailPainter(usePhotoThumbnailVO = true)?.painter
            ?: painterResource(id = BasePresentation.drawable.img_whats_new_public_sharing) //TODO: once available put proper no-cover illustration
        Image(
            painter = coverPainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black.copy(alpha = 0.25f))
        )
        TopAppBar(
            navigationIcon = painterResource(CorePresentation.drawable.ic_arrow_back),
            onNavigationIcon = onBack,
            title = {},
            backgroundColor = Color.Transparent,
            contentColor = Color.White,
            modifier = Modifier.statusBarsPadding(),
            actions = actions,
        )
    }
}

@Composable
fun AlbumListHeader(
    name: String,
    details: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        AlbumDetails(details)
        Spacer(modifier = Modifier.height(ProtonDimens.SmallSpacing))
        AlbumName(name)
    }
}

@Composable
fun AlbumName(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = name,
        modifier = modifier,
        style = ProtonTheme.typography.hero,
        color = ProtonTheme.colors.textNorm,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun AlbumDetails(
    details: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = details,
        modifier = modifier,
        style = ProtonTheme.typography.captionMedium,
        color = ProtonTheme.colors.textHint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun AlbumEmpty(
    imageResId: Int,
    titleResId: Int,
    descriptionResId: Int?,
    modifier: Modifier = Modifier,
) {
    IllustratedMessage(
        imageResId = imageResId,
        titleResId = titleResId,
        descriptionResId = descriptionResId,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun AlbumError(
    message: String,
    actionResId: Int?,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
) {
    ListError(
        message = message,
        actionResId = actionResId,
        onAction = onAction,
        modifier = modifier,
    )
}

private val topComposableHeightPortrait = 200.dp
private val topComposableHeightLandscape = 150.dp
