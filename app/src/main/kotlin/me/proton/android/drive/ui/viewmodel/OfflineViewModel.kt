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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
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
import me.proton.android.drive.usecase.OpenProtonDocumentInBrowser
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.entity.onProcessing
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.effect.ListEffect
import me.proton.core.drive.base.presentation.state.ListContentAppendingState
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.base.presentation.viewmodel.onLoadState
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.download.domain.usecase.GetDownloadProgress
import me.proton.core.drive.drivelink.offline.domain.usecase.GetPagedOfflineDriveLinksList
import me.proton.core.drive.feature.flag.domain.usecase.IsDownloadManagerEnabled
import me.proton.core.drive.files.presentation.state.FilesViewState
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.android.settings.domain.entity.LayoutType
import me.proton.drive.android.settings.domain.usecase.GetLayoutType
import me.proton.drive.android.settings.domain.usecase.ToggleLayoutType
import javax.inject.Inject
import me.proton.core.drive.base.domain.extension.flowOf as baseFlowOf
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
@ExperimentalCoroutinesApi
@Suppress("StaticFieldLeak")
class OfflineViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    getDriveLink: GetDecryptedDriveLink,
    getSorting: GetSorting,
    getPagedOfflineDriveLinksList: GetPagedOfflineDriveLinksList,
    private val getDownloadProgress: GetDownloadProgress,
    getLayoutType: GetLayoutType,
    private val toggleLayoutType: ToggleLayoutType,
    private val configurationProvider: ConfigurationProvider,
    private val openProtonDocumentInBrowser: OpenProtonDocumentInBrowser,
    private val broadcastMessages: BroadcastMessages,
    private val isDownloadManagerEnabled: IsDownloadManagerEnabled,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val shareId = savedStateHandle.get<String>(Screen.Files.SHARE_ID)
    private val folderId = savedStateHandle.get<String>(Screen.Files.FOLDER_ID)?.let { folderId ->
        shareId?.let { FolderId(ShareId(userId, shareId), folderId) }
    }
    private val isDownloadEnabled: StateFlow<Boolean> = baseFlowOf {
        isDownloadManagerEnabled(userId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
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
    val driveLinks: Flow<PagingData<DriveLink>> = flowOf(folderId).flatMapLatest { folderId ->
        if (folderId == null) {
            getPagedOfflineDriveLinksList(userId)
        } else {
            getPagedOfflineDriveLinksList(folderId)
        }
    }
        .cachedIn(viewModelScope)
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    private val isRootFolder: Boolean = folderId == null || folderId.id.isEmpty()
    private val _listEffect = MutableSharedFlow<ListEffect>()
    private val listContentState = MutableStateFlow<ListContentState>(ListContentState.Loading)
    private val listContentAppendingState = MutableStateFlow<ListContentAppendingState>(ListContentAppendingState.Idle)

    val initialViewState = OfflineViewState(
        filesViewState = FilesViewState(
            title = savedStateHandle.get(Screen.Files.FOLDER_NAME),
            titleResId = I18N.string.title_offline_available,
            navigationIconResId = CorePresentation.drawable.ic_arrow_back,
            drawerGesturesEnabled = false,
            sorting = Sorting.DEFAULT,
            listContentState = listContentState.value,
            listContentAppendingState = listContentAppendingState.value,
        )
    )

    private val layoutType = getLayoutType(userId).stateIn(viewModelScope, SharingStarted.Eagerly, LayoutType.DEFAULT)
    val viewState: Flow<OfflineViewState> = combine(
        driveLink,
        getSorting(userId),
        listContentState,
        listContentAppendingState,
        layoutType,
    ) { driveLink, sorting, contentState, appendingContentState, layoutType ->
        val listContentState = when (contentState) {
            is ListContentState.Empty -> contentState.copy(
                imageResId = emptyStateImageResId,
            )
            else -> contentState
        }
        initialViewState.copy(
            filesViewState = initialViewState.filesViewState.copy(
                title = if (isRootFolder) null else {
                    driveLink?.name ?: initialViewState.filesViewState.title
                },
                isTitleEncrypted = isRootFolder.not() && driveLink.isNameEncrypted,
                sorting = sorting,
                listContentState = listContentState,
                listContentAppendingState = appendingContentState,
                isGrid = layoutType == LayoutType.GRID,
            )
        )
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private val emptyState = ListContentState.Empty(
        imageResId = emptyStateImageResId,
        titleId = if (isRootFolder) {
            I18N.string.title_empty_offline_available
        } else {
            I18N.string.title_empty_folder
        },
        descriptionResId = if (isRootFolder) {
            I18N.string.description_empty_offline_available
        } else {
            I18N.string.description_empty_folder
        },
    )

    private val emptyStateImageResId: Int get() = if (isRootFolder) {
        getThemeDrawableId(
            light = BasePresentation.drawable.empty_offline_light,
            dark = BasePresentation.drawable.empty_offline_dark,
            dayNight = BasePresentation.drawable.empty_offline_daynight,
        )
    } else {
        getThemeDrawableId(
            light = BasePresentation.drawable.empty_folder_light,
            dark = BasePresentation.drawable.empty_folder_dark,
            dayNight = BasePresentation.drawable.empty_folder_daynight,
        )
    }

    fun viewEvent(
        navigateToFiles: (FolderId, String?) -> Unit,
        navigateToPreview: (PagerType, FileId) -> Unit,
        navigateToAlbum: (AlbumId) -> Unit,
        navigateToSortingDialog: (Sorting) -> Unit,
        navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
        navigateToAlbumOptions: (AlbumId) -> Unit,
        navigateBack: () -> Unit,
        lifecycle: Lifecycle,
    ): OfflineViewEvent = object : OfflineViewEvent {

        private val driveLinkShareFlow = MutableSharedFlow<DriveLink>(extraBufferCapacity = 1).also { flow ->
            viewModelScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    flow.take(1).collect { driveLink ->
                        driveLink.onClick(
                            navigateToFolder = navigateToFiles,
                            navigateToPreview = { fileId ->
                                navigateToPreview(
                                    if (isRootFolder) PagerType.OFFLINE else PagerType.FOLDER,
                                    fileId
                                )
                            },
                            navigateToAlbum = navigateToAlbum,
                        )
                    }
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
        override val onLoadState = onLoadState(
            appContext = appContext,
            useExceptionMessage = configurationProvider.useExceptionMessage,
            listContentState = listContentState,
            listAppendContentState = listContentAppendingState,
            coroutineScope = viewModelScope,
            emptyState = MutableStateFlow(emptyState),
        ) { }
        override val onMoreOptions = { driveLink: DriveLink ->
            if (driveLink is DriveLink.Album) {
                navigateToAlbumOptions(driveLink.id)
            } else {
                navigateToFileOrFolderOptions(driveLink.id)
            }
        }
        override val onToggleLayout = this@OfflineViewModel::onToggleLayout
        override val onErrorAction = { retryList() }
        override val onAppendErrorAction = { retryList() }
    }

    val listEffect: Flow<ListEffect>
        get() = _listEffect.asSharedFlow()

    fun getDownloadProgressFlow(driveLink: DriveLink): Flow<Percentage>? = if (driveLink is DriveLink.File) {
        getDownloadProgress(driveLink, isDownloadEnabled.value)
    } else {
        null
    }

    private fun onToggleLayout() {
        viewModelScope.launch { toggleLayoutType(userId = userId, currentLayoutType = layoutType.value) }
    }

    private fun retryList() {
        viewModelScope.launch {
            _listEffect.emit(ListEffect.RETRY)
        }
    }

    private suspend fun openDocument(driveLink: DriveLink.File) =
        openProtonDocumentInBrowser(driveLink)
            .onFailure { error ->
                error.log(VIEW_MODEL, "Open document failed")
                broadcastMessages(
                    userId = userId,
                    message = error.getDefaultMessage(
                        context = appContext,
                        useExceptionMessage = configurationProvider.useExceptionMessage,
                    ),
                    type = BroadcastMessage.Type.ERROR,
                )
            }
}
