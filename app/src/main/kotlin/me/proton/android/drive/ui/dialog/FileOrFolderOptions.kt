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

package me.proton.android.drive.ui.dialog

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.viewmodel.FileOrFolderOptionsViewModel
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.base.presentation.component.rememberCreateDocumentLauncher
import me.proton.core.drive.base.presentation.extension.launchWithNotFound
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.files.presentation.component.files.FilesOptions
import me.proton.core.drive.files.presentation.component.folder.FolderOptions
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.notification.presentation.NotificationPermission

@Composable
fun FileOrFolderOptions(
    runAction: RunAction,
    navigateToInfo: (linkId: LinkId) -> Unit,
    navigateToMove: (linkId: LinkId, parentId: FolderId?) -> Unit,
    navigateToRename: (linkId: LinkId) -> Unit,
    navigateToDelete: (linkId: LinkId) -> Unit,
    navigateToSendFile: (fileId: FileId) -> Unit,
    navigateToStopSharing: (linkId: LinkId) -> Unit,
    navigateToShareViaLink: (linkId: LinkId) -> Unit,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<FileOrFolderOptionsViewModel>()
    val driveLink by rememberFlowWithLifecycle(viewModel.driveLink).collectAsState(initial = null)
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
        navigateToStopSharing = navigateToStopSharing,
        navigateToShareViaLink = navigateToShareViaLink,
        dismiss = dismiss,
        modifier = modifier
            .navigationBarsPadding()
            .testTag(FileFolderOptionsDialogTestTag.fileOrFolderOptions),
    )
    NotificationPermission()
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
    navigateToStopSharing: (linkId: LinkId) -> Unit,
    navigateToShareViaLink: (linkId: LinkId) -> Unit,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            FilesOptions(
                file = driveLink,
                entries = viewModel.entries(
                    driveLink = driveLink,
                    runAction = runAction,
                    navigateToInfo = navigateToInfo,
                    navigateToMove = navigateToMove,
                    navigateToRename = navigateToRename,
                    navigateToDelete = navigateToDelete,
                    navigateToSendFile = navigateToSendFile,
                    navigateToStopSharing = navigateToStopSharing,
                    navigateToShareViaLink = navigateToShareViaLink,
                    dismiss = dismiss,
                    showCreateDocumentPicker = { filename, onNotFound ->
                        createDocumentLauncher.launchWithNotFound(filename, onNotFound)
                    },
                ),
                modifier = modifier
                    .testTag(FileFolderOptionsDialogTestTag.fileOptions),
            )
        }
        is DriveLink.Folder ->
            FolderOptions(
                folder = driveLink,
                entries = viewModel.entries(
                    driveLink = driveLink,
                    runAction = runAction,
                    navigateToInfo = navigateToInfo,
                    navigateToMove = navigateToMove,
                    navigateToRename = navigateToRename,
                    navigateToDelete = navigateToDelete,
                    navigateToSendFile = navigateToSendFile,
                    navigateToStopSharing = navigateToStopSharing,
                    navigateToShareViaLink = navigateToShareViaLink,
                    dismiss = dismiss,
                ),
                modifier = modifier
                    .testTag(FileFolderOptionsDialogTestTag.folderOptions),
            )
    }
}

object FileFolderOptionsDialogTestTag {
    const val fileOptions = "file options context menu"
    const val folderOptions = "folder options context menu"
    const val fileOrFolderOptions = "file or folder options context menu"
}
