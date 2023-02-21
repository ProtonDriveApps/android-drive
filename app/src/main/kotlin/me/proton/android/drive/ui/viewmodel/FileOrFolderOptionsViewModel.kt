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

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.options.Option
import me.proton.android.drive.ui.options.filter
import me.proton.android.drive.usecase.NotifyActivityNotFound
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.documentsprovider.domain.usecase.ExportTo
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.offline.domain.usecase.ToggleOffline
import me.proton.core.drive.drivelink.trash.domain.usecase.ToggleTrashState
import me.proton.core.drive.files.presentation.entry.FileOptionEntry
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.shareurl.crypto.domain.usecase.CopyPublicUrl
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation

@HiltViewModel
class FileOrFolderOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getDriveLink: GetDecryptedDriveLink,
    private val toggleOffline: ToggleOffline,
    private val toggleTrashState: ToggleTrashState,
    private val copyPublicUrl: CopyPublicUrl,
    private val exportTo: ExportTo,
    private val notifyActivityNotFound: NotifyActivityNotFound,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private var dismiss: (() -> Unit)? = null
    val driveLink: Flow<DriveLink?> = getDriveLink(
        linkId = FileId(ShareId(userId, savedStateHandle.require(KEY_SHARE_ID)), savedStateHandle.require(KEY_LINK_ID)),
        failOnDecryptionError = false,
    )
        .mapSuccessValueOrNull()
        .mapWithPrevious { previous, new ->
            if (previous != null && new == null) {
                dismiss?.invoke()
            }
            new
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun <T : DriveLink> entries(
        driveLink: DriveLink,
        runAction: (suspend () -> Unit) -> Unit,
        navigateToInfo: (linkId: LinkId) -> Unit,
        navigateToMove: (linkId: LinkId, parentId: FolderId?) -> Unit,
        navigateToRename: (linkId: LinkId) -> Unit,
        navigateToDelete: (linkId: LinkId) -> Unit,
        navigateToSendFile: (fileId: FileId) -> Unit,
        navigateToStopSharing: (linkId: LinkId) -> Unit,
        navigateToShareViaLink: (linkId: LinkId) -> Unit,
        dismiss: () -> Unit,
        showCreateDocumentPicker: (String, () -> Unit) -> Unit = { _, _ -> },
    ): List<FileOptionEntry<T>> =
        options
            .filter(driveLink)
            .map { option ->
                @Suppress("UNCHECKED_CAST")
                when (option) {
                    is Option.DeletePermanently -> option.build(runAction, navigateToDelete)
                    is Option.Info -> option.build(runAction, navigateToInfo)
                    is Option.Move -> option.build(runAction, navigateToMove)
                    is Option.OfflineToggle -> option.build(runAction) { driveLink ->
                        viewModelScope.launch {
                            toggleOffline(driveLink)
                            dismiss()
                        }
                    }
                    is Option.Rename -> option.build(runAction, navigateToRename)
                    is Option.Trash -> option.build(
                        runAction = runAction,
                        toggleTrash = {
                            viewModelScope.launch {
                                toggleTrashState(driveLink)
                                dismiss()
                            }
                        }
                    )
                    is Option.SendFile -> option.build(runAction, navigateToSendFile)
                    is Option.Download -> option.build { filename ->
                        showCreateDocumentPicker(filename) { handleActivityNotFound() }
                    }
                    is Option.CopySharedLink -> option.build(runAction) { linkId ->
                        copyPublicUrl(linkId)
                        dismiss()
                    }
                    is Option.ShareViaLink -> option.build(runAction, navigateToShareViaLink)
                    is Option.StopSharing -> option.build(runAction, navigateToStopSharing)
                    else -> throw IllegalStateException(
                        "Option ${option.javaClass.simpleName} is not found. Did you forget to add it?"
                    )
                }
            }.also {
                this.dismiss = dismiss
            }

    fun onCreateDocumentResult(fileId: FileId, documentUri: Uri) {
        exportTo(fileId = fileId, destinationUri = documentUri)
        dismiss?.invoke()
    }

    private fun handleActivityNotFound() {
        this.dismiss?.invoke()
        notifyActivityNotFound(userId, BasePresentation.string.operation_create_document)
    }

    companion object {
        const val KEY_SHARE_ID = "shareId"
        const val KEY_LINK_ID = "linkId"

        private val options = setOf(
            Option.OfflineToggle,
            Option.CopySharedLink,
            Option.ShareViaLink,
            Option.SendFile,
            Option.Download,
            Option.Move,
            Option.Rename,
            Option.Trash,
            Option.Info,
            Option.StopSharing,
            Option.DeletePermanently,
        )
    }
}
