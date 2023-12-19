/*
 * Copyright (c) 2023 Proton AG.
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
                is PhotosStatusViewState.InProgress -> BackupInProgressState(inProgress = viewState)
                is PhotosStatusViewState.Failed -> {
                    viewState.errors.forEach { error ->
                        when (error.type) {
                            BackupErrorType.PERMISSION -> MissingPermissionsState(onPermissions = onPermissions)
                            BackupErrorType.LOCAL_STORAGE -> LocalStorageState(onRetry = onRetry)
                            BackupErrorType.DRIVE_STORAGE -> BackupFailedState(onRetry = onRetry)
                            BackupErrorType.OTHER -> BackupFailedState(onRetry = onRetry)
                            BackupErrorType.CONNECTIVITY -> WaitingConnectivityState()
                            BackupErrorType.PHOTOS_UPLOAD_NOT_ALLOWED ->
                                BackupTemporarilyDisabledState(onRetry = onRetry)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PhotosStatesContainerDisablePreview() {
    ProtonTheme {
        PhotosStatesContainer(
            modifier = Modifier.background(ProtonTheme.colors.backgroundNorm),
            viewState = PhotosStatusViewState.Disabled(true),
            showPhotosStateBanner = true,
            onEnable = { },
            onPermissions = { },
            onRetry = { },
            onResolve = { },
            onResolveMissingFolder = { },
        )
    }
}

@Preview
@Composable
fun PhotosStatesContainerMissingFolderPreview() {
    ProtonTheme {
        PhotosStatesContainer(
            modifier = Modifier.background(ProtonTheme.colors.backgroundNorm),
            viewState = PhotosStatusViewState.Disabled(false),
            showPhotosStateBanner = true,
            onEnable = { },
            onPermissions = { },
            onRetry = { },
            onResolve = { },
            onResolveMissingFolder = { },
        )
    }
}

@Preview
@Composable
fun PhotosStatesContainerCompletePreview() {
    ProtonTheme {
        PhotosStatesContainer(
            modifier = Modifier.background(ProtonTheme.colors.backgroundNorm),
            viewState = PhotosStatusViewState.Complete("1 000 items saved"),
            showPhotosStateBanner = true,
            onEnable = { },
            onPermissions = { },
            onRetry = { },
            onResolve = { },
            onResolveMissingFolder = { },
        )
    }
}

@Preview
@Composable
fun PhotosStatesContainerUncompletedPreview() {
    ProtonTheme {
        PhotosStatesContainer(
            modifier = Modifier.background(ProtonTheme.colors.backgroundNorm),
            viewState = PhotosStatusViewState.Uncompleted,
            showPhotosStateBanner = true,
            onEnable = { },
            onPermissions = { },
            onRetry = { },
            onResolve = { },
            onResolveMissingFolder = { },
        )
    }
}

@Preview
@Composable
fun PhotosStatesContainerInProgressPreview() {
    ProtonTheme {
        PhotosStatesContainer(
            modifier = Modifier.background(ProtonTheme.colors.backgroundNorm),
            viewState = PhotosStatusViewState.InProgress(
                0.1F,
                "X items left"
            ),
            showPhotosStateBanner = true,
            onEnable = { },
            onPermissions = { },
            onRetry = { },
            onResolve = { },
            onResolveMissingFolder = { },
        )
    }
}

@Preview
@Composable
fun PhotosStatesContainerFailedPreview() {
    ProtonTheme {
        PhotosStatesContainer(
            modifier = Modifier.background(ProtonTheme.colors.backgroundNorm),
            viewState = PhotosStatusViewState.Failed(
                errors = listOf(BackupError.Permissions())
            ),
            showPhotosStateBanner = true,
            onEnable = { },
            onPermissions = { },
            onRetry = { },
            onResolve = { },
            onResolveMissingFolder = { },
        )
    }
}
