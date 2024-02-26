/*
 * Copyright (c) 2024 Proton AG.
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
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.common.onClick
import me.proton.android.drive.ui.effect.HomeEffect
import me.proton.android.drive.ui.effect.HomeTabViewModel
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.data.extension.logDefaultMessage
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.list.domain.usecase.GetPagedDriveLinksList
import me.proton.core.drive.files.presentation.event.FilesViewEvent
import me.proton.core.drive.files.presentation.state.FilesViewState
import me.proton.core.drive.files.presentation.state.ListContentAppendingState
import me.proton.core.drive.files.presentation.state.ListContentState
import me.proton.core.drive.files.presentation.state.ListEffect
import me.proton.core.drive.i18n.R
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.android.settings.domain.entity.LayoutType
import me.proton.drive.android.settings.domain.usecase.GetLayoutType
import me.proton.drive.android.settings.domain.usecase.ToggleLayoutType
import javax.inject.Inject
import me.proton.core.drive.drivelink.device.presentation.R as DriveLinkDevicePresentation
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SyncedFoldersViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val savedStateHandle: SavedStateHandle,
    private val configurationProvider: ConfigurationProvider,
    private val toggleLayoutType: ToggleLayoutType,
    private val getPagedDriveLinks: GetPagedDriveLinksList,
    getDriveLink: GetDecryptedDriveLink,
    getLayoutType: GetLayoutType,
    getSorting: GetSorting,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle), HomeTabViewModel {
    private val shareId = savedStateHandle.get<String>(SHARE_ID)
    private val folderId = savedStateHandle.get<String>(FOLDER_ID)?.let { folderId ->
        shareId?.let { FolderId(ShareId(userId, shareId), folderId) }
    }
    private val folderName = savedStateHandle.get<String>(FOLDER_NAME)

    private val _listEffect = MutableSharedFlow<ListEffect>()
    private val _homeEffect = MutableSharedFlow<HomeEffect>()
    private val listContentState = MutableStateFlow<ListContentState>(ListContentState.Loading)
    private val listContentAppendingState = MutableStateFlow<ListContentAppendingState>(ListContentAppendingState.Idle)
    val sorting: Flow<Sorting> = getSorting(userId).shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    private val layoutType = getLayoutType(userId).stateIn(viewModelScope, SharingStarted.Eagerly, LayoutType.DEFAULT)

    val initialViewState = FilesViewState(
        title = folderName,
        titleResId = R.string.title_offline_available,
        navigationIconResId = me.proton.core.presentation.R.drawable.ic_arrow_back,
        drawerGesturesEnabled = true,
        sorting = Sorting.DEFAULT,
        listContentState = listContentState.value,
        listContentAppendingState = listContentAppendingState.value,
        isDriveLinkMoreOptionsEnabled = false,
    )
    val driveLink: StateFlow<DriveLink.Folder?> =
        getDriveLink(userId, folderId, failOnDecryptionError = false)
            .filterSuccessOrError()
            .map { result ->
                result
                    .onSuccess { driveLink ->
                        CoreLogger.d(VIEW_MODEL, "drive link onSuccess")
                        return@map driveLink
                    }
                    .onFailure { error ->
                        listContentState.value = ListContentState.Error(
                            if (error.cause is NoSuchElementException) {
                                appContext.getString(I18N.string.computers_synced_folders_no_folder)
                            } else {
                                error.logDefaultMessage(
                                    context = appContext,
                                    useExceptionMessage = configurationProvider.useExceptionMessage,
                                    tag = VIEW_MODEL,
                                )
                            }
                        )
                    }
                return@map null
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val driveLinks: Flow<PagingData<DriveLink>> =
        driveLink.filterNotNull()
            .flatMapLatest { folder -> getPagedDriveLinks(folder.id) }
            .cachedIn(viewModelScope)

    val viewState: Flow<FilesViewState> = combine(
        driveLink,
        sorting,
        listContentState,
        layoutType,
    ) { driveLink, sorting, listContentState, layoutType ->
        initialViewState.copy(
            title = folderName ?: driveLink?.name,
            isTitleEncrypted = folderName == null && driveLink.isNameEncrypted,
            sorting = sorting,
            listContentState = listContentState,
            isGrid = layoutType == LayoutType.GRID,
        )
    }

    private val emptyState = ListContentState.Empty(
        imageResId = getThemeDrawableId(
            light = DriveLinkDevicePresentation.drawable.empty_synced_folders_light,
            dark = DriveLinkDevicePresentation.drawable.empty_synced_folders_dark,
            dayNight = DriveLinkDevicePresentation.drawable.empty_synced_folders_daynight,
        ),
        titleId = I18N.string.computers_synced_folders_empty_title,
        descriptionResId = I18N.string.computers_synced_folders_empty_description,
    )

    fun viewEvent(
        navigateToFiles: (FolderId, String?) -> Unit,
        navigateToSortingDialog: (Sorting) -> Unit,
        navigateBack: () -> Unit,
    ): FilesViewEvent = object : FilesViewEvent {

        private val driveLinkShareFlow = MutableSharedFlow<DriveLink>(extraBufferCapacity = 1).also { flow ->
            viewModelScope.launch {
                flow.take(1).collect { driveLink ->
                    driveLink.onClick(
                        navigateToFolder = navigateToFiles,
                        navigateToPreview = { error("Preview is not supported here") }
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
        override val onLoadState = onLoadState(
            appContext = appContext,
            useExceptionMessage = configurationProvider.useExceptionMessage,
            listContentState = listContentState,
            listAppendContentState = listContentAppendingState,
            coroutineScope = viewModelScope,
            emptyState = emptyState,
        ) { }
        override val onMoreOptions = { _: DriveLink -> error("More options is not supported here") }
        override val onToggleLayout = this@SyncedFoldersViewModel::onToggleLayout
        override val onErrorAction = { retryList() }
        override val onAppendErrorAction = { retryList() }
    }

    val listEffect: Flow<ListEffect>
        get() = _listEffect.asSharedFlow()

    override val homeEffect: Flow<HomeEffect>
        get() = _homeEffect.asSharedFlow()

    fun refresh() {
        viewModelScope.launch {
            _listEffect.emit(ListEffect.REFRESH)
        }
    }

    private fun onToggleLayout() {
        viewModelScope.launch { toggleLayoutType(userId = userId, currentLayoutType = layoutType.value) }
    }

    private fun retryList() {
        viewModelScope.launch {
            _listEffect.emit(ListEffect.RETRY)
        }
    }

    companion object {
        const val SHARE_ID = "shareId"
        const val FOLDER_ID = "folderId"
        const val FOLDER_NAME = "folderName"
    }
}
