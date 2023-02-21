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

package me.proton.android.drive.ui.viewmodel

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.options.Option
import me.proton.android.drive.ui.options.filterAll
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.documentsprovider.domain.usecase.ExportToDownload
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.selection.domain.usecase.GetSelectedDriveLinks
import me.proton.core.drive.files.presentation.entry.OptionEntry
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.trash.domain.usecase.SendToTrash
import javax.inject.Inject

@HiltViewModel
class MultipleFileOrFolderOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getSelectedDriveLinks: GetSelectedDriveLinks,
    private val sendToTrash: SendToTrash,
    private val exportToDownload: ExportToDownload,
    private val deselectLinks: DeselectLinks,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val selectionId = SelectionId(requireNotNull(savedStateHandle.get(KEY_SELECTION_ID)))
    val selectedDriveLinks: Flow<List<DriveLink>> = getSelectedDriveLinks(selectionId)
    // Send -> ACTION_SEND_MULTIPLE (mime type aggregation) - we need to update sendfiledialog with multiple file download
    //   Mime type aggregation - all the same use that one, all same prefix use prefix/*  else use */*
    fun entries(
        driveLinks: List<DriveLink>,
        runAction: (suspend () -> Unit) -> Unit,
        navigateToMove: (SelectionId, parentId: FolderId?) -> Unit,
        dismiss: () -> Unit,
    ): List<OptionEntry<Unit>> =
        options
            .filterAll(driveLinks)
            .map { option ->
                when (option) {
                    is Option.Trash -> option.build(
                        runAction = runAction,
                        moveToTrash = {
                            viewModelScope.launch {
                                sendToTrash(userId, driveLinks)
                                deselectLinks(selectionId)
                                dismiss()
                            }
                        },
                    )
                    is Option.Move -> option.build(
                        runAction = runAction,
                        navigateToMoveAll = {
                            navigateToMove(selectionId, driveLinks.first().parentId)
                        }
                    )
                    is Option.Download -> option.build(
                        runAction = runAction,
                        download = {
                            viewModelScope.launch {
                                exportToDownload(
                                    driveLinks.filterIsInstance<DriveLink.File>().map { driveLink -> driveLink.id }
                                )
                                deselectLinks(selectionId)
                                dismiss()
                            }
                        }
                    )
                    else -> throw IllegalStateException(
                        "Option ${option.javaClass.simpleName} is not found. Did you forget to add it?"
                    )
                }
            }
            .apply {
                if (isEmpty() || driveLinks.isEmpty()) dismiss()
            }

    companion object {
        const val KEY_SELECTION_ID = "selectionId"

        private val options = setOfNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Option.Download else null,
            Option.Move,
            Option.Trash,
        )
    }
}
