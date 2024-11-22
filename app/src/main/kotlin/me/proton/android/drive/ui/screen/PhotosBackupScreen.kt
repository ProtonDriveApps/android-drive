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

package me.proton.android.drive.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.photos.presentation.component.BackupPermissions
import me.proton.android.drive.photos.presentation.component.LibraryFolders
import me.proton.android.drive.ui.action.PhotoExtractDataAction
import me.proton.android.drive.ui.viewevent.PhotosBackupViewEvent
import me.proton.android.drive.ui.viewmodel.PhotosBackupViewModel
import me.proton.android.drive.ui.viewstate.PhotosBackupOption
import me.proton.android.drive.ui.viewstate.PhotosBackupViewState
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.ListItemHeight
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.presentation.R as CorePresentation

@Composable
fun PhotosBackupScreen(
    modifier: Modifier = Modifier,
    navigateToPhotosPermissionRationale: () -> Unit,
    navigateToConfirmStopSyncFolder: (FolderId, Int) -> Unit,
    navigateToNotificationPermissionRationale: () -> Unit,
    navigateBack: () -> Unit,
) {
    val viewModel = hiltViewModel<PhotosBackupViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(initialValue = viewModel.initialViewState)
    val viewEvent = remember {
        viewModel.viewEvent()
    }
    PhotosBackup(
        viewState = viewState,
        viewEvent = viewEvent,
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        navigateBack = navigateBack,
    ) {
        PhotoExtractDataAction()
        LibraryFolders(
            modifier = Modifier.fillMaxSize(),
            navigateToConfirmStopSyncFolder = navigateToConfirmStopSyncFolder,
        )
    }
    BackupPermissions(
        viewState = viewModel.backupPermissionsViewModel.initialViewState,
        viewEvent = viewModel.backupPermissionsViewModel.viewEvent(
            navigateToPhotosPermissionRationale
        ),
        navigateToNotificationPermissionRationale = navigateToNotificationPermissionRationale,
    )
}

@Composable
fun PhotosBackup(
    viewState: PhotosBackupViewState,
    viewEvent: PhotosBackupViewEvent,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    content: @Composable() (ColumnScope.() -> Unit)
) {
    Column(modifier = modifier) {
        TopAppBar(
            navigationIcon = painterResource(id = CorePresentation.drawable.ic_arrow_back),
            onNavigationIcon = navigateBack,
            title = viewState.title,
        )
        val localContext = LocalContext.current
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {}
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .nestedScroll(nestedScrollConnection)
        ) {
            BackupPhotosOptions(
                onToggleBackup = { viewEvent.onToggleBackup() },
                onToggleMobileData = { viewEvent.onToggleMobileData() },
                onToggleIgnoringBatteryOptimizations = {
                    viewEvent.onToggleIgnoringBatteryOptimizations(
                        localContext
                    )
                },
                backup = viewState.backup,
                mobileData = viewState.mobileData,
                ignoringBatteryOptimizations = viewState.ignoringBatteryOptimizations,
            )
            content()
        }
    }
}

@Composable
fun BackupPhotosOptions(
    modifier: Modifier = Modifier,
    onToggleBackup: () -> Unit,
    onToggleMobileData: () -> Unit,
    onToggleIgnoringBatteryOptimizations: () -> Unit,
    backup: PhotosBackupOption,
    mobileData: PhotosBackupOption,
    ignoringBatteryOptimizations: PhotosBackupOption,
) {
    Column(modifier) {
        BackupPhotosToggle(option = backup, onToggle = onToggleBackup)
        BackupPhotosToggle(option = mobileData, onToggle = onToggleMobileData)
        BackupPhotosToggle(
            option = ignoringBatteryOptimizations,
            onToggle = onToggleIgnoringBatteryOptimizations,
        )
    }
}

@Composable
fun BackupPhotosToggle(
    option: PhotosBackupOption,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
) {
    BackupPhotosToggle(
        modifier = modifier,
        checked = option.checked,
        enabled = option.enabled,
        onToggle = onToggle,
    ) {
        Text(
            text = option.title,
            style = ProtonTheme.typography.defaultNorm(option.enabled),

            )
        if (option.description != null) {
            Text(
                option.description,
                style = ProtonTheme.typography.captionWeak(option.enabled),
            )
        }
    }
}

@Composable
fun BackupPhotosToggle(
    checked: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ProtonRawListItem(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = ListItemHeight)
            .toggleable(
                value = enabled,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = { onToggle() },
            )
            .padding(horizontal = DefaultSpacing, vertical = SmallSpacing),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            content()
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

@Preview
@Composable
private fun PhotosBackupPreview() {
    ProtonTheme {
        PhotosBackup(
            viewState = PhotosBackupViewState(
                title = "Photo backup",
                backup = PhotosBackupOption(
                    title = "Photo backup",
                    checked = true,
                ),
                mobileData = PhotosBackupOption(
                    title = "Use mobile data to backup photos",
                    checked = false,
                    enabled = false,
                ),
                ignoringBatteryOptimizations = PhotosBackupOption(
                    title = "Disable battery optimization",
                    description = "Allows uninterrupted backup when the app is running in the background.",
                    checked = false,
                    enabled = false,
                ),
            ),
            viewEvent = object : PhotosBackupViewEvent {
                override val onToggleBackup = {}
                override val onToggleMobileData = {}
                override val onToggleIgnoringBatteryOptimizations = { _: Context -> }
            },
            modifier = Modifier.fillMaxSize(),
            navigateBack = {},
            content = {}
        )
    }
}

@Preview
@Composable
private fun PreviewBackupPhotosToggle() {
    ProtonTheme {
        BackupPhotosToggle(
            PhotosBackupOption(
                title = "Photo backup",
                checked = true,
            ),
            onToggle = {},
        )
    }
}

@Preview
@Composable
private fun PreviewBackupPhotosDescriptionToggle() {
    ProtonTheme {
        BackupPhotosToggle(
            PhotosBackupOption(
                title = "Disable battery optimization",
                description = "Allows uninterrupted backup when the app is running in the background.",
                checked = true,
            ),
            onToggle = {},
        )
    }
}
