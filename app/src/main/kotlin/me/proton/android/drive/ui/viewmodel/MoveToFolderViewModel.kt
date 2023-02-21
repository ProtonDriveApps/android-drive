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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.CombinedLoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.R
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.viewstate.MoveFileViewState
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.list.domain.usecase.GetPagedDriveLinksList
import me.proton.core.drive.drivelink.selection.domain.usecase.GetSelectedDriveLinks
import me.proton.core.drive.files.domain.usecase.MoveFile
import me.proton.core.drive.files.presentation.event.FilesViewEvent
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Sorting
import javax.inject.Inject
import me.proton.core.presentation.R as CorePresentation

@ExperimentalCoroutinesApi
@HiltViewModel
class MoveToFolderViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    getDriveLink: GetDecryptedDriveLink,
    getPagedDriveLinks: GetPagedDriveLinksList,
    getSelectedDriveLinks: GetSelectedDriveLinks,
    private val moveFile: MoveFile,
    private val deselectLinks: DeselectLinks,
    savedStateHandle: SavedStateHandle,
    configurationProvider: ConfigurationProvider,
) : HostFilesViewModel(appContext, getDriveLink, getPagedDriveLinks, savedStateHandle, configurationProvider) {
    private val shareId = savedStateHandle.get<String?>(Screen.Move.SHARE_ID)?.let { id ->
        ShareId(userId, id)
    }
    private val linkId = savedStateHandle.get<String?>(Screen.Move.LINK_ID)
    private val selectionId = savedStateHandle.get<String?>(SELECTION_ID)?.let { SelectionId(it) }
    private val parentShareId: ShareId? = savedStateHandle.get<String?>(Screen.Move.PARENT_SHARE_ID)?.let { id ->
        ShareId(userId, id)
    }
    override val parentId = savedStateHandle.get<String>(Screen.Move.PARENT_ID)?.let { parentId ->
        parentShareId?.let {
            FolderId(parentShareId, parentId).also { folderId ->
                trigger.tryEmit(folderId)
            }
        }
    }

    private val driveLinksToMove: StateFlow<List<DriveLink>> = if (selectionId != null) {
        getSelectedDriveLinks(selectionId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    } else if (shareId != null && linkId != null) {
        getDriveLink(linkId = FileId(shareId, linkId), failOnDecryptionError = false)
            .mapSuccessValueOrNull()
            .transformLatest { link -> emit(listOfNotNull(link)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    } else {
        throw IllegalStateException("")
    }
    val initialViewState = MoveFileViewState(
        filesViewState = initialFilesViewState,
        isMoveButtonEnabled = false,
        title = "",
    )
    val viewState: Flow<MoveFileViewState> = combine(
        driveLinksToMove,
        parentLink,
        listContentState,
        listContentAppendingState
    ) { filesToMove, parentLink, contentState, appendingState ->
        val isRoot = parentLink != null && parentLink.parentId == null
        initialViewState.copy(
            filesViewState = initialViewState.filesViewState.copy(
                title = if (isRoot) null else parentLink?.name.orEmpty(),
                isTitleEncrypted = isRoot.not() && parentLink.isNameEncrypted,
                navigationIconResId = if (isRoot) 0 else CorePresentation.drawable.ic_arrow_back,
                listContentState = contentState,
                listContentAppendingState = appendingState
            ),
            isMoveButtonEnabled = parentLink?.id != parentId,
            title = filesToMove.title,
            isTitleEncrypted = (filesToMove.size == 1 && filesToMove.first().isNameEncrypted)
        )
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    fun viewEvent(
        onDismissRequest: () -> Unit,
    ): FilesViewEvent = object : FilesViewEvent {
        override val onDriveLink = { driveLink: DriveLink ->
            if (driveLink is DriveLink.Folder) {
                viewModelScope.launch {
                    if ((driveLink.id.shareId == shareId && driveLink.id.id == linkId) ||
                        driveLinksToMove.value.map { driveLink -> driveLink.id }.contains(driveLink.id)) {
                        showError(R.string.move_file_error_cannot_move_folder_into_itself)
                    } else {
                        trigger.emit(driveLink.id)
                    }
                }
            }
        }
        override val onLoadState: (CombinedLoadStates, Int) -> Unit = this@MoveToFolderViewModel.onLoadState
        override val onSorting: (Sorting) -> Unit = this@MoveToFolderViewModel.onSorting
        override val onTopAppBarNavigation: () -> Unit = this@MoveToFolderViewModel.onTopAppBarNavigation(onDismissRequest)
        override val onErrorAction: () -> Unit = this@MoveToFolderViewModel.onRetry
        override val onAppendErrorAction: () -> Unit = this@MoveToFolderViewModel.onRetry
    }

    fun confirmMove() {
        parentLink.value?.let { folder ->
            if (folder.id != parentId) {
                viewModelScope.launch {
                    moveFile(userId, driveLinksToMove.value.map { driveLink -> driveLink.id }, folder.id)
                    selectionId?.let{ deselectLinks(selectionId) }
                }
            }
        }
    }

    private val List<DriveLink>.title: String get() = when (size) {
        0 -> ""
        1 -> appContext.getString(R.string.move_file_to_title_format, if (first().isNameEncrypted) "" else first().name)
        else -> appContext.getString(R.string.move_multiple_to, size, moveSuffix)
    }

    private val List<DriveLink>.moveSuffix: String get() = when {
        all { driveLink -> driveLink is DriveLink.File } -> appContext.getString(R.string.move_files)
        all { driveLink -> driveLink is DriveLink.Folder } -> appContext.getString(R.string.move_folders)
        else -> appContext.getString(R.string.move_items)
    }

    companion object {
        const val SELECTION_ID = "selectionId"
    }
}
