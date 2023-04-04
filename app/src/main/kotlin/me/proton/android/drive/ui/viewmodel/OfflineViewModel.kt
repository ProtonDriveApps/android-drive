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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.CombinedLoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.common.onClick
import me.proton.android.drive.ui.navigation.PagerType
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.viewevent.OfflineViewEvent
import me.proton.android.drive.ui.viewstate.OfflineViewState
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.entity.onProcessing
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.download.domain.usecase.GetDownloadProgress
import me.proton.core.drive.drivelink.list.domain.usecase.GetSortedDecryptedDriveLinks
import me.proton.core.drive.drivelink.offline.domain.usecase.GetDecryptedOfflineDriveLinks
import me.proton.core.drive.files.presentation.state.FilesViewState
import me.proton.core.drive.files.presentation.state.ListContentAppendingState
import me.proton.core.drive.files.presentation.state.ListContentState
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.android.settings.domain.entity.LayoutType
import me.proton.drive.android.settings.domain.usecase.GetLayoutType
import me.proton.drive.android.settings.domain.usecase.ToggleLayoutType
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
@ExperimentalCoroutinesApi
class OfflineViewModel @Inject constructor(
    getDriveLink: GetDecryptedDriveLink,
    private val getDriveLinks: GetSortedDecryptedDriveLinks,
    getSorting: GetSorting,
    getOfflineDriveLinks: GetDecryptedOfflineDriveLinks,
    private val getDownloadProgress: GetDownloadProgress,
    getLayoutType: GetLayoutType,
    private val toggleLayoutType: ToggleLayoutType,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val shareId = savedStateHandle.get<String>(Screen.Files.SHARE_ID)
    private val folderId = savedStateHandle.get<String>(Screen.Files.FOLDER_ID)?.let { folderId ->
        shareId?.let { FolderId(ShareId(userId, shareId), folderId) }
    }
    val driveLink: StateFlow<DriveLink.Folder?> = getDriveLink(userId, folderId, failOnDecryptionError = false)
        .map { result ->
            result
                .onSuccess { driveLink ->
                    CoreLogger.d(VIEW_MODEL, "drive node onSuccess")
                    return@map driveLink
                }
                .onProcessing { CoreLogger.d(VIEW_MODEL, "drive node onProcessing") }
                .onFailure { error -> error.log(VIEW_MODEL) }
            return@map null
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val driveLinks: Flow<List<DriveLink>> = flowOf(folderId).flatMapLatest { folderId ->
        if (folderId == null) {
            getOfflineDriveLinks(userId)
        } else {
            getDriveLinks(folderId)
                .map { driveLinksResult -> driveLinksResult.getOrNull() }
                .filterNotNull()
        }
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    private val isRootFolder: Boolean = folderId == null || folderId.id.isEmpty()

    val initialViewState = OfflineViewState(
        filesViewState = FilesViewState(
            title = savedStateHandle.get(Screen.Files.FOLDER_NAME),
            titleResId = BasePresentation.string.title_offline_available,
            navigationIconResId = CorePresentation.drawable.ic_arrow_back,
            drawerGesturesEnabled = false,
            sorting = Sorting.DEFAULT,
            listContentState = ListContentState.Loading,
            listContentAppendingState = ListContentAppendingState.Idle,
        )
    )

    private val layoutType = getLayoutType(userId).stateIn(viewModelScope, SharingStarted.Eagerly, LayoutType.DEFAULT)
    val viewState: Flow<OfflineViewState> = combine(
        driveLink,
        getSorting(userId),
        driveLinks,
        layoutType,
    ) { driveLink, sorting, driveLinks, layoutType ->
        initialViewState.copy(
            filesViewState = initialViewState.filesViewState.copy(
                title = if (isRootFolder) null else {
                    driveLink?.name ?: initialViewState.filesViewState.title
                },
                isTitleEncrypted = isRootFolder.not() && driveLink.isNameEncrypted,
                sorting = sorting,
                listContentState = driveLinks.toListContentState(),
                isGrid = layoutType == LayoutType.GRID,
            )
        )
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private fun List<DriveLink>?.toListContentState(): ListContentState = when {
        this == null -> ListContentState.Loading
        isNotEmpty() -> ListContentState.Content(false)
        else -> if (folderId == null) {
            ListContentState.Empty(
                BasePresentation.drawable.empty_offline,
                BasePresentation.string.title_empty_offline_available,
                BasePresentation.string.description_empty_offline_available
            )
        } else {
            ListContentState.Empty(
                BasePresentation.drawable.empty_folder,
                BasePresentation.string.title_empty_files,
                BasePresentation.string.description_empty_files,
            )
        }
    }

    fun viewEvent(
        navigateToFiles: (FolderId, String?) -> Unit,
        navigateToPreview: (PagerType, FileId) -> Unit,
        navigateToSortingDialog: (Sorting) -> Unit,
        navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
        navigateBack: () -> Unit,
    ): OfflineViewEvent = object : OfflineViewEvent {

        private val driveLinkShareFlow = MutableSharedFlow<DriveLink>(extraBufferCapacity = 1).also { flow ->
            viewModelScope.launch {
                flow.take(1).collect { driveLink ->
                    driveLink.onClick(
                        navigateToFolder = navigateToFiles,
                        navigateToPreview = { fileId ->
                            navigateToPreview(
                                if (folderId == null) PagerType.OFFLINE else PagerType.FOLDER,
                                fileId
                            )
                        }
                    )
                }
            }
        }

        override val onTopAppBarNavigation = {
            navigateBack()
        }
        override val onSorting = navigateToSortingDialog
        override val onDriveLink = { driveLink: DriveLink ->
            driveLinkShareFlow.tryEmit(driveLink)
            Unit
        }
        override val onLoadState = { _: CombinedLoadStates, _: Int -> }
        override val onMoreOptions = { driveLink: DriveLink -> navigateToFileOrFolderOptions(driveLink.id) }
        override val onToggleLayout = this@OfflineViewModel::onToggleLayout
    }

    fun getDownloadProgressFlow(driveLink: DriveLink): Flow<Percentage>? = if (driveLink is DriveLink.File) {
        getDownloadProgress(driveLink)
    } else {
        null
    }

    private fun onToggleLayout() {
        viewModelScope.launch { toggleLayoutType(userId = userId, currentLayoutType = layoutType.value) }
    }
}
