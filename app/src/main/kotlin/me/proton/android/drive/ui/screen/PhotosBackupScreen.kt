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

package me.proton.android.drive.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.photos.presentation.component.BackupPermissions
import me.proton.android.drive.photos.presentation.component.LibraryFolders
import me.proton.android.drive.ui.action.PhotoExtractDataAction
import me.proton.android.drive.ui.viewevent.PhotosBackupViewEvent
import me.proton.android.drive.ui.viewmodel.PhotosBackupViewModel
import me.proton.android.drive.ui.viewstate.PhotosBackupViewState
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.ListItemHeight
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.presentation.R as CorePresentation

@Composable
fun PhotosBackupScreen(
    modifier: Modifier = Modifier,
    navigateToPhotosPermissionRationale: () -> Unit,
    navigateToConfirmStopSyncFolder: (FolderId, Int) -> Unit,
    navigateBack: () -> Unit,
) {
    val viewModel = hiltViewModel<PhotosBackupViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = viewModel.initialViewState)
    val viewEvent = remember {
        viewModel.viewEvent(navigateBack = navigateBack)
    }
    PhotosBackup(
        viewState = viewState,
        viewEvent = viewEvent,
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        navigateBack = navigateBack,
        navigateToConfirmStopSyncFolder = navigateToConfirmStopSyncFolder,
    )
    BackupPermissions(
        viewState = viewModel.backupPermissionsViewModel.initialViewState,
        viewEvent = viewModel.backupPermissionsViewModel.viewEvent(
            navigateToPhotosPermissionRationale
        )
    )
}

@Composable
fun PhotosBackup(
    viewState: PhotosBackupViewState,
    viewEvent: PhotosBackupViewEvent,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    navigateToConfirmStopSyncFolder: (FolderId, Int) -> Unit,
) {
    Column(modifier = modifier) {
        TopAppBar(
            navigationIcon = painterResource(id = CorePresentation.drawable.ic_arrow_back),
            onNavigationIcon = navigateBack,
            title = viewState.title,
        )
        BackupPhotosOptions(
            enableBackupTitle = viewState.enableBackupTitle,
            isBackupEnabled = viewState.isBackupEnabled,
        ) {
            viewEvent.onToggle()
        }
        PhotoExtractDataAction()
        LibraryFolders(
            modifier = Modifier.fillMaxSize(),
            navigateToConfirmStopSyncFolder = navigateToConfirmStopSyncFolder,
        )
    }
}

@Composable
fun BackupPhotosOptions(
    enableBackupTitle: String,
    isBackupEnabled: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
) {
    BackupPhotosToggle(
        title = enableBackupTitle,
        isEnabled = isBackupEnabled,
        onToggle = onToggle,
        modifier = modifier,
    )
}

@Composable
fun BackupPhotosToggle(
    title: String,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
) {
    ProtonRawListItem(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = ListItemHeight)
            .toggleable(
                value = isEnabled,
                enabled = true,
                role = Role.Switch,
                onValueChange = { onToggle() },
            )
            .padding(horizontal = DefaultSpacing)
            .testTag(PhotosBackupSettingsScreenTestTag.previewBackupToggle),
    ) {
        Text(
            text = title,
            style = ProtonTheme.typography.defaultNorm,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = null,
        )
    }
}

@Preview
@Composable
private fun PhotosBackupPreview() {
    ProtonTheme {
        PhotosBackup(
            viewState = PhotosBackupViewState(
                title = "Photos Backup",
                enableBackupTitle = "Photos Backup",
                isBackupEnabled = true,
            ),
            viewEvent = object : PhotosBackupViewEvent {
                override val onToggle = {}
            },
            modifier = Modifier.fillMaxSize(),
            navigateBack = {},
            navigateToConfirmStopSyncFolder = { _, _ -> }
        )
    }
}

@Preview
@Composable
private fun PreviewBackupPhotosToggle() {
    ProtonTheme {
        BackupPhotosToggle(
            title = "Photos backup",
            isEnabled = true,
            onToggle = {},
        )
    }
}

object PhotosBackupSettingsScreenTestTag {
    const val previewBackupToggle = "backup preview toggle"
}
