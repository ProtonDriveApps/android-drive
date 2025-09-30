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

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.headlineSmallUnspecified
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.component.Deferred
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.ExpandableLazyVerticalGrid
import me.proton.core.drive.base.presentation.component.IllustratedMessage
import me.proton.core.drive.base.presentation.component.LARGE_HEIGHT
import me.proton.core.drive.base.presentation.component.ProtonIconTextButton
import me.proton.core.drive.base.presentation.component.ProtonPullToRefresh
import me.proton.core.drive.base.presentation.component.ThemelessStatusBarScreen
import me.proton.core.drive.base.presentation.component.Title
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.base.presentation.component.TopBarActions
import me.proton.core.drive.base.presentation.component.defaultMinMaxGridHeight
import me.proton.core.drive.base.presentation.component.list.ListError
import me.proton.core.drive.base.presentation.component.rememberExpandableLazyVerticalGridState
import me.proton.core.drive.base.presentation.effect.HandleListEffect
import me.proton.core.drive.base.presentation.effect.ListEffect
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.onContent
import me.proton.core.drive.base.presentation.extension.onEmpty
import me.proton.core.drive.base.presentation.extension.onError
import me.proton.core.drive.base.presentation.extension.onLoading
import me.proton.core.drive.base.presentation.extension.protonDriveCustomGreenButtonColors
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserViewState
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.core.drive.files.presentation.extension.driveLinkSemantics
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun Album(
    viewState: AlbumViewState,
    viewEvent: AlbumViewEvent,
    items: LazyPagingItems<PhotosItem.PhotoListing>,
    listEffect: Flow<ListEffect>,
    driveLinksFlow: Flow<Map<LinkId, DriveLink>>,
    selectedPhotos: Set<LinkId>,
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
            selectedPhotos = selectedPhotos,
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
    selectedPhotos: Set<LinkId>,
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
        isNameEncrypted = viewState.isNameEncrypted,
        details = viewState.details,
        navigationIcon = viewState.navigationIconResId,
        title = viewState.title,
        coverLinkId = viewState.coverLinkId,
        items = items,
        driveLinksMap = driveLinksMap,
        selectedPhotos = selectedPhotos,
        state = state,
        listContentState = viewState.listContentState,
        inMultiselect = viewState.inMultiselect,
        showActions = viewState.showActions,
        showAddAction = viewState.showAddAction,
        addActionEnabled = viewState.addActionEnabled,
        showSaveAllAction = viewState.showSaveAllAction,
        saveAllActionEnabled = viewState.saveAllActionEnabled,
        saveAllActionLoading = viewState.saveAllActionLoading,
        showShareAction = viewState.showShareAction,
        shareUsers = viewState.shareUsers,
        shareActionEnabled = viewState.shareActionEnabled,
        actionFlow = viewState.topBarActions,
        onTopAppBarNavigation = viewEvent.onTopAppBarNavigation,
        onScroll = viewEvent.onScroll,
        onErrorAction = viewEvent.onErrorAction,
        onClick = viewEvent.onDriveLink,
        onLongClick = viewEvent.onSelectDriveLink,
        onAddToAlbum = viewEvent.onAddToAlbum,
        onSaveAll = viewEvent.onSaveAll,
        onShare = viewEvent.onShare,
        onShareUsers = viewEvent.onShareUsers,
        modifier = modifier,
    )
}

@Composable
fun Album(
    name: String,
    isNameEncrypted: Boolean,
    details: String,
    navigationIcon: Int?,
    title: String?,
    coverLinkId: FileId?,
    items: LazyPagingItems<PhotosItem.PhotoListing>,
    driveLinksMap: Map<LinkId, DriveLink>,
    selectedPhotos: Set<LinkId>,
    state: MutableState<ExpandableLazyVerticalGrid.State>,
    listContentState: ListContentState,
    inMultiselect: Boolean,
    showActions: Boolean,
    showAddAction: Boolean,
    addActionEnabled: Boolean,
    showSaveAllAction: Boolean,
    saveAllActionEnabled: Boolean,
    saveAllActionLoading: Boolean,
    showShareAction: Boolean,
    shareUsers: List<ShareUserViewState>,
    shareActionEnabled: Boolean,
    actionFlow: Flow<Set<Action>>,
    onTopAppBarNavigation: () -> Unit,
    onScroll: (Set<LinkId>) -> Unit,
    onErrorAction: () -> Unit,
    onClick: (DriveLink) -> Unit,
    onLongClick: (DriveLink) -> Unit,
    onAddToAlbum: () -> Unit,
    onSaveAll: () -> Unit,
    onShare: () -> Unit,
    onShareUsers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = items.rememberLazyGridState()
    val firstVisibleItemIndex by remember(gridState) { derivedStateOf { gridState.firstVisibleItemIndex } }
    LaunchedEffect(items.itemSnapshotList.items, firstVisibleItemIndex) {
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
        isNameEncrypted = isNameEncrypted,
        details = details,
        navigationIcon = navigationIcon,
        title = title,
        coverLinkId = coverLinkId,
        items = items,
        driveLinksMap = driveLinksMap,
        selectedPhotos = selectedPhotos,
        state = state,
        listContentState = listContentState,
        gridState = gridState,
        inMultiselect = inMultiselect,
        showActions = showActions,
        showAddAction = showAddAction,
        addActionEnabled = addActionEnabled,
        onSaveAll = onSaveAll,
        showSaveAllAction = showSaveAllAction,
        saveAllActionEnabled = saveAllActionEnabled,
        saveAllActionLoading = saveAllActionLoading,
        showShareAction = showShareAction,
        shareUsers = shareUsers,
        shareActionEnabled = shareActionEnabled,
        actionFlow = actionFlow,
        onTopAppBarNavigation = onTopAppBarNavigation,
        onErrorAction = onErrorAction,
        onClick = onClick,
        onLongClick = onLongClick,
        onAddToAlbum = onAddToAlbum,
        onShare = onShare,
        onShareUsers = onShareUsers,
        modifier = modifier,
    )
}

@Composable
fun Album(
    name: String,
    isNameEncrypted: Boolean,
    details: String,
    navigationIcon: Int?,
    title: String?,
    coverLinkId: FileId?,
    items: LazyPagingItems<PhotosItem.PhotoListing>,
    driveLinksMap: Map<LinkId, DriveLink>,
    selectedPhotos: Set<LinkId>,
    state: MutableState<ExpandableLazyVerticalGrid.State>,
    listContentState: ListContentState,
    gridState: LazyGridState,
    inMultiselect: Boolean,
    showActions: Boolean,
    showAddAction: Boolean,
    addActionEnabled: Boolean,
    showSaveAllAction: Boolean,
    saveAllActionEnabled: Boolean,
    saveAllActionLoading: Boolean,
    showShareAction: Boolean,
    shareUsers: List<ShareUserViewState>,
    shareActionEnabled: Boolean,
    actionFlow: Flow<Set<Action>>,
    onTopAppBarNavigation: () -> Unit,
    onErrorAction: () -> Unit,
    onClick: (DriveLink) -> Unit,
    onLongClick: (DriveLink) -> Unit,
    onAddToAlbum: () -> Unit,
    onSaveAll: () -> Unit,
    onShare: () -> Unit,
    onShareUsers: () -> Unit,
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
    val density = LocalDensity.current
    var gridHeaderHeight by remember { mutableStateOf(0.dp) }
    ExpandableLazyVerticalGrid(
        columns = PhotosGridCells(minSize = 128.dp, minCount = 3),
        minGridHeight = minGridHeight,
        maxGridHeight = maxGridHeight,
        contentPadding = PaddingValues(SmallSpacing),
        verticalArrangement = Arrangement.spacedBy(SmallSpacing),
        horizontalArrangement = Arrangement.spacedBy(SmallSpacing),
        state = state,
        gridState = gridState,
        lazyVerticalGridModifier = Modifier
            .fillMaxWidth()
            .background(
                ProtonTheme.colors.backgroundNorm,
                RoundedCornerShape(ProtonDimens.ExtraLargeCornerRadius)
            )
            .padding(all = SmallSpacing),
        topComposable = {
            AlbumTopBar(
                navigationIcon = navigationIcon,
                title = title,
                coverLink = coverLinkId?.let { driveLinksMap[coverLinkId] },
                topComposableHeight = topComposableHeight,
                onTopAppBarNavigation = onTopAppBarNavigation,
                actions = { TopBarActions(actionFlow = actionFlow, iconTintColor = Color.White) }
            )
        },
        modifier = modifier
            .fillMaxSize(),
    ) {
        item(
            span = { GridItemSpan(maxLineSpan) },
        ) {
            Column(
                modifier = Modifier
                    .onGloballyPositioned { layoutCoordinates ->
                        gridHeaderHeight = with(density) { layoutCoordinates.size.height.toDp() }
                    }
            ) {
                AlbumListHeader(
                    name = name,
                    isNameEncrypted = isNameEncrypted,
                    details = details,
                    shareUsers = shareUsers,
                    modifier = Modifier.padding(
                        vertical = SmallSpacing,
                        horizontal = ProtonDimens.ExtraSmallSpacing,
                    ),
                    onShareUsers = onShareUsers,
                )
                if (showActions) {
                    AlbumActions(
                        modifier = Modifier.padding(
                            vertical = ProtonDimens.DefaultSpacing,
                            horizontal = ProtonDimens.ExtraSmallSpacing,
                        ),
                        onAdd = onAddToAlbum,
                        showAdd = showAddAction,
                        addEnabled = addActionEnabled,
                        onSaveAll = onSaveAll,
                        showSaveAll = showSaveAllAction,
                        saveAllEnabled = saveAllActionEnabled,
                        saveAllLoading = saveAllActionLoading,
                        onShare = onShare,
                        showShare = showShareAction,
                        shareEnabled = shareActionEnabled,
                    )
                }
            }
        }
        listContentState
            .onLoading {
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    AlbumGridBoxItem(
                        minGridHeight = minGridHeight,
                        gridHeaderHeight = gridHeaderHeight,
                    ) {
                        Deferred {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
            .onEmpty { state ->
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    AlbumGridBoxItem(
                        minGridHeight = minGridHeight,
                        gridHeaderHeight = gridHeaderHeight,
                    ) {
                        AlbumEmpty(
                            imageResId = state.imageResId,
                            titleResId = state.titleId,
                            descriptionResId = state.descriptionResId,
                        )
                    }
                }
            }
            .onError { state ->
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    AlbumGridBoxItem(
                        minGridHeight = minGridHeight,
                        gridHeaderHeight = gridHeaderHeight,
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        AlbumError(
                            message = state.message,
                            actionResId = state.actionResId,
                            onAction = onErrorAction,
                        )
                    }
                }
            }
            .onContent {
                items(
                    count = items.itemCount,
                    span = { GridItemSpan(1) },
                    key = items.itemKey { photoItem -> photoItem.id.id }
                ) { index ->
                    items[index]?.let { item ->
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
                            onRenderThumbnail = {},
                        )
                    }
                }
            }
    }
}

@Composable
fun BoxScope.AlbumTopBar(
    @DrawableRes navigationIcon: Int?,
    title: String?,
    coverLink: DriveLink?,
    topComposableHeight: Dp,
    modifier: Modifier = Modifier,
    onTopAppBarNavigation: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    ThemelessStatusBarScreen(useDarkIcons = false)
    val driveLinkSemanticsModifier = if (coverLink != null) {
        Modifier.driveLinkSemantics(coverLink, LayoutType.Cover)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(topComposableHeight)
            .align(Alignment.TopCenter)
            .then(driveLinkSemanticsModifier)
    ) {
        val coverPainter = coverLink?.thumbnailPainter(usePhotoThumbnailVO = true)?.painter
        if (coverPainter != null) {
            Image(
                painter = coverPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = ProtonTheme.colors.backgroundDeep)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black.copy(alpha = 0.25f))
        )
        TopAppBar(
            navigationIcon = navigationIcon?.let { painterResource(navigationIcon) },
            onNavigationIcon = onTopAppBarNavigation,
            title = { modifier ->
                title?.let {
                    Title(
                        title = title,
                        isTitleEncrypted = false,
                        style = ProtonTheme.typography.headlineSmallUnspecified.copy(
                            color = Color.White
                        ),
                        modifier = modifier,
                    )
                }
            },
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
    isNameEncrypted: Boolean,
    details: String,
    shareUsers: List<ShareUserViewState>,
    modifier: Modifier = Modifier,
    onShareUsers: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SmallSpacing),
    ) {
        AlbumDetails(
            details = details,
            shareUsers = shareUsers,
            onShareUsers = onShareUsers,
        )
        AlbumName(name, isNameEncrypted)
    }
}

@Composable
fun AlbumName(
    name: String,
    isNameEncrypted: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isNameEncrypted) {
        EncryptedItem(modifier = modifier.height(LARGE_HEIGHT))
    } else {
        Text(
            text = name,
            modifier = modifier,
            style = ProtonTheme.typography.hero,
            color = ProtonTheme.colors.textNorm,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AlbumDetails(
    details: String,
    shareUsers: List<ShareUserViewState>,
    modifier: Modifier = Modifier,
    onShareUsers: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MediumSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SmallSpacing)
    ) {
        Text(
            text = details,
            style = ProtonTheme.typography.captionMedium,
            color = ProtonTheme.colors.textHint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        AnimatedVisibility(
            visible = shareUsers.isNotEmpty(),
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SharedUsers(
                users = shareUsers.map { shareUser -> shareUser.firstLetter },
                onClick = onShareUsers,
            )
        }
    }
}

@Composable
fun AlbumActions(
    onAdd: () -> Unit,
    onSaveAll: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    showAdd: Boolean = true,
    addEnabled: Boolean = true,
    showSaveAll: Boolean = true,
    saveAllEnabled: Boolean = true,
    saveAllLoading: Boolean = false,
    showShare: Boolean = true,
    shareEnabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showAdd) {
            ProtonIconTextButton(
                iconPainter = painterResource(CorePresentation.drawable.ic_proton_plus_circle_filled),
                title = stringResource(I18N.string.common_add_action),
                enabled = addEnabled,
                onClick = onAdd,
            )
        }
        if (showSaveAll) {
            ProtonIconTextButton(
                iconPainter = painterResource(BasePresentation.drawable.ic_cloud_arrow_down_filled),
                title = stringResource(I18N.string.common_save_all_action),
                enabled = saveAllEnabled,
                loading = saveAllLoading,
                onClick = onSaveAll,
            )
        }
        if (showShare) {
            ProtonIconTextButton(
                iconPainter = painterResource(BasePresentation.drawable.ic_user_plus_filled),
                title = stringResource(I18N.string.common_share),
                enabled = shareEnabled,
                colors = ButtonDefaults.protonDriveCustomGreenButtonColors(),
                onClick = onShare,
                modifier = Modifier.heightIn(min = 40.dp)
            )
        }
    }
}

@Composable
fun AlbumGridBoxItem(
    minGridHeight: Dp,
    gridHeaderHeight: Dp,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    if (minGridHeight > 0.dp && gridHeaderHeight > 0.dp) {
        Box(
            modifier = modifier
                .offset(y = (-24).dp)
                .heightIn(min = minGridHeight - gridHeaderHeight),
            contentAlignment = contentAlignment,
        ) {
            content()
        }
    }
}

@Composable
fun AlbumEmpty(
    imageResId: Int,
    titleResId: Int,
    descriptionResId: Int?,
    modifier: Modifier = Modifier,
) {
    IllustratedMessage(
        imageContent = {
            Image(
                painter = painterResource(imageResId),
                contentDescription = null,
            )
        },
        titleResId = titleResId,
        descriptionResId = descriptionResId,
        modifier = modifier,
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
