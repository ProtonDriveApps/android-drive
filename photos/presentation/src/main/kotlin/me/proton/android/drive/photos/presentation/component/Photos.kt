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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import me.proton.android.drive.photos.presentation.state.PhotosItem
import me.proton.android.drive.photos.presentation.viewevent.PhotosViewEvent
import me.proton.android.drive.photos.presentation.viewstate.EmptyPhotoTagState
import me.proton.android.drive.photos.presentation.viewstate.PhotosViewState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultHighlightNorm
import me.proton.core.drive.base.domain.entity.FastScrollAnchor
import me.proton.core.drive.base.presentation.component.IllustratedMessage
import me.proton.core.drive.base.presentation.component.list.ListEmpty
import me.proton.core.drive.base.presentation.effect.HandleListEffect
import me.proton.core.drive.base.presentation.effect.ListEffect
import me.proton.core.drive.base.presentation.extension.onContent
import me.proton.core.drive.base.presentation.extension.onEmpty
import me.proton.core.drive.base.presentation.extension.onError
import me.proton.core.drive.base.presentation.extension.onLoading
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N

@Composable
fun Photos(
    viewState: PhotosViewState,
    viewEvent: PhotosViewEvent,
    photos: Flow<PagingData<PhotosItem>>,
    listEffect: Flow<ListEffect>,
    driveLinksFlow: Flow<Map<LinkId, DriveLink>>,
    selectedPhotos: Set<LinkId>,
    getFastScrollAnchors: suspend (List<PhotosItem>, Int, Int) -> List<FastScrollAnchor>,
    modifier: Modifier = Modifier,
) {
    val items = photos.collectAsLazyPagingItems()
    LaunchedEffect(items) {
        snapshotFlow { items.loadState }
            .distinctUntilChanged()
            .collect { loadState ->
                viewEvent.onLoadState(loadState, items.itemCount)
            }
    }
    listEffect.HandleListEffect(items = items)
    if (viewState.showPhotoShareMigrationInProgress) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            PhotoShareMigrationInProgress(
                title = stringResource(I18N.string.photos_share_migration_in_progress_title),
                description = stringResource(I18N.string.photos_share_migration_in_progress_description),
                modifier = Modifier.padding(all = ProtonDimens.DefaultSpacing)
            )
        }
    } else {
        Column {
            if (viewState.shouldShowFilters) {
                PhotosFilter(viewState.filters, viewEvent.onFilterSelected)
            }
            if (viewState.backupStatusViewState == null) {
                PhotosLoading(modifier)
            } else {
                val localContext = LocalContext.current
                viewState.listContentState
                    .onEmpty { state ->
                        if (viewState.showEmptyList == true) {
                            val emptyPhotoTagState = viewState.emptyPhotoTagState
                            if (emptyPhotoTagState != null) {
                                EmptyPhotoTag(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f),
                                    emptyPhotoTagState = emptyPhotoTagState,
                                )
                            } else {
                                PhotosEmpty(
                                    state = state,
                                    viewState = viewState.backupStatusViewState,
                                    showPhotosStateBanner = viewState.showPhotosStateBanner,
                                    showPhotoShareMigrationNeededBanner = viewState.showPhotoShareMigrationNeededBanner,
                                    showStorageBanner = viewState.showStorageBanner,
                                    modifier = modifier,
                                    onEnable = viewEvent.onEnable,
                                    onPermissions = viewEvent.onPermissions,
                                    onRetry = viewEvent.onRetry,
                                    onResolve = viewEvent.onResolve,
                                    onGetStorage = viewEvent.onGetStorage,
                                    onResolveMissingFolder = viewEvent.onResolveMissingFolder,
                                    onChangeNetwork = viewEvent.onChangeNetwork,
                                    onIgnoreBackgroundRestrictions = {
                                        viewEvent.onIgnoreBackgroundRestrictions(localContext)
                                    },
                                    onDismissBackgroundRestrictions = viewEvent.onDismissBackgroundRestrictions,
                                    onStartPhotoShareMigration = viewEvent.onStartPhotoShareMigration,
                                )
                            }
                        } else {
                            PhotosEmptyWithBackupTurnedOff(
                                modifier = modifier,
                                onEnable = viewEvent.onEnable,
                            )
                        }
                    }
                    .onLoading { PhotosLoading(modifier.testTag(PhotosTestTag.loading)) }
                    .onError { state ->
                        val titleId = state.titleId
                        val imageResId = state.imageResId
                        if (imageResId != null && titleId != null) {
                            ListEmpty(
                                imageResId = imageResId,
                                titleResId = titleId,
                                descriptionResId = state.descriptionResId,
                                actionResId = null,
                                onAction = {},
                            )
                        } else {
                            PhotosError(
                                message = state.message,
                                actionResId = state.actionResId,
                                modifier = modifier,
                                onAction = viewEvent.onErrorAction,
                            )
                        }
                    }
                    .onContent {
                        PhotosContent(
                            items = items,
                            driveLinksFlow = driveLinksFlow,
                            viewState = viewState.backupStatusViewState,
                            showPhotosStateBanner = viewState.showPhotosStateBanner,
                            showPhotoShareMigrationNeededBanner = viewState.showPhotoShareMigrationNeededBanner,
                            showStorageBanner = viewState.showStorageBanner,
                            modifier = modifier.testTag(PhotosTestTag.content),
                            inMultiselect = viewState.inMultiselect,
                            isFastScrollEnabled = viewState.isFastScrollEnabled,
                            selectedPhotos = selectedPhotos,
                            onClick = viewEvent.onDriveLink,
                            onLongClick = viewEvent.onSelectDriveLink,
                            onEnable = viewEvent.onEnable,
                            onPermissions = viewEvent.onPermissions,
                            onRetry = viewEvent.onRetry,
                            onResolve = viewEvent.onResolve,
                            onScroll = viewEvent.onScroll,
                            onGetStorage = viewEvent.onGetStorage,
                            onResolveMissingFolder = viewEvent.onResolveMissingFolder,
                            onChangeNetwork = viewEvent.onChangeNetwork,
                            onIgnoreBackgroundRestrictions = {
                                viewEvent.onIgnoreBackgroundRestrictions(localContext)
                            },
                            onDismissBackgroundRestrictions = viewEvent.onDismissBackgroundRestrictions,
                            isRefreshEnabled = viewState.isRefreshEnabled,
                            isRefreshing = viewState.listContentState.isRefreshing,
                            onRefresh = viewEvent.onRefresh,
                            onStartPhotoShareMigration = viewEvent.onStartPhotoShareMigration,
                            getFastScrollAnchors = getFastScrollAnchors,
                        )
                    }
            }
        }
    }
}

@Composable
private fun EmptyPhotoTag(
    emptyPhotoTagState: EmptyPhotoTagState,
    modifier: Modifier,
) {
    IllustratedMessage(
        modifier = modifier,
        imageContent = {
            Icon(
                modifier = Modifier.size(ProtonDimens.DefaultIconWithPadding),
                painter = painterResource(id = emptyPhotoTagState.state.imageResId),
                tint = if (emptyPhotoTagState.photoTag == PhotoTag.Favorites) {
                    ProtonTheme.colors.notificationError
                } else {
                    ProtonTheme.colors.iconWeak
                },
                contentDescription = null,
            )
        },
        titleResId = emptyPhotoTagState.state.titleId,
        descriptionResId = emptyPhotoTagState.state.descriptionResId,
    )
}

@Composable
fun PhotoShareMigrationInProgress(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(BasePresentation.raw.file_transfer_animation)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    IllustratedMessage(
        imageContent = {
            LottieAnimation(
                composition = composition,
                progress = { progress },
            )
        },
        titleContent = {
            Text(
                text = title,
                style = ProtonTheme.typography.defaultHighlightNorm,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    top = ProtonDimens.LargerSpacing,
                )
            )
        },
        description = description,
        modifier = modifier,
    )
}

object PhotosTestTag {
    const val content = "photos tab content"
    const val loading = "photos tab loading"
}
