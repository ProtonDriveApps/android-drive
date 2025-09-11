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

package me.proton.android.drive.photos.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow
import me.proton.android.drive.photos.presentation.extension.rememberLazyGridState
import me.proton.android.drive.photos.presentation.state.PhotosItem
import me.proton.android.drive.photos.presentation.viewstate.PhotosStatusViewState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.drive.base.domain.entity.FastScrollAnchor
import me.proton.core.drive.base.presentation.component.FastScroller
import me.proton.core.drive.base.presentation.component.ProtonPullToRefresh
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.user.presentation.quota.component.StorageBanner
import me.proton.core.drive.base.presentation.R as BasePresentation

@Composable
fun PhotosContent(
    items: LazyPagingItems<PhotosItem>,
    driveLinksFlow: Flow<Map<LinkId, DriveLink>>,
    selectedPhotos: Set<LinkId>,
    viewState: PhotosStatusViewState,
    showPhotosStateBanner: Boolean,
    showStorageBanner: Boolean,
    modifier: Modifier = Modifier,
    inMultiselect: Boolean = false,
    isFastScrollEnabled: Boolean = false,
    onClick: (DriveLink) -> Unit,
    onLongClick: (DriveLink) -> Unit,
    onEnable: () -> Unit,
    onPermissions: () -> Unit,
    onRetry: () -> Unit,
    onResolve: () -> Unit,
    onScroll: (Int, Set<LinkId>) -> Unit,
    onGetStorage: () -> Unit,
    onResolveMissingFolder: () -> Unit,
    onChangeNetwork: () -> Unit,
    onIgnoreBackgroundRestrictions: () -> Unit,
    onDismissBackgroundRestrictions: () -> Unit,
    isRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    getFastScrollAnchors: suspend (List<PhotosItem>, Int, Int) -> List<FastScrollAnchor>,
) {
    ProtonPullToRefresh(
        isPullToRefreshEnabled = isRefreshEnabled,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        PhotosContent(
            items = items,
            driveLinksFlow = driveLinksFlow,
            selectedPhotos = selectedPhotos,
            viewState = viewState,
            showPhotosStateBanner = showPhotosStateBanner,
            showStorageBanner = showStorageBanner,
            modifier = modifier,
            inMultiselect= inMultiselect,
            isFastScrollEnabled = isFastScrollEnabled,
            onClick = onClick,
            onLongClick = onLongClick,
            onEnable = onEnable,
            onPermissions = onPermissions,
            onRetry = onRetry,
            onResolve = onResolve,
            onScroll = onScroll,
            onGetStorage = onGetStorage,
            onResolveMissingFolder = onResolveMissingFolder,
            onChangeNetwork = onChangeNetwork,
            onIgnoreBackgroundRestrictions = onIgnoreBackgroundRestrictions,
            onDismissBackgroundRestrictions = onDismissBackgroundRestrictions,
            getFastScrollAnchors = getFastScrollAnchors,
        )
    }
}

@Composable
fun PhotosContent(
    items: LazyPagingItems<PhotosItem>,
    driveLinksFlow: Flow<Map<LinkId, DriveLink>>,
    selectedPhotos: Set<LinkId>,
    viewState: PhotosStatusViewState,
    showPhotosStateBanner: Boolean,
    showStorageBanner: Boolean,
    modifier: Modifier = Modifier,
    inMultiselect: Boolean = false,
    isFastScrollEnabled: Boolean = false,
    onClick: (DriveLink) -> Unit,
    onLongClick: (DriveLink) -> Unit,
    onEnable: () -> Unit,
    onPermissions: () -> Unit,
    onRetry: () -> Unit,
    onResolve: () -> Unit,
    onScroll: (Int, Set<LinkId>) -> Unit,
    onGetStorage: () -> Unit,
    onResolveMissingFolder: () -> Unit,
    onChangeNetwork: () -> Unit,
    onIgnoreBackgroundRestrictions: () -> Unit,
    onDismissBackgroundRestrictions: () -> Unit,
    getFastScrollAnchors: suspend (List<PhotosItem>, Int, Int) -> List<FastScrollAnchor>,
) {
    val gridState = items.rememberLazyGridState()
    val driveLinksMap by driveLinksFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
    val firstVisibleItemIndex by remember(gridState) { derivedStateOf { gridState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleItemIndex, items.itemSnapshotList.items) {
        onScroll(
            firstVisibleItemIndex,
            items.itemSnapshotList.items
                .takeIf { list -> list.isNotEmpty() && list.size > firstVisibleItemIndex }
                ?.let { list ->
                    val sizeRange = IntRange(0, list.size - 1)
                    val fromIndex = (firstVisibleItemIndex - 10).coerceIn(sizeRange)
                    val toIndex = (firstVisibleItemIndex + 20).coerceIn(sizeRange)
                    list.subList(fromIndex, toIndex + 1)
                        .filterIsInstance<PhotosItem.PhotoListing>()
                        .map { photoListing -> photoListing.id }
                        .toSet()
                } ?: emptySet(),
        )
    }


    var sizeInDp by remember { mutableStateOf(DpSize.Zero) }
    val density = LocalDensity.current
    val isThumbVisible = remember { mutableStateOf(false) }

    Box {
        LazyVerticalGrid(
            modifier = modifier.fillMaxSize(),
            columns = PhotosGridCells(minSize = 128.dp, minCount = 3),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            state = gridState,
            contentPadding = PaddingValues(bottom = sizeInDp.height)
        ) {
            items(
                count = items.itemCount,
                span = { index ->
                    when (items[index]) {
                        is PhotosItem.Separator -> GridItemSpan(maxLineSpan)
                        else -> GridItemSpan(1)
                    }
                },
                key = items.itemKey { photoItem ->
                    when (photoItem) {
                        is PhotosItem.Separator -> photoItem.value
                        is PhotosItem.PhotoListing -> photoItem.id.id
                    }
                }
            ) { index ->
                items[index]?.let { item ->
                    when (item) {
                        is PhotosItem.Separator -> Text(
                            modifier = Modifier.padding(
                                top = ProtonDimens.MediumSpacing,
                                bottom = ProtonDimens.SmallSpacing,
                                start = ProtonDimens.DefaultSpacing,
                                end = ProtonDimens.DefaultSpacing,
                            ),
                            text = item.value,
                            style = ProtonTheme.typography.defaultWeak,
                        )

                        is PhotosItem.PhotoListing -> {
                            val selected = selectedPhotos.contains(item.id)
                            MediaItem(
                                modifier = Modifier
                                    .clip(ProtonTheme.shapes.small),
                                link = driveLinksMap[item.id],
                                index = index,
                                isSelected = selected,
                                inMultiselect = selected || selectedPhotos.isNotEmpty() || inMultiselect,
                                onClick = onClick,
                                onLongClick = onLongClick,
                            )
                        }
                    }
                }
            }
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "footer",
            ) {
                EncryptedFooter(
                    Modifier
                        .fillMaxWidth()
                        .padding(ProtonDimens.DefaultSpacing)
                )
            }
        }

        AnimatedVisibility(
            visible = !isThumbVisible.value,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(BottomCenter),
        ) {
            PhotosBanners(
                modifier = Modifier
                    .onSizeChanged { size ->
                        sizeInDp = density.run {
                            DpSize(size.width.toDp(), size.height.toDp())
                        }
                    }
            ) {
                PhotosStatesContainer(
                    viewState = viewState,
                    showPhotosStateBanner = showPhotosStateBanner,
                    onEnable = onEnable,
                    onPermissions = onPermissions,
                    onRetry = onRetry,
                    onResolve = onResolve,
                    onResolveMissingFolder = onResolveMissingFolder,
                    onChangeNetwork = onChangeNetwork,
                    onIgnoreBackgroundRestrictions = onIgnoreBackgroundRestrictions,
                    onDismissBackgroundRestrictions = onDismissBackgroundRestrictions,
                )
                StorageBanner(
                    isVisible = showStorageBanner,
                    onGetStorage = onGetStorage,
                )
            }
        }
        val haptic = LocalHapticFeedback.current
        FastScroller(
            state = gridState,
            itemCount = items.itemCount,
            isThumbVisible = isThumbVisible,
            isFastScrollEnabled = isFastScrollEnabled,
            thumbContent = {
                Box(
                    modifier = Modifier
                        .size(48.dp),
                    contentAlignment = CenterEnd,
                ) {
                    Card(
                        shape = RoundedCornerShape(100.dp),
                        elevation = ProtonDimens.SmallSpacing,
                        backgroundColor = ProtonTheme.colors.backgroundSecondary,
                        contentColor = ProtonTheme.colors.textNorm,
                        modifier = Modifier
                            .size(width = 32.dp, height = 37.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(
                                    BasePresentation.drawable.ic_proton_chevron_up_down_16
                                ),
                                contentDescription = null,
                                tint = ProtonTheme.colors.iconNorm,
                            )
                        }
                    }
                }
            },
            onDraggedToPosition = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            getFastScrollAnchors = { maxSteps, stepsForLabel ->
                getFastScrollAnchors(items.itemSnapshotList.items, maxSteps, stepsForLabel)
            },
        )
    }
}
