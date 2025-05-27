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

package me.proton.android.drive.ui.dialog

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import me.proton.android.drive.ui.viewmodel.FileOrFolderOptionsViewModel
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.drive.base.presentation.component.rememberCreateDocumentLauncher
import me.proton.core.drive.base.presentation.extension.launchWithNotFound
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.files.presentation.component.files.FilesOptions
import me.proton.core.drive.files.presentation.component.folder.FolderOptions
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.notification.presentation.component.NotificationPermission

@Composable
fun FileOrFolderOptions(
    runAction: RunAction,
    navigateToInfo: (linkId: LinkId) -> Unit,
    navigateToMove: (linkId: LinkId, parentId: FolderId?) -> Unit,
    navigateToRename: (linkId: LinkId) -> Unit,
    navigateToDelete: (linkId: LinkId) -> Unit,
    navigateToSendFile: (fileId: FileId) -> Unit,
    navigateToManageAccess: (linkId: LinkId) -> Unit,
    navigateToShareViaInvitations: (linkId: LinkId) -> Unit,
    navigateToNotificationPermissionRationale: () -> Unit,
    navigateToAddToAlbumsOptions: (selectionId: SelectionId) -> Unit,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<FileOrFolderOptionsViewModel>()
    val driveLink by viewModel.driveLink.collectAsStateWithLifecycle(initialValue = null)
    val link = driveLink ?: return
    FileOrFolderOptions(
        driveLink = link,
        viewModel = viewModel,
        runAction = runAction,
        navigateToInfo = navigateToInfo,
        navigateToMove = navigateToMove,
        navigateToRename = navigateToRename,
        navigateToDelete = navigateToDelete,
        navigateToSendFile = navigateToSendFile,
        navigateToManageAccess = navigateToManageAccess,
        navigateToShareViaInvitations = navigateToShareViaInvitations,
        navigateToAddToAlbumsOptions = navigateToAddToAlbumsOptions,
        dismiss = dismiss,
        modifier = modifier
            .navigationBarsPadding()
            .testTag(FileFolderOptionsDialogTestTag.fileOrFolderOptions),
    )
    NotificationPermission(
        shouldShowRationale = false,
        navigateToNotificationPermissionRationale = navigateToNotificationPermissionRationale,
    )
}

@Composable
fun FileOrFolderOptions(
    driveLink: DriveLink,
    viewModel: FileOrFolderOptionsViewModel,
    runAction: RunAction,
    navigateToInfo: (linkId: LinkId) -> Unit,
    navigateToMove: (linkId: LinkId, parentId: FolderId?) -> Unit,
    navigateToRename: (linkId: LinkId) -> Unit,
    navigateToDelete: (linkId: LinkId) -> Unit,
    navigateToSendFile: (fileId: FileId) -> Unit,
    navigateToManageAccess: (linkId: LinkId) -> Unit,
    navigateToShareViaInvitations: (linkId: LinkId) -> Unit,
    navigateToAddToAlbumsOptions: (selectionId: SelectionId) -> Unit,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    when (driveLink) {
        is DriveLink.File -> {
            val createDocumentLauncher = rememberCreateDocumentLauncher(
                onDocumentCreated = { documentUri ->
                    documentUri?.let {
                        viewModel.onCreateDocumentResult(driveLink.id, documentUri)
                    }
                },
                mimeType = driveLink.mimeType,
            )
            val entries by remember(viewModel, lifecycle) {
                viewModel.entries<DriveLink.File>(
                    runAction = runAction,
                    navigateToInfo = navigateToInfo,
                    navigateToMove = navigateToMove,
                    navigateToRename = navigateToRename,
                    navigateToDelete = navigateToDelete,
                    navigateToSendFile = navigateToSendFile,
                    navigateToManageAccess = navigateToManageAccess,
                    navigateToShareViaInvitations = navigateToShareViaInvitations,
                    navigateToAddToAlbumsOptions = navigateToAddToAlbumsOptions,
                    dismiss = dismiss,
                    showCreateDocumentPicker = { filename, onNotFound ->
                        createDocumentLauncher.launchWithNotFound(filename, onNotFound)
                    },
                ).flowWithLifecycle(
                    lifecycle = lifecycle,
                    minActiveState = Lifecycle.State.STARTED
                )
            }.collectAsState(initial = null)
            val fileEntries = entries ?: return
            FilesOptions(
                file = driveLink,
                entries = fileEntries,
                modifier = modifier
                    .testTag(FileFolderOptionsDialogTestTag.fileOptions),
            )
        }
        is DriveLink.Folder -> {
            val entries by remember(viewModel, lifecycle) {
                viewModel.entries<DriveLink.Folder>(
                    runAction = runAction,
                    navigateToInfo = navigateToInfo,
                    navigateToMove = navigateToMove,
                    navigateToRename = navigateToRename,
                    navigateToDelete = navigateToDelete,
                    navigateToSendFile = navigateToSendFile,
                    navigateToManageAccess = navigateToManageAccess,
                    navigateToShareViaInvitations = navigateToShareViaInvitations,
                    navigateToAddToAlbumsOptions = navigateToAddToAlbumsOptions,
                    dismiss = dismiss,
                ).flowWithLifecycle(
                    lifecycle = lifecycle,
                    minActiveState = Lifecycle.State.STARTED
                )
            }.collectAsState(initial = null)
            val folderEntries = entries ?: return
            FolderOptions(
                folder = driveLink,
                entries = folderEntries,
                modifier = modifier
                    .testTag(FileFolderOptionsDialogTestTag.folderOptions),
            )
        }
        is DriveLink.Album -> error("On Album we should invoke AlbumOptions")
    }
}

object FileFolderOptionsDialogTestTag {
    const val fileOptions = "file options context menu"
    const val folderOptions = "folder options context menu"
    const val fileOrFolderOptions = "file or folder options context menu"
}
