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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
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
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun BackupCompletedState(
    extraLabel: String?,
    modifier: Modifier = Modifier,
) {
    BackupStateCard(
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
    BackupStateCard(
        modifier = modifier,
        icon = CorePresentation.drawable.ic_proton_exclamation_circle,
        tint = ProtonTheme.colors.notificationWarning,
        text = I18N.string.photos_backup_state_uncompleted,
        action = I18N.string.photos_backup_state_uncompleted_action,
        onClick = onResolve
    )
}

@Composable
fun BackupPreparingState(
    preparing: PhotosStatusViewState.Preparing,
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
                Icon(
                    painter = painterResource(id = CorePresentation.drawable.ic_proton_hourglass),
                    contentDescription = null,
                    tint = ProtonTheme.colors.interactionNorm,
                )
                Text(
                    text = stringResource(
                        id = I18N.string.photos_backup_state_preparing
                    ),
                    style = ProtonTheme.typography.defaultSmallUnspecified,
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
                progress = preparing.progress,
            )
        }
    }
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
                        fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
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
                        fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
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
    BackupCard(
        modifier = modifier,
        onClick = onPermissions,
    ) {
        Column {
            BackupState(
                icon = CorePresentation.drawable.ic_proton_exclamation_circle,
                tint = ProtonTheme.colors.notificationError,
                text = I18N.string.photos_error_missing_permissions,
            )
            Divider(
                color = ProtonTheme.colors.separatorNorm,
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
            )
            ErrorDetails(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(
                    I18N.string.photos_error_missing_permissions_description,
                    stringResource(id = I18N.string.app_name),
                ),
                action = I18N.string.photos_permission_rational_confirm_action,
            )
        }
    }
}

@Composable
fun LocalStorageState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    BackupCard(
        modifier = modifier,
        onClick = onRetry,
    ) {
        Column {
            BackupStateCard(
                modifier = Modifier,
                icon = CorePresentation.drawable.ic_proton_exclamation_circle_filled,
                tint = ProtonTheme.colors.notificationError,
                text = I18N.string.photos_error_local_storage,
            )
            Divider(
                color = ProtonTheme.colors.separatorNorm,
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
            )
            ErrorDetails(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(
                    I18N.string.photos_error_local_storage_description,
                    stringResource(id = I18N.string.app_name),
                ),
                action = I18N.string.photos_error_local_storage_action,
            )
        }
    }
}

@Composable
private fun ErrorDetails(
    text: String,
    modifier: Modifier = Modifier,
    action: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .conditional(onClick != null) {
                clickable { onClick?.invoke() }
            }
            .padding(ProtonDimens.ListItemTextStartPadding),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing)
    ) {
        Text(
            text = text,
            style = ProtonTheme.typography.defaultSmallNorm,
        )
        if (action != null) {
            Text(
                modifier = Modifier.align(Alignment.End),
                text = stringResource(id = action),
                style = ProtonTheme.typography.defaultSmallStrongUnspecified,
                color = ProtonTheme.colors.interactionNorm,
            )
        }
    }
}

@Composable
fun BackupDisableState(
    modifier: Modifier = Modifier,
    onEnableBackup: () -> Unit,
) {
    BackupStateCard(
        modifier = modifier,
        icon = R.drawable.ic_proton_cloud_slash,
        tint = ProtonTheme.colors.iconWeak,
        text = I18N.string.photos_error_backup_disabled,
        action = I18N.string.photos_error_backup_disabled_action,
        onClick = onEnableBackup
    )
}

@Composable
fun BackupMissingFolderState(
    modifier: Modifier = Modifier,
    onMore: () -> Unit,
) {
    BackupStateCard(
        modifier = modifier,
        icon = CorePresentation.drawable.ic_proton_exclamation_circle,
        tint = ProtonTheme.colors.notificationWarning,
        text = I18N.string.photos_error_backup_missing_folder,
        action = I18N.string.photos_error_backup_missing_folder_action,
        onClick = onMore
    )
}

@Composable
fun BackupFailedState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    BackupStateCard(
        modifier = modifier,
        icon = CorePresentation.drawable.ic_proton_exclamation_circle,
        tint = ProtonTheme.colors.notificationError,
        text = I18N.string.photos_error_backup_failed,
        action = I18N.string.photos_error_backup_failed_action,
        onClick = onRetry
    )
}

@Composable
fun BackupMigrationState(
    modifier: Modifier = Modifier,
) {
    BackupCard(modifier = modifier) {
        Column {
            BackupStateCard(
                modifier = Modifier,
                icon = CorePresentation.drawable.ic_proton_exclamation_circle,
                tint = ProtonTheme.colors.notificationError,
                text = I18N.string.photos_error_backup_migration_title,
            )
            Divider(
                color = ProtonTheme.colors.separatorNorm,
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
            )
            ErrorDetails(
                text = stringResource(I18N.string.photos_error_backup_migration_description,),
                action = null,
            )
        }
    }
}

@Composable
fun BackgroundRestrictions(
    modifier: Modifier = Modifier,
    onIgnoreBackgroundRestrictions: () -> Unit,
    onDismissBackgroundRestrictions: () -> Unit,
) {
    BackupCard(modifier = modifier) {
        Column {
            BackupState(
                icon = CorePresentation.drawable.ic_proton_exclamation_circle,
                tint = ProtonTheme.colors.notificationWarning,
                text = I18N.string.photos_error_background_restrictions,
            ) {
                IconButton(modifier = Modifier.offset(x = ProtonDimens.ListItemTextStartPadding),
                    onClick = { onDismissBackgroundRestrictions() }) {
                    Icon(
                        painter = painterResource(id = CorePresentation.drawable.ic_proton_cross),
                        contentDescription = stringResource(id = I18N.string.common_close_action)
                    )
                }
            }
            Divider(
                color = ProtonTheme.colors.separatorNorm,
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
            )
            ErrorDetails(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(
                    I18N.string.photos_error_background_restrictions_description,
                    stringResource(id = I18N.string.app_name),
                ),
                action = I18N.string.photos_error_background_restrictions_action,
                onClick = onIgnoreBackgroundRestrictions,
            )
        }
    }
}

@Composable
fun BackupTemporarilyDisabledState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    BackupStateCard(
        modifier = modifier,
        icon = CorePresentation.drawable.ic_proton_exclamation_circle,
        tint = ProtonTheme.colors.notificationWarning,
        text = I18N.string.photos_error_backup_temporarily_disabled,
        action = I18N.string.photos_error_backup_failed_action,
        onClick = onRetry
    )
}

@Composable
fun WaitingConnectivityState(
    modifier: Modifier = Modifier,
    onChangeNetwork: () -> Unit,
) {
    BackupStateCard(
        modifier = modifier,
        icon = R.drawable.ic_no_wifi,
        tint = ProtonTheme.colors.notificationError,
        text = I18N.string.photos_error_waiting_wifi_connectivity,
        action = I18N.string.photos_error_waiting_wifi_connectivity_action,
        onClick = onChangeNetwork
    )
}

@Composable
fun NoConnectivityState(
    modifier: Modifier = Modifier,
) {
    BackupStateCard(
        modifier = modifier,
        icon = R.drawable.ic_no_wifi,
        tint = ProtonTheme.colors.notificationError,
        text = I18N.string.photos_error_waiting_connectivity,
    )
}

@Composable
fun PhotoShareMigrationNeededState(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = EnterTransition.None,
        exit = shrinkVertically() + fadeOut(),
    ) {
        BackupCard(modifier = modifier) {
            Column {
                BackupStateCard(
                    modifier = Modifier,
                    icon = CorePresentation.drawable.ic_proton_cloud,
                    tint = ProtonTheme.colors.notificationError,
                    text = I18N.string.photos_share_migration_needed_banner_title,
                )
                Divider(
                    color = ProtonTheme.colors.separatorNorm,
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                )
                ErrorDetails(
                    text = stringResource(I18N.string.photos_share_migration_needed_banner_description),
                    action = I18N.string.common_start_action,
                    onClick = onStart,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BackupCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = ProtonTheme.shapes.medium,
            backgroundColor = ProtonTheme.colors.backgroundSecondary,
            contentColor = ProtonTheme.colors.textNorm,
            elevation = 0.dp,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            onClick = onClick,
            shape = ProtonTheme.shapes.medium,
            backgroundColor = ProtonTheme.colors.backgroundSecondary,
            contentColor = ProtonTheme.colors.textNorm,
            elevation = 0.dp,
            content = content,
        )
    }
}

@Composable
private fun BackupStateCard(
    modifier: Modifier = Modifier,
    icon: Int,
    tint: Color,
    text: Int,
    action: Int,
    onClick: (() -> Unit)? = null,
) {
    BackupCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        BackupState(
            icon = icon,
            tint = tint,
            text = text,
            action = action,
        )
    }
}

@Composable
private fun BackupState(
    icon: Int,
    tint: Color,
    text: Int,
    content: @Composable () -> Unit = {}
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
        content()
    }
}

@Composable
private fun BackupState(
    icon: Int,
    tint: Color,
    text: Int,
    action: Int,
) {
    BackupState(icon, tint, text) {
        Text(
            text = stringResource(id = action),
            style = ProtonTheme.typography.defaultSmallStrongUnspecified,
            color = ProtonTheme.colors.interactionNorm,
        )
    }
}

@Composable
private fun BackupStateCard(
    modifier: Modifier = Modifier,
    icon: Int,
    tint: Color,
    text: Int,
    secondaryText: String? = null,
) {
    BackupCard(modifier = modifier) {
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
fun BackupCompletedStatePreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            BackupCompletedState("1 000 items saved")
        }
    }
}

@Preview
@Composable
fun BackupUncompletedStatePreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            BackupUncompletedState(onResolve = {})
        }
    }
}

@Preview
@Composable
private fun PreparingStatePreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            BackupPreparingState(
                preparing = PhotosStatusViewState.Preparing(
                    progress = 0.3F,
                ),
            )
        }
    }
}

@Preview
@Composable
fun EncryptingStatePreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
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
fun UploadingStatePreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
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
fun MissingPermissionsStatePreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            MissingPermissionsState(onPermissions = { })
        }
    }
}

@Preview
@Composable
fun BackupDisableStatePreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            BackupDisableState(onEnableBackup = { })
        }
    }
}

@Preview
@Composable
fun BackupMissingFolderStatePreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            BackupMissingFolderState(onMore = { })
        }
    }
}

@Preview
@Composable
fun BackupFailedStatePreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            BackupFailedState(onRetry = { })
        }
    }
}

@Preview
@Composable
fun BackgroundRestrictionsPreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            BackgroundRestrictions(
                onIgnoreBackgroundRestrictions = { },
                onDismissBackgroundRestrictions = { },
            )
        }
    }
}

@Preview
@Composable
fun LocalStorageStatePreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            LocalStorageState(onRetry = { })
        }
    }
}

@Preview
@Composable
fun BackupTemporarilyDisabledStatePreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            BackupTemporarilyDisabledState(onRetry = { })
        }
    }
}

@Preview
@Composable
fun BackupMigrationStatePreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            BackupMigrationState()
        }
    }
}
