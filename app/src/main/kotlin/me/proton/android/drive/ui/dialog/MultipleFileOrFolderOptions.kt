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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import me.proton.android.drive.ui.viewmodel.MultipleFileOrFolderOptionsViewModel
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.drive.files.presentation.component.common.MultipleOptions
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.selection.domain.entity.SelectionId

@Composable
fun MultipleFileOrFolderOptions(
    runAction: RunAction,
    navigateToMove: (selectionId: SelectionId, folderId: FolderId?) -> Unit,
    navigateToCreateNewAlbum: () -> Unit,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<MultipleFileOrFolderOptionsViewModel>()
    val driveLinks by viewModel.selectedDriveLinks.collectAsStateWithLifecycle(initialValue = null)
    val selectedDriveLinks = driveLinks ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val entries by remember(viewModel, lifecycle) {
        viewModel.entries(
            driveLinks = selectedDriveLinks,
            runAction = runAction,
            navigateToMove = navigateToMove,
            navigateToCreateNewAlbum = navigateToCreateNewAlbum,
            dismiss = dismiss,
        ).flowWithLifecycle(
            lifecycle = lifecycle,
            minActiveState = Lifecycle.State.STARTED
        )
    }.collectAsState(initial = null)
    val multipleEntries = entries ?: return
    MultipleOptions(
        count = selectedDriveLinks.size,
        entries = multipleEntries,
        modifier = modifier
            .testTag(MultipleFileFolderOptionsDialogTestTag.fileOrFolderOptions)
            .navigationBarsPadding(),
    )
}

object MultipleFileFolderOptionsDialogTestTag {
    const val fileOrFolderOptions = "file or folder options context menu"
}
