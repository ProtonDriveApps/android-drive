/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.files.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.CombinedLoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.presentation.component.ReportDrawnWhenLoaded
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.base.presentation.effect.ListEffect
import me.proton.core.drive.base.presentation.extension.SuppressContentFadeIn
import me.proton.core.drive.base.presentation.extension.fadeInOnInsert
import me.proton.core.drive.base.presentation.extension.onContent
import me.proton.core.drive.base.presentation.extension.onEmpty
import me.proton.core.drive.base.presentation.extension.onError
import me.proton.core.drive.base.presentation.extension.onLoading
import me.proton.core.drive.base.presentation.extension.withContentFadeInSuppressed
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.files.presentation.component.files.FilesGridContent
import me.proton.core.drive.files.presentation.component.files.FilesGridItem
import me.proton.core.drive.files.presentation.component.files.FilesListContent
import me.proton.core.drive.files.presentation.component.files.FilesListEmpty
import me.proton.core.drive.files.presentation.component.files.FilesListError
import me.proton.core.drive.files.presentation.component.files.FilesListFooter
import me.proton.core.drive.files.presentation.component.files.FilesListHeader
import me.proton.core.drive.files.presentation.component.files.FilesListItem
import me.proton.core.drive.files.presentation.component.files.FilesListLoading
import me.proton.core.drive.files.presentation.component.files.FilesSectionHeader
import me.proton.core.drive.files.presentation.component.files.rememberKeyGridRowsByPosition
import me.proton.core.drive.files.presentation.event.FilesViewEvent
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.core.drive.files.presentation.extension.driveLinkSemantics
import me.proton.core.drive.files.presentation.state.FilesViewState
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.presentation.compose.FilesUploadingListItem
import me.proton.core.drive.sorting.presentation.state.toSortingViewState
import kotlin.math.floor
import kotlin.math.roundToInt
import me.proton.core.drive.i18n.R as I18N

@Composable
fun Files(
    driveLinks: DriveLinksFlow,
    viewState: FilesViewState,
    viewEvent: FilesViewEvent,
    modifier: Modifier = Modifier,
    getTransferProgress: (DriveLink) -> Flow<Percentage>? = { null },
    uploadingFileLinks: Flow<List<UploadFileLink>> = emptyFlow(),
    showTopAppBar: Boolean = true,
    scrollToId: LinkId? = null,
    onScrollCompleted: () -> Unit = {},
    highlightedId: LinkId? = null,
    tabs: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val uploadFileLinkList by rememberFlowWithLifecycle(flow = uploadingFileLinks)
        .collectAsState(initial = emptyList())
    val lazyColumnItems = driveLinks.toLazyColumnItems(onLoadState = viewEvent.onLoadState)
    ReportDrawnWhenLoaded(isLoading = viewState.listContentState is ListContentState.Loading)
    Column(modifier) {
        if (showTopAppBar) {
            TopAppBar(
                viewState = viewState,
                viewEvent = viewEvent,
                actions = actions
            )
            tabs()
        }
        // Only fade after the user toggles the layout. Other changes, such as the stored layout
        // arriving after the first frame, switch instantly.
        var isLayoutToggledByUser by remember { mutableStateOf(false) }
        val layoutViewEvent = remember(viewEvent) {
            object : FilesViewEvent by viewEvent {
                override val onToggleLayout: () -> Unit = {
                    isLayoutToggledByUser = true
                    viewEvent.onToggleLayout()
                }
            }
        }
        LaunchedEffect(viewState.isGrid) { isLayoutToggledByUser = false }
        AnimatedContent(
            targetState = viewState.isGrid,
            transitionSpec = {
                val transform = if (isLayoutToggledByUser) {
                    fadeIn(tween()) togetherWith fadeOut(tween())
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
                transform using null
            },
            label = "FilesLayout",
        ) { isGrid ->
            if (isGrid) {
                lazyColumnItems.DisplayAsGrid(viewState, layoutViewEvent, uploadFileLinkList, getTransferProgress, scrollToId, onScrollCompleted, highlightedId)
            } else {
                lazyColumnItems.DisplayAsList(viewState, layoutViewEvent, uploadFileLinkList, getTransferProgress, scrollToId, onScrollCompleted, highlightedId)
            }
        }
    }
}

@Composable
fun TopAppBar(
    viewState: FilesViewState,
    viewEvent: FilesViewEvent,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        navigationIcon = if (viewState.navigationIconResId != 0) {
            painterResource(id = viewState.navigationIconResId)
        } else null,
        navigationContentDescription = viewState.navigationContentDescription,
        notificationDotVisible = viewState.notificationDotVisible,
        onNavigationIcon = viewEvent.onTopAppBarNavigation,
        title = viewState.title ?: stringResource(id = viewState.titleResId),
        isTitleEncrypted = viewState.isTitleEncrypted,
        actions = actions
    )
}

@Composable
private inline fun ListContent(
    viewState: FilesViewState,
    viewEvent: FilesViewEvent,
    uploadFileLinkList: List<UploadFileLink>,
    lazyListState: LazyListState,
    verticalArrangement : Arrangement.Vertical = Arrangement.Top,
    isContentFadeInSuppressed: Boolean = false,
    crossinline content: LazyListScope.() -> Unit,
) {
    val modifier = if (viewState.listContentState is ListContentState.Content) {
        Modifier.testTag(FilesTestTag.content)
    } else {
        Modifier
    }
    SuppressContentFadeIn(isSuppressed = isContentFadeInSuppressed) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            state = lazyListState,
            verticalArrangement = verticalArrangement,
        ) {
            itemsIndexed(
                items = uploadFileLinkList,
                key = { _, uploadFileLink -> uploadFileLink.id },
            ) { index, uploadFileLink ->
                UploadItem(viewState, viewEvent, index, uploadFileLink)
            }
            viewState.listContentState
                .onLoading(::FilesListLoading)
                .onEmpty { state ->
                    FilesListEmpty(
                        imageResId = state.imageResId,
                        titleResId = state.titleId,
                        descriptionResId = state.descriptionResId,
                        actionResId = state.actionResId,
                        onAction = viewEvent.onAddFiles
                    )
                }
                .onError { state ->
                    FilesListError(
                        message = state.message,
                        actionResId = state.actionResId,
                        onAction = viewEvent.onErrorAction,
                    )
                }
                .onContent {
                    if (viewState.showHeader) {
                        item {
                            FilesListHeader(
                                sortingViewState = viewState.sorting.toSortingViewState(),
                                isDisplayingGridView = viewState.isGrid,
                                onSorting = { viewEvent.onSorting(viewState.sorting) },
                                onToggleLayout = viewEvent.onToggleLayout,
                            )
                        }
                    }
                    content()
                    item {
                        FilesListFooter(
                            listContentAppendingState = viewState.listContentAppendingState,
                            onErrorAction = viewEvent.onErrorAction
                        )
                    }
                }
        }
    }
}

@Composable
private fun LazyColumnItems.DisplayAsGrid(
    viewState: FilesViewState,
    viewEvent: FilesViewEvent,
    uploadFileLinkList: List<UploadFileLink>,
    getTransferProgress: (DriveLink) -> Flow<Percentage>?,
    scrollToId: LinkId? = null,
    onScrollCompleted: () -> Unit = {},
    highlightedId: LinkId? = null,
) {
    val selectedDriveLinks by rememberFlowWithLifecycle(flow = viewState.selected)
        .collectAsState(initial = emptySet())
    val keyRowsByPosition = rememberKeyGridRowsByPosition()
    var isScrollingToItem by remember { mutableStateOf(false) }
    BoxWithConstraints {
        val itemsPerRow = floor(maxWidth / GridItemWidth).roundToInt().coerceAtLeast(1)
        val lazyListState = this@DisplayAsGrid.rememberLazyListState()
        ScrollToEffect(
            scrollToId = scrollToId,
            lazyListState = lazyListState,
            viewState = viewState,
            uploadFileLinkList = uploadFileLinkList,
            onScrollCompleted = onScrollCompleted,
            onScrollingToItem = { isScrolling -> isScrollingToItem = isScrolling },
            toLazyListIndex = { itemIndex -> itemIndex / itemsPerRow },
        )
        ListContent(
            viewState = viewState,
            viewEvent = viewEvent,
            uploadFileLinkList = uploadFileLinkList,
            lazyListState = lazyListState,
            verticalArrangement = Arrangement.spacedBy(ExtraSmallSpacing),
            isContentFadeInSuppressed = isScrollingToItem,
        ) {
            FilesGridContent(
                driveLinks = this@DisplayAsGrid,
                itemsPerRow = itemsPerRow,
                keyRowsByPosition = keyRowsByPosition,
            ) { driveLink: DriveLink ->
                val selected = selectedDriveLinks.contains(driveLink.id)
                FilesGridItem(
                    link = driveLink,
                    onClick = viewEvent.onDriveLink,
                    onLongClick = viewEvent.onSelectDriveLink,
                    onMoreOptionsClick = viewEvent.onMoreOptions,
                    isMoreOptionsEnabled = viewState.isDriveLinkMoreOptionsEnabled,
                    isSelectingDestination = viewState.isSelectingDestination,
                    isClickEnabled = viewState.isClickEnabled,
                    isTextEnabled = viewState.isTextEnabled,
                    transferProgressFlow = remember(driveLink.downloadState) { getTransferProgress(driveLink) },
                    modifier = Modifier
                        .weight(1F)
                        .driveLinkSemantics(driveLink, LayoutType.Grid, driveLink.id == highlightedId),
                    isSelected = selected,
                    inMultiselect = selected || selectedDriveLinks.isNotEmpty(),
                    isHighlighted = driveLink.id == highlightedId,
                    onRenderThumbnail = viewEvent.onRenderThumbnail,
                )
            }
        }
    }
}

@Composable
private fun LazyColumnItems.DisplayAsList(
    viewState: FilesViewState,
    viewEvent: FilesViewEvent,
    uploadFileLinkList: List<UploadFileLink>,
    getTransferProgress: (DriveLink) -> Flow<Percentage>?,
    scrollToId: LinkId? = null,
    onScrollCompleted: () -> Unit = {},
    highlightedId: LinkId? = null,
) {
    val selectedDriveLinks by rememberFlowWithLifecycle(flow = viewState.selected)
        .collectAsState(initial = emptySet())
    val lazyListState = this@DisplayAsList.rememberLazyListState()
    var isScrollingToItem by remember { mutableStateOf(false) }
    ScrollToEffect(
        scrollToId = scrollToId,
        lazyListState = lazyListState,
        viewState = viewState,
        uploadFileLinkList = uploadFileLinkList,
        onScrollCompleted = onScrollCompleted,
        onScrollingToItem = { isScrolling -> isScrollingToItem = isScrolling },
        toLazyListIndex = { itemIndex -> itemIndex },
    )
    ListContent(
        viewState = viewState,
        viewEvent = viewEvent,
        uploadFileLinkList = uploadFileLinkList,
        lazyListState = lazyListState,
        isContentFadeInSuppressed = isScrollingToItem,
    ) {
        FilesListContent(this@DisplayAsList) { driveLink: DriveLink ->
            val selected = selectedDriveLinks.contains(driveLink.id)
            FilesListItem(
                link = driveLink,
                onClick = viewEvent.onDriveLink,
                onLongClick = viewEvent.onSelectDriveLink,
                onMoreOptionsClick = viewEvent.onMoreOptions,
                isMoreOptionsEnabled = viewState.isDriveLinkMoreOptionsEnabled,
                isSelectingDestination = viewState.isSelectingDestination,
                isClickEnabled = viewState.isClickEnabled,
                isTextEnabled = viewState.isTextEnabled,
                transferProgressFlow = remember(driveLink.downloadState) { getTransferProgress(driveLink) },
                isSelected = selected,
                inMultiselect = selected || selectedDriveLinks.isNotEmpty(),
                isHighlighted = driveLink.id == highlightedId,
                modifier = fadeInOnInsert()
                    .driveLinkSemantics(driveLink, LayoutType.List, driveLink.id == highlightedId),
                onRenderThumbnail = viewEvent.onRenderThumbnail,
            )
        }
    }
}

@Composable
private fun LazyColumnItems.ScrollToEffect(
    scrollToId: LinkId?,
    lazyListState: LazyListState,
    viewState: FilesViewState,
    uploadFileLinkList: List<UploadFileLink>,
    onScrollCompleted: () -> Unit,
    onScrollingToItem: (Boolean) -> Unit,
    toLazyListIndex: (itemIndex: Int) -> Int,
) {
    LaunchedEffect(scrollToId) {
        scrollToId ?: return@LaunchedEffect
        snapshotFlow {
            when (this@ScrollToEffect) {
                is LazyColumnItems.PagingItems -> value.itemSnapshotList.items
                is LazyColumnItems.ListItems -> value
            }.indexOfFirst { driveLink -> driveLink.id == scrollToId }
        }
            .filter { itemIndex -> itemIndex >= 0 }
            .first()
            .let { itemIndex ->
                val uploadOffset = uploadFileLinkList.size
                val headerOffset = if (viewState.showHeader && viewState.listContentState is ListContentState.Content) 1 else 0
                withContentFadeInSuppressed(setSuppressed = onScrollingToItem) {
                    lazyListState.animateScrollToItem(toLazyListIndex(itemIndex) + uploadOffset + headerOffset)
                }
                onScrollCompleted()
            }
    }
}

@Composable
private fun LazyColumnItems.rememberLazyListState(): LazyListState {
    // After recreation, LazyPagingItems first return 0 items, then the cached items.
    // This behavior/issue is resetting the LazyListState scroll position.
    // Below is a workaround. More info: https://issuetracker.google.com/issues/177245496.
    return when (size) {
        // Return a different LazyListState instance.
        0 -> remember(this) { LazyListState(0, 0) }
        // Return rememberLazyListState (normal case).
        else -> androidx.compose.foundation.lazy.rememberLazyListState()
    }
}

@Composable
private fun UploadItem(
    viewState: FilesViewState,
    viewEvent: FilesViewEvent,
    index: Int,
    uploadFileLink: UploadFileLink,
) {
    val uploadProgress = remember(uploadFileLink) { viewState.getUploadProgress(uploadFileLink) }
        ?.let { uploadProgressFlow ->
            rememberFlowWithLifecycle(flow = uploadProgressFlow).collectAsState(initial = null)
        }
    if (index == 0) {
        FilesSectionHeader(titleResId = I18N.string.common_uploading)
    }
    FilesUploadingListItem(
        uploadFileLink = uploadFileLink,
        onCancelClick = { viewEvent.onCancelUpload(uploadFileLink) },
        uploadProgress = uploadProgress?.value
    )
}

@Composable
fun DriveLinksFlow.toLazyColumnItems(
    onLoadState: (CombinedLoadStates, Int) -> Unit,
) = when (this) {
    is DriveLinksFlow.NonPagingList -> {
        val driveNodeList by rememberFlowWithLifecycle(flow = value)
            .collectAsState(initial = emptyList())
        LazyColumnItems.ListItems(driveNodeList)
    }
    is DriveLinksFlow.PagingList -> {
        val driveNodePagingData = rememberFlowWithLifecycle(flow = value)
        val pagingData = driveNodePagingData.collectAsLazyPagingItems()
        LaunchedEffect(pagingData) {
            snapshotFlow { pagingData.loadState }
                .distinctUntilChanged()
                .collect { loadState ->
                    onLoadState(loadState, pagingData.itemCount)
                }
        }
        LaunchedEffect(LocalContext.current) {
            effect
                .onEach { effect ->
                    when (effect) {
                        ListEffect.REFRESH -> pagingData.apply { refresh() }
                        ListEffect.RETRY -> pagingData.apply { retry() }
                    }
                }
                .launchIn(this)
        }
        LazyColumnItems.PagingItems(pagingData)
    }
}

sealed class DriveLinksFlow {
    data class PagingList(
        val value: Flow<PagingData<DriveLink>>,
        val effect: Flow<ListEffect>,
    ) : DriveLinksFlow()

    data class NonPagingList(val value: Flow<List<DriveLink>>) : DriveLinksFlow()
}

sealed class LazyColumnItems {
    abstract val size: Int

    abstract operator fun get(index: Int): DriveLink?

    data class PagingItems(val value: LazyPagingItems<DriveLink>) : LazyColumnItems() {
        override val size: Int get() = value.itemCount
        override fun get(index: Int): DriveLink? = if (index < size) {
            value[index]
        } else {
            null
        }
    }

    data class ListItems(val value: List<DriveLink>) : LazyColumnItems() {
        override val size: Int get() = value.size
        override fun get(index: Int): DriveLink? = value.getOrNull(index)
    }
}

object FilesTestTag {
    const val content = "files content"
    const val gridDetailsTitle = "grid item"
    const val listDetailsTitle = "list item"
    const val multiSelectionTag = "multiSelectionTag"
    const val moreButton = "three dots button"
    const val itemWithSharedIcon = "item with shared icon"
    const val offlineIconDownloaded = "TestTag::Offline::Icon:Downloaded"
    const val offlineIconWaiting = "TestTag::Offline::Icon:Waiting"
    const val offlineDownloding = "TestTag::Offline::Downloading"
}

private val GridItemWidth = 174.dp
