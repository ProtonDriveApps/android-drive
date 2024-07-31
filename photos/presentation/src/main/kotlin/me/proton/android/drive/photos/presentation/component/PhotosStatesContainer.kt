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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import me.proton.android.drive.photos.presentation.viewstate.PhotosStatusViewState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupErrorType

@Composable
internal fun PhotosStatesContainer(
    viewState: PhotosStatusViewState,
    showPhotosStateBanner: Boolean,
    modifier: Modifier = Modifier,
    onEnable: () -> Unit,
    onPermissions: () -> Unit,
    onRetry: () -> Unit,
    onResolve: () -> Unit,
    onResolveMissingFolder: () -> Unit,
    onChangeNetwork: () -> Unit,
    onIgnoreBackgroundRestrictions: () -> Unit,
    onDismissBackgroundRestrictions: () -> Unit,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = showPhotosStateBanner,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing),
        ) {
            when (viewState) {
                is PhotosStatusViewState.Disabled -> if (viewState.hasDefaultFolder == false) {
                    BackupMissingFolderState(onMore = onResolveMissingFolder)
                } else {
                    BackupDisableState(onEnableBackup = onEnable)
                }
                is PhotosStatusViewState.Complete -> BackupCompletedState(extraLabel = viewState.labelItemSaved)
                is PhotosStatusViewState.Uncompleted -> BackupUncompletedState(onResolve = onResolve)
                is PhotosStatusViewState.Preparing -> BackupPreparingState(preparing = viewState)
                is PhotosStatusViewState.InProgress -> BackupInProgressState(inProgress = viewState)
                is PhotosStatusViewState.Failed -> {
                    viewState.errors.forEach { error ->
                        when (error.type) {
                            BackupErrorType.PERMISSION -> MissingPermissionsState(onPermissions = onPermissions)
                            BackupErrorType.LOCAL_STORAGE -> LocalStorageState(onRetry = onRetry)
                            BackupErrorType.DRIVE_STORAGE -> BackupFailedState(onRetry = onRetry)
                            BackupErrorType.OTHER -> BackupFailedState(onRetry = onRetry)
                            BackupErrorType.CONNECTIVITY -> NoConnectivityState()
                            BackupErrorType.WIFI_CONNECTIVITY -> WaitingConnectivityState(
                                onChangeNetwork = onChangeNetwork
                            )

                            BackupErrorType.PHOTOS_UPLOAD_NOT_ALLOWED ->
                                BackupTemporarilyDisabledState(onRetry = onRetry)

                            BackupErrorType.BACKGROUND_RESTRICTIONS ->
                                BackgroundRestrictions(
                                    onIgnoreBackgroundRestrictions = onIgnoreBackgroundRestrictions,
                                    onDismissBackgroundRestrictions = onDismissBackgroundRestrictions,
                                )
                        }
                    }
                }
            }
        }
    }
}
@Preview
@Composable
fun PhotosStatesContainerPreview(
    @PreviewParameter(ViewStatePreviewParameterProvider::class) viewState: PhotosStatusViewState,
) {
    ProtonTheme {
        Surface {
            PhotosStatesContainer(
                modifier = Modifier.background(ProtonTheme.colors.backgroundNorm),
                viewState = viewState,
                showPhotosStateBanner = true,
                onEnable = { },
                onPermissions = { },
                onRetry = { },
                onResolve = { },
                onResolveMissingFolder = { },
                onChangeNetwork = { },
                onIgnoreBackgroundRestrictions = { },
                onDismissBackgroundRestrictions = { },
            )
        }
    }
}

class ViewStatePreviewParameterProvider : CollectionPreviewParameterProvider<PhotosStatusViewState>(
    listOf(
        PhotosStatusViewState.Disabled(hasDefaultFolder = true),
        PhotosStatusViewState.Disabled(hasDefaultFolder = false),
        PhotosStatusViewState.Complete("1 000 items saved"),
        PhotosStatusViewState.Uncompleted,
        PhotosStatusViewState.InProgress(0.1F, "X items left"),
        PhotosStatusViewState.Failed(listOf(BackupError.Other())),
        PhotosStatusViewState.Failed(listOf(BackupError.LocalStorage())),
        PhotosStatusViewState.Failed(listOf(BackupError.DriveStorage())),
        PhotosStatusViewState.Failed(listOf(BackupError.Permissions())),
        PhotosStatusViewState.Failed(listOf(BackupError.Connectivity())),
        PhotosStatusViewState.Failed(listOf(BackupError.WifiConnectivity())),
        PhotosStatusViewState.Failed(listOf(BackupError.PhotosUploadNotAllowed())),
        PhotosStatusViewState.Failed(listOf(BackupError.BackgroundRestrictions())),
        PhotosStatusViewState.Preparing(0.1F),
    )
)
