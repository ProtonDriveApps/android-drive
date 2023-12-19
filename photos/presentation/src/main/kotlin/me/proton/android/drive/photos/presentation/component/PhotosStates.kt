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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.android.drive.photos.presentation.R
import me.proton.android.drive.photos.presentation.viewstate.PhotosStatusViewState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.compose.theme.defaultSmallStrongUnspecified
import me.proton.core.compose.theme.defaultSmallUnspecified
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun BackupCompletedState(
    extraLabel: String?,
    modifier: Modifier = Modifier,
) {
    BackupState(
        modifier = modifier,
        icon = CorePresentation.drawable.ic_proton_checkmark_circle,
        tint = ProtonTheme.colors.notificationSuccess,
        text = I18N.string.photos_backup_state_completed,
        secondaryText = extraLabel
    )
}

@Composable
fun BackupUncompletedState(
    onResolve: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackupState(
        modifier = modifier,
        icon = CorePresentation.drawable.ic_proton_exclamation_circle,
        tint = ProtonTheme.colors.notificationWarning,
        text = I18N.string.photos_backup_state_uncompleted,
        action = I18N.string.photos_backup_state_uncompleted_action,
        onAction = onResolve
    )
}

@Composable
fun BackupInProgressState(
    inProgress: PhotosStatusViewState.InProgress,
    modifier: Modifier = Modifier,
) {
    BackupInProgress { encrypting ->
        BackupInProgressState(inProgress, encrypting, modifier)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun BackupInProgressState(
    inProgress: PhotosStatusViewState.InProgress,
    encrypting: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        backgroundColor = ProtonTheme.colors.backgroundSecondary,
        contentColor = ProtonTheme.colors.textNorm,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .defaultMinSize(minHeight = ProtonDimens.ListItemHeight),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        horizontal = ProtonDimens.ListItemTextStartPadding,
                        vertical = ProtonDimens.ExtraSmallSpacing,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing),
            ) {
                AnimatedContent(
                    targetState = encrypting,
                    modifier = Modifier.size(ProtonDimens.DefaultIconSize),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220, delayMillis = 90)) with
                                fadeOut(animationSpec = tween(90))
                    }
                ) { encrypting ->
                    if (encrypting) {
                        Icon(
                            painter = painterResource(id = CorePresentation.drawable.ic_proton_lock),
                            contentDescription = null,
                            tint = ProtonTheme.colors.notificationSuccess,
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = CorePresentation.drawable.ic_proton_arrows_rotate),
                            contentDescription = null,
                            tint = ProtonTheme.colors.interactionNorm,
                        )
                    }
                }
                AnimatedContent(
                    targetState = encrypting,
                    modifier = Modifier.weight(1F),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220, delayMillis = 90)) with
                                fadeOut(animationSpec = tween(90))
                    },
                ) { encrypting ->
                    Text(
                        text = stringResource(
                            id = if (encrypting)
                                I18N.string.photos_backup_state_encrypting
                            else
                                I18N.string.photos_backup_state_uploading
                        ),
                        style = ProtonTheme.typography.defaultSmallUnspecified,
                    )
                }
                Text(
                    text = inProgress.labelItemsLeft,
                    style = ProtonTheme.typography.defaultSmallStrongUnspecified,
                    color = ProtonTheme.colors.textWeak
                )
            }
            val progressModifier = Modifier
                .padding(
                    start = ProtonDimens.ListItemTextStartPadding,
                    end = ProtonDimens.ListItemTextStartPadding,
                    bottom = ProtonDimens.SmallSpacing,
                )
                .height(6.dp)
                .clip(RoundedCornerShape(ProtonDimens.ExtraSmallSpacing))
                .fillMaxWidth()
            LinearProgressIndicator(
                modifier = progressModifier,
                color = ProtonTheme.colors.interactionNorm,
                backgroundColor = ProtonTheme.colors.interactionWeakNorm,
                progress = inProgress.progress,
            )
        }
    }
}

@Composable
fun MissingPermissionsState(
    modifier: Modifier = Modifier,
    onPermissions: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing)
    ) {
        BackupState(
            modifier = modifier,
            icon = CorePresentation.drawable.ic_proton_exclamation_circle,
            tint = ProtonTheme.colors.notificationError,
            text = I18N.string.photos_error_missing_permissions,
            action = I18N.string.photos_permission_rational_confirm_action,
            onAction = onPermissions,
        )
        ErrorDetails(
            stringResource(
                I18N.string.photos_error_missing_permissions_description,
                stringResource(id = I18N.string.app_name),
            )
        )
    }
}

@Composable
fun LocalStorageState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing)
    ) {
        BackupState(
            modifier = modifier,
            icon = CorePresentation.drawable.ic_proton_exclamation_circle_filled,
            tint = ProtonTheme.colors.notificationError,
            text = I18N.string.photos_error_local_storage,
            action = I18N.string.photos_error_local_storage_action,
            onAction = onRetry,
        )
        ErrorDetails(
            stringResource(
                I18N.string.photos_error_local_storage_description,
                stringResource(id = I18N.string.app_name),
            )
        )
    }
}

@Composable
private fun ErrorDetails(text: String) {
    Card(
        backgroundColor = ProtonTheme.colors.backgroundSecondary,
        contentColor = ProtonTheme.colors.textNorm,
        elevation = 0.dp,
    ) {
        Text(
            modifier = Modifier
                .defaultMinSize(minHeight = ProtonDimens.ListItemHeight)
                .padding(
                    horizontal = ProtonDimens.ListItemTextStartPadding,
                    vertical = ProtonDimens.SmallSpacing,
                ),
            text = text,
            style = ProtonTheme.typography.defaultSmallNorm,
        )
    }
}

@Composable
fun BackupDisableState(
    modifier: Modifier = Modifier,
    onEnableBackup: () -> Unit,
) {
    BackupState(
        modifier = modifier,
        icon = R.drawable.ic_proton_cloud_slash,
        tint = ProtonTheme.colors.iconWeak,
        text = I18N.string.photos_error_backup_disabled,
        action = I18N.string.photos_error_backup_disabled_action,
        onAction = onEnableBackup
    )
}

@Composable
fun BackupMissingFolderState(
    modifier: Modifier = Modifier,
    onMore: () -> Unit,
) {
    BackupState(
        modifier = modifier,
        icon = CorePresentation.drawable.ic_proton_exclamation_circle,
        tint = ProtonTheme.colors.notificationWarning,
        text = I18N.string.photos_error_backup_missing_folder,
        action = I18N.string.photos_error_backup_missing_folder_action,
        onAction = onMore
    )
}

@Composable
fun BackupFailedState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    BackupState(
        modifier = modifier,
        icon = CorePresentation.drawable.ic_proton_exclamation_circle,
        tint = ProtonTheme.colors.notificationError,
        text = I18N.string.photos_error_backup_failed,
        action = I18N.string.photos_error_backup_failed_action,
        onAction = onRetry
    )
}

@Composable
fun BackupTemporarilyDisabledState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    BackupState(
        modifier = modifier,
        icon = CorePresentation.drawable.ic_proton_exclamation_circle,
        tint = ProtonTheme.colors.notificationError,
        text = I18N.string.photos_error_backup_temporarily_disabled,
        action = I18N.string.photos_error_backup_failed_action,
        onAction = onRetry
    )
}

@Composable
fun WaitingConnectivityState(
    modifier: Modifier = Modifier,
) {
    BackupState(
        modifier = modifier,
        icon = R.drawable.ic_no_wifi,
        tint = ProtonTheme.colors.notificationError,
        text = I18N.string.photos_error_waiting_connectivity,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BackupState(
    modifier: Modifier = Modifier,
    icon: Int,
    tint: Color,
    text: Int,
    action: Int,
    onAction: () -> Unit,
) {
    Card(
        modifier = modifier,
        onClick = onAction,
        backgroundColor = ProtonTheme.colors.backgroundSecondary,
        contentColor = ProtonTheme.colors.textNorm,
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = ProtonDimens.ListItemHeight)
                .padding(
                    horizontal = ProtonDimens.ListItemTextStartPadding,
                    vertical = ProtonDimens.SmallSpacing,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing)
        ) {
            Icon(
                modifier = Modifier.size(ProtonDimens.DefaultIconSize),
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = tint,
            )
            Text(
                modifier = Modifier.weight(1F),
                text = stringResource(id = text),
                style = ProtonTheme.typography.defaultSmallUnspecified,
            )
            Text(
                text = stringResource(id = action),
                style = ProtonTheme.typography.defaultSmallStrongUnspecified,
                color = ProtonTheme.colors.interactionNorm,
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BackupState(
    modifier: Modifier = Modifier,
    icon: Int,
    tint: Color,
    text: Int,
    secondaryText: String? = null,
) {
    Card(
        modifier = modifier,
        backgroundColor = ProtonTheme.colors.backgroundSecondary,
        contentColor = ProtonTheme.colors.textNorm,
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = ProtonDimens.ListItemHeight)
                .padding(
                    horizontal = ProtonDimens.ListItemTextStartPadding,
                    vertical = ProtonDimens.SmallSpacing,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing)
        ) {
            Icon(
                modifier = Modifier.size(ProtonDimens.DefaultIconSize),
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = tint,
            )
            Text(
                modifier = Modifier.weight(1F),
                text = stringResource(id = text),
                style = ProtonTheme.typography.defaultSmallUnspecified,
            )
            if (secondaryText != null) {
                Text(
                    text = secondaryText,
                    style = ProtonTheme.typography.defaultSmallStrongUnspecified,
                    color = ProtonTheme.colors.textWeak,
                )
            }
        }
    }
}

@Preview
@Composable
private fun BackupCompletedStatePreview() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            BackupCompletedState("1 000 items saved")
        }
    }
}

@Preview
@Composable
private fun BackupUnompletedStatePreview() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            BackupUncompletedState(onResolve = {})
        }
    }
}

@Preview
@Composable
private fun EncryptingStatePreview() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            BackupInProgressState(
                inProgress = PhotosStatusViewState.InProgress(
                    progress = 0.3F,
                    labelItemsLeft = "12 345 items left",
                ),
                encrypting = true,
            )
        }
    }
}

@Preview
@Composable
private fun UploadingStatePreview() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            BackupInProgressState(
                inProgress = PhotosStatusViewState.InProgress(
                    progress = 0.9F,
                    labelItemsLeft = "1 item left",
                ),
                encrypting = false,
            )
        }
    }
}

@Preview
@Composable
private fun MissingPermissionsStatePreview() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            MissingPermissionsState(onPermissions = { })
        }
    }
}

@Preview
@Composable
private fun BackupDisableStatePreview() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            BackupDisableState(onEnableBackup = { })
        }
    }
}

@Preview
@Composable
private fun BackupMissingFolderStatePreview() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            BackupMissingFolderState(onMore = { })
        }
    }
}

@Preview
@Composable
private fun BackupFailedStatePreview() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            BackupFailedState(onRetry = { })
        }
    }
}

@Preview
@Composable
private fun LocalStorageStatePreview() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            LocalStorageState(onRetry = { })
        }
    }
}

@Preview
@Composable
private fun BackupTemporarilyDisabledStatePreview() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            BackupTemporarilyDisabledState(onRetry = { })
        }
    }
}
