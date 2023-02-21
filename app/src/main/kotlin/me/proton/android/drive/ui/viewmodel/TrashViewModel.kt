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
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.effect.TrashEffect
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.screen.EmptyTrashIconState
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.trash.domain.usecase.GetPagedTrashedDriveLinks
import me.proton.core.drive.files.presentation.event.FilesViewEvent
import me.proton.core.drive.files.presentation.state.FilesViewState
import me.proton.core.drive.files.presentation.state.ListContentAppendingState
import me.proton.core.drive.files.presentation.state.ListContentState
import me.proton.core.drive.files.presentation.state.ListEffect
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import me.proton.core.drive.trash.domain.TrashManager
import me.proton.core.drive.trash.domain.usecase.GetEmptyTrashState
import me.proton.drive.android.settings.domain.entity.LayoutType
import me.proton.drive.android.settings.domain.usecase.GetLayoutType
import me.proton.drive.android.settings.domain.usecase.ToggleLayoutType
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
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
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val listContentState = MutableStateFlow<ListContentState>(ListContentState.Loading)
    private val listContentAppendingState = MutableStateFlow<ListContentAppendingState>(ListContentAppendingState.Idle)
    private val _listEffect = MutableSharedFlow<ListEffect>()
    private val _trashEffect = MutableSharedFlow<TrashEffect>()
    val initialViewState = FilesViewState(
        title = savedStateHandle.get(Screen.Files.FOLDER_NAME),
        titleResId = BasePresentation.string.title_trash,
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
    ) { sorting, contentState, appendingState, layoutType ->
        initialViewState.copy(
            sorting = sorting,
            listContentState = contentState,
            listContentAppendingState = appendingState,
            isGrid = layoutType == LayoutType.GRID,
        )
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    val driveLinks: Flow<PagingData<DriveLink>> = getTrashedDriveLinks(userId).cachedIn(viewModelScope)
    val listEffect: Flow<ListEffect> = _listEffect.asSharedFlow()
    val trashEffect: Flow<TrashEffect> = _trashEffect.asSharedFlow()
    val emptyTrashState = combine(listContentState, getEmptyTrashState(userId)) { content, state ->
        if (content !is ListContentState.Content) {
            EmptyTrashIconState.HIDDEN
        } else when (state) {
            TrashManager.EmptyTrashState.INACTIVE -> EmptyTrashIconState.VISIBLE
            TrashManager.EmptyTrashState.NO_FILES_TO_TRASH -> EmptyTrashIconState.HIDDEN
            TrashManager.EmptyTrashState.TRASHING -> EmptyTrashIconState.LOADING
        }
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private val emptyState = ListContentState.Empty(
        imageResId = BasePresentation.drawable.empty_trash,
        titleId = BasePresentation.string.title_empty_trash,
        descriptionResId = BasePresentation.string.description_empty_trash,
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
            emptyState = emptyState,
        ) { message ->
            viewModelScope.launch {
                _trashEffect.emit(TrashEffect.ShowSnackbar(message))
            }
        }
        override val onErrorAction = { retry() }
        override val onAppendErrorAction = { retry() }
        override val onMoreOptions = { driveLink: DriveLink -> navigateToFileOrFolderOptions(driveLink.id) }
        override val onToggleLayout = this@TrashViewModel::onToggleLayout
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
            _trashEffect.emit(TrashEffect.MoreOptions)
        }
    }

    private fun onToggleLayout() {
        viewModelScope.launch { toggleLayoutType(userId = userId, currentLayoutType = layoutType.value) }
    }
}
