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
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.effect.SnackbarEffect
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.R
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.list.domain.usecase.GetPagedDriveLinksList
import me.proton.core.drive.files.presentation.state.FilesViewState
import me.proton.core.drive.files.presentation.state.ListContentAppendingState
import me.proton.core.drive.files.presentation.state.ListContentState
import me.proton.core.drive.files.presentation.state.ListEffect
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.android.drive.R as Presentation

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("StaticFieldLeak")
abstract class HostFilesViewModel(
    protected val appContext: Context,
    getDriveLink: GetDecryptedDriveLink,
    getPagedDriveLinks: GetPagedDriveLinksList,
    savedStateHandle: SavedStateHandle,
    protected val configurationProvider: ConfigurationProvider,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    protected abstract val parentId: FolderId?
    protected val _listEffect = MutableSharedFlow<ListEffect>()
    val listEffect: SharedFlow<ListEffect>
        get() = _listEffect
    protected val _snackbarEffect = MutableSharedFlow<SnackbarEffect>()
    val snackbarEffect: SharedFlow<SnackbarEffect>
        get() = _snackbarEffect
    protected val listContentState = MutableStateFlow<ListContentState>(ListContentState.Loading)
    protected val listContentAppendingState =
        MutableStateFlow<ListContentAppendingState>(ListContentAppendingState.Idle)
    protected val initialFilesViewState = FilesViewState(
        title = null,
        titleResId = Presentation.string.title_my_files,
        sorting = Sorting.DEFAULT,
        navigationIconResId = 0,
        drawerGesturesEnabled = false,
        listContentState = listContentState.value,
        listContentAppendingState = listContentAppendingState.value,
        showHeader = false,
        isSelectingDestination = true,
        isClickEnabled = { driveLink -> driveLink is Folder },
        isTextEnabled = { driveLink -> FilesViewState.defaultIsTextEnabled(driveLink) && driveLink is Folder }
    )
    protected val trigger = MutableSharedFlow<FolderId?>(replay = 1).apply { tryEmit(parentId) }
    protected val parentLink = trigger
        .transformLatest { folderId ->
            listContentState.value = ListContentState.Loading
            emitAll(getDriveLink(userId, folderId = folderId))
        }
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val driveLinks = parentLink
        .transformLatest { driveLink ->
            emit(PagingData.empty())
            if (driveLink != null) {
                emitAll(getPagedDriveLinks(folderId = driveLink.id))
            }
        }.cachedIn(viewModelScope)
    protected val emptyState = ListContentState.Empty(
        imageResId = R.drawable.empty_folder,
        titleId = R.string.title_empty_files,
        descriptionResId = R.string.description_empty_files,
    )
    protected val onSorting: (Sorting) -> Unit = { _: Sorting -> }
    protected val onLoadState = onLoadState(
        appContext = appContext,
        useExceptionMessage = configurationProvider.useExceptionMessage,
        listContentState = listContentState,
        listAppendContentState = listContentAppendingState,
        coroutineScope = viewModelScope,
        emptyState = emptyState,
    ) { message ->
        viewModelScope.launch {
            showError(message)
        }
    }
    protected val onRetry = { retry() }
    protected fun onTopAppBarNavigation(navigateBack: () -> Unit): () -> Unit = {
        Unit.also {
            if (parentLink.value?.parentId == null) {
                navigateBack()
            } else {
                viewModelScope.launch {
                    trigger.emit(parentLink.value?.parentId)
                }
            }
        }
    }

    protected suspend fun showError(message: String) {
        _snackbarEffect.emit(SnackbarEffect.ShowSnackbar.Message(message))
    }

    protected suspend fun showError(@StringRes messageResId: Int) {
        _snackbarEffect.emit(SnackbarEffect.ShowSnackbar.Resource(messageResId))
    }

    protected suspend fun showInfo(message: String) {
        _snackbarEffect.emit(SnackbarEffect.ShowSnackbar.Message(message, ProtonSnackbarType.NORM))
    }

    protected suspend fun showInfo(@StringRes messageResId: Int) {
        _snackbarEffect.emit(SnackbarEffect.ShowSnackbar.Resource(messageResId, ProtonSnackbarType.NORM))
    }

    open fun onCreateFolder(
        navigateToCreateFolder: (FolderId) -> Unit,
    ) {
        parentLink.value?.let { folder -> navigateToCreateFolder(folder.id) }
    }

    private fun retry() {
        viewModelScope.launch {
            _listEffect.emit(ListEffect.RETRY)
        }
    }
}
