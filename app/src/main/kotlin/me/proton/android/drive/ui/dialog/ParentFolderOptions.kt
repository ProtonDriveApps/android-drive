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

import android.content.Intent
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.viewmodel.ParentFolderOptionsViewModel
import me.proton.core.compose.activity.rememberCameraLauncher
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.drive.base.presentation.component.rememberFilePickerLauncher
import me.proton.core.drive.base.presentation.extension.captureWithNotFound
import me.proton.core.drive.base.presentation.extension.launchWithNotFound
import me.proton.core.drive.files.presentation.component.folder.ParentFolderOptions
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.notification.presentation.NotificationPermission

/**
 * This method is split into two because we want to decouple the retrieval of the view model from the content.
 * As the composable could be dismissed (removed from backstack) at the same time of a recomposition.
 * This results in an:
 * IllegalStateException: You cannot access the NavBackStackEntry's ViewModels after theNavBackStackEntry is destroyed.
 * To alleviate this problem, we moved the recomposition into a sub composable which doesn't trigger the re-fetching
 * of the ViewModel
 */

@Composable
fun ParentFolderOptions(
    runAction: RunAction,
    navigateToCreateFolder: (folderId: FolderId) -> Unit,
    navigateToStorageFull: () -> Unit,
    modifier: Modifier = Modifier,
    dismiss: () -> Unit,
) = ParentFolderOptions(
    viewModel = hiltViewModel(),
    runAction = runAction,
    navigateToCreateFolder = navigateToCreateFolder,
    navigateToStorageFull = navigateToStorageFull,
    modifier = modifier
        .testTag(ParentFolderOptionsDialogTestTag.contextMenu),
    dismiss = dismiss,
)

@Composable
fun ParentFolderOptions(
    viewModel: ParentFolderOptionsViewModel,
    runAction: RunAction,
    navigateToCreateFolder: (folderId: FolderId) -> Unit,
    navigateToStorageFull: () -> Unit,
    modifier: Modifier = Modifier,
    dismiss: () -> Unit,
) {
    val driveLink by rememberFlowWithLifecycle(viewModel.driveLink).collectAsState(initial = null)
    val folder = driveLink ?: return

    val filePickerLauncher = rememberFilePickerLauncher(
        onFilesPicked = { filesUri ->
            viewModel.onAddFileResult(
                uriStrings = filesUri.map { fileUri -> fileUri.toString() },
                navigateToStorageFull = navigateToStorageFull,
                dismiss = dismiss
            )
        },
        modifyIntent = { intent -> intent.addCategory(Intent.CATEGORY_OPENABLE) }
    )
    val cameraLauncher = rememberCameraLauncher(
        onCaptured = { isTaken ->
            viewModel.onCameraResult(
                isTaken = isTaken,
                navigateToStorageFull = navigateToStorageFull,
                dismiss = dismiss,
            )
        }
    )
    ParentFolderOptions(
        folder = folder,
        entries = viewModel.entries(
            folder = folder,
            runAction = runAction,
            navigateToCreateFolder = navigateToCreateFolder,
            showFilePicker = { onNotFound -> filePickerLauncher.launchWithNotFound(onNotFound) },
            takeAPhoto = { uri, onNotFound -> cameraLauncher.captureWithNotFound(uri, onNotFound) },
            dismiss = dismiss,
        ),
        modifier = modifier.navigationBarsPadding(),
    )
    NotificationPermission()
}

object ParentFolderOptionsDialogTestTag {
    const val contextMenu = "parent folder options context menu"
}
