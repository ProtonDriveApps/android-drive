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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.effect.TrashEffect
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.screen.EmptyTrashIconState
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.effect.ListEffect
import me.proton.core.drive.base.presentation.state.ListContentAppendingState
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.base.presentation.viewmodel.onLoadState
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.trash.domain.usecase.GetPagedTrashedDriveLinks
import me.proton.core.drive.files.presentation.event.FilesViewEvent
import me.proton.core.drive.files.presentation.state.FilesViewState
import me.proton.core.drive.files.presentation.state.VolumeEntry
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import me.proton.core.drive.trash.domain.TrashManager
import me.proton.core.drive.trash.domain.usecase.GetEmptyTrashState
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.usecase.GetVolumes
import me.proton.drive.android.settings.domain.entity.LayoutType
import me.proton.drive.android.settings.domain.usecase.GetLayoutType
import me.proton.drive.android.settings.domain.usecase.ToggleLayoutType
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
@ExperimentalCoroutinesApi
@Suppress("StaticFieldLeak")
class TrashViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    getTrashedDriveLinks: GetPagedTrashedDriveLinks,
    getSorting: GetSorting,
    getLayoutType: GetLayoutType,
    getEmptyTrashState: GetEmptyTrashState,
    savedStateHandle: SavedStateHandle,
    private val toggleLayoutType: ToggleLayoutType,
    private val configurationProvider: ConfigurationProvider,
    getVolumes: GetVolumes,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val volumeIdFlow = MutableStateFlow<VolumeId?>(null)

    private val listContentState = MutableStateFlow<ListContentState>(ListContentState.Loading)
    private val listContentAppendingState = MutableStateFlow<ListContentAppendingState>(ListContentAppendingState.Idle)
    private val _listEffect = MutableSharedFlow<ListEffect>()
    private val _trashEffect = MutableSharedFlow<TrashEffect>()
    val initialViewState = FilesViewState(
        title = savedStateHandle.get(Screen.Files.FOLDER_NAME),
        titleResId = I18N.string.common_trash,
        sorting = Sorting.DEFAULT,
        navigationIconResId = CorePresentation.drawable.ic_arrow_back,
        drawerGesturesEnabled = true,
        listContentState = listContentState.value,
        listContentAppendingState = listContentAppendingState.value,
    )
    private val layoutType = getLayoutType(userId).stateIn(viewModelScope, SharingStarted.Eagerly, LayoutType.DEFAULT)
    val viewState: Flow<FilesViewState> = combine(
        getSorting(userId),
        listContentState,
        listContentAppendingState,
        layoutType,
        getVolumes(userId).mapSuccessValueOrNull(),
    ) { sorting, contentState, appendingState, layoutType, volumes ->
        val listContentState = when (contentState) {
            is ListContentState.Empty -> contentState.copy(
                imageResId = emptyStateImageResId,
            )
            else -> contentState
        }
        if (volumeIdFlow.value == null) {
            volumeIdFlow.emit(
                volumes?.firstOrNull { volume -> volume.type == Volume.Type.REGULAR }?.id
                    ?: volumes?.firstOrNull { volume -> volume.type == Volume.Type.PHOTO }?.id
            )
        }
        initialViewState.copy(
            sorting = sorting,
            listContentState = listContentState,
            listContentAppendingState = appendingState,
            isGrid = layoutType == LayoutType.GRID,
            volumesEntries = volumes?.takeIf { it.size > 1 }
                .orEmpty()
                .sortedBy { volume -> volume.type }
                .map { volume ->
                    VolumeEntry(
                        id = volume.id,
                        title = when (volume.type) {
                            Volume.Type.UNKNOWN -> I18N.string.common_unknown
                            Volume.Type.REGULAR -> I18N.string.title_files
                            Volume.Type.PHOTO -> I18N.string.photos_title
                        },
                        isSelected = volume.id == volumeIdFlow.value
                    )
                },
        )
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    val driveLinks: Flow<PagingData<DriveLink>> = volumeIdFlow.filterNotNull().transformLatest { volumeId ->
        emitAll(getTrashedDriveLinks(userId, volumeId))
    }.cachedIn(viewModelScope)
    val listEffect: Flow<ListEffect> = _listEffect.asSharedFlow()
    val trashEffect: Flow<TrashEffect> = _trashEffect.asSharedFlow()
    val emptyTrashState = combine(listContentState, volumeIdFlow.filterNotNull().transformLatest { volumeId ->
        emitAll(getEmptyTrashState(userId, volumeId))
    }) { content, state ->
        if (content !is ListContentState.Content) {
            EmptyTrashIconState.HIDDEN
        } else when (state) {
            TrashManager.EmptyTrashState.INACTIVE -> EmptyTrashIconState.VISIBLE
            TrashManager.EmptyTrashState.NO_FILES_TO_TRASH -> EmptyTrashIconState.HIDDEN
            TrashManager.EmptyTrashState.TRASHING -> EmptyTrashIconState.LOADING
        }
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private val emptyState = ListContentState.Empty(
        imageResId = emptyStateImageResId,
        titleId = I18N.string.title_empty_trash,
        descriptionResId = I18N.string.description_empty_trash,
    )
    private val emptyStateImageResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.empty_trash_light,
        dark = BasePresentation.drawable.empty_trash_dark,
        dayNight = BasePresentation.drawable.empty_trash_daynight,
    )

    fun viewEvent(
        navigateBack: () -> Unit,
        navigateToSortingDialog: (Sorting) -> Unit,
        navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
    ): FilesViewEvent = object : FilesViewEvent {
        override val onTopAppBarNavigation = navigateBack
        override val onSorting = navigateToSortingDialog
        override val onDriveLink: ((DriveLink) -> Unit)? = null
        override val onLoadState = onLoadState(
            appContext = appContext,
            useExceptionMessage = configurationProvider.useExceptionMessage,
            listContentState = listContentState,
            listAppendContentState = listContentAppendingState,
            coroutineScope = viewModelScope,
            emptyState = MutableStateFlow(emptyState),
        ) { message ->
            viewModelScope.launch {
                _trashEffect.emit(TrashEffect.ShowSnackbar(message))
            }
        }
        override val onErrorAction = { retry() }
        override val onAppendErrorAction = { retry() }
        override val onMoreOptions = { driveLink: DriveLink -> navigateToFileOrFolderOptions(driveLink.id) }
        override val onToggleLayout = this@TrashViewModel::onToggleLayout
        override val onTab = { volumeEntry: VolumeEntry ->
            volumeIdFlow.tryEmit(volumeEntry.id)
            Unit
        }
    }

    private fun retry() {
        viewModelScope.launch {
            _listEffect.emit(ListEffect.RETRY)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _listEffect.emit(ListEffect.REFRESH)
        }
    }

    fun onMoreOptionsClicked() {
        viewModelScope.launch {
            volumeIdFlow.firstOrNull()?.let { volumeId ->
                _trashEffect.emit(TrashEffect.MoreOptions(volumeId))
            }
        }
    }

    private fun onToggleLayout() {
        viewModelScope.launch { toggleLayoutType(userId = userId, currentLayoutType = layoutType.value) }
    }
}
