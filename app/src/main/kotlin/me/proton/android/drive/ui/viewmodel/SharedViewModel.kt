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
import androidx.paging.CombinedLoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.common.onClick
import me.proton.android.drive.ui.effect.HomeEffect
import me.proton.android.drive.ui.effect.HomeTabViewModel
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.viewevent.SharedViewEvent
import me.proton.android.drive.ui.viewstate.SharedViewState
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.base.presentation.extension.logDefaultMessage
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.download.domain.usecase.GetDownloadProgress
import me.proton.core.drive.drivelink.shared.domain.usecase.GetDecryptedSharedDriveLinks
import me.proton.core.drive.files.presentation.state.FilesViewState
import me.proton.core.drive.files.presentation.state.ListContentAppendingState
import me.proton.core.drive.files.presentation.state.ListContentState
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.isSharedUrlExpired
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetMainShare
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.sorting.domain.usecase.GetSorting
import me.proton.drive.android.settings.domain.entity.LayoutType
import me.proton.drive.android.settings.domain.usecase.GetLayoutType
import me.proton.drive.android.settings.domain.usecase.ToggleLayoutType
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
@ExperimentalCoroutinesApi
@Suppress("StaticFieldLeak")
class SharedViewModel @Inject constructor(
    private val getMainShare: GetMainShare,
    private val getDriveLink: GetDecryptedDriveLink,
    getSorting: GetSorting,
    getSharedDriveLinks: GetDecryptedSharedDriveLinks,
    private val getDownloadProgress: GetDownloadProgress,
    getLayoutType: GetLayoutType,
    private val toggleLayoutType: ToggleLayoutType,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val configurationProvider: ConfigurationProvider,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle), HomeTabViewModel {
    private val _effects = MutableSharedFlow<HomeEffect>()
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    private val shareId = refreshTrigger.transformLatest {
        savedStateHandle.get<String>(Screen.Shared.SHARE_ID)?.takeIf { shareId -> shareId != "null" }?.let { shareId ->
            emit(ShareId(userId, shareId))
        } ?: emitAll(
            getMainShare(userId)
                .map { dataResult -> dataResult.onFailure { error -> error.log(VIEW_MODEL) }}
                .mapSuccessValueOrNull()
                .filterNotNull()
                .map { share -> share.id }
                .distinctUntilChanged()
        )
    }

    private val driveLinks: Flow<DataResult<List<DriveLink>>> = shareId.flatMapLatest { shareId ->
        getSharedDriveLinks(shareId, refresh = true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = DataResult.Processing(ResponseSource.Local))

    val driveLinksFlow = driveLinks.mapSuccessValueOrNull()
        .filterNotNull()
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    val initialViewState = SharedViewState(
        filesViewState = FilesViewState(
            title = savedStateHandle[Screen.Files.FOLDER_NAME],
            titleResId = BasePresentation.string.title_shared,
            navigationIconResId = CorePresentation.drawable.ic_proton_hamburger,
            drawerGesturesEnabled = true,
            sorting = Sorting.DEFAULT,
            listContentState = ListContentState.Loading,
            listContentAppendingState = ListContentAppendingState.Idle,
            isTextEnabled = { driveLink -> FilesViewState.defaultIsTextEnabled(driveLink) && !driveLink.isSharedUrlExpired },
        )
    )

    private val layoutType = getLayoutType(userId).stateIn(viewModelScope, SharingStarted.Eagerly, LayoutType.DEFAULT)
    val viewState: Flow<SharedViewState> = combine(
        getSorting(userId),
        driveLinks,
        layoutType,
    ) { sorting, driveLinks, layoutType ->
        initialViewState.copy(
            filesViewState = initialViewState.filesViewState.copy(
                sorting = sorting,
                listContentState = driveLinks.toListContentState(),
                isGrid = layoutType == LayoutType.GRID,
            )
        )
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private fun DataResult<List<DriveLink>>.toListContentState(): ListContentState = when (val cause = this) {
        is DataResult.Processing -> if (driveLinksFlow.replayCache.isNotEmpty()) {
            ListContentState.Content(true)
        } else {
            ListContentState.Loading
        }
        is DataResult.Success -> if (cause.value.isNotEmpty()) {
            ListContentState.Content(false)
        } else {
            ListContentState.Empty(
                BasePresentation.drawable.empty_links,
                BasePresentation.string.title_empty_links,
                BasePresentation.string.description_empty_links,
            )
        }
        is DataResult.Error -> ListContentState.Error(
            message = cause.logDefaultMessage(appContext, configurationProvider.useExceptionMessage, VIEW_MODEL),
            actionResId = BasePresentation.string.title_retry,
        )
    }

    override val homeEffect: Flow<HomeEffect>
        get() = _effects.asSharedFlow()

    fun viewEvent(
        navigateToFiles: (FolderId, String?) -> Unit,
        navigateToPreview: (FileId) -> Unit,
        navigateToSortingDialog: (Sorting) -> Unit,
        navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
    ): SharedViewEvent = object : SharedViewEvent {

        private val driveLinkShareFlow = MutableSharedFlow<DriveLink>(extraBufferCapacity = 1).also { flow ->
            viewModelScope.launch {
                flow.take(1).collect { driveLink ->
                    driveLink.onClick(navigateToFiles, navigateToPreview)
                }
            }
        }
        override val onTopAppBarNavigation = {
            viewModelScope.launch { _effects.emit(HomeEffect.OpenDrawer) }
            Unit
        }
        override val onSorting = navigateToSortingDialog
        override val onDriveLink = { driveLink: DriveLink ->
            driveLinkShareFlow.tryEmit(driveLink)
            Unit
        }
        override val onLoadState = { _: CombinedLoadStates, _: Int -> }
        override val onMoreOptions = { driveLink: DriveLink -> navigateToFileOrFolderOptions(driveLink.id) }
        override val onErrorAction = ::refresh
        override val onToggleLayout = this@SharedViewModel::onToggleLayout
    }

    fun driveLink(linkId: LinkId): Flow<DriveLink.File> = getDriveLink(linkId)
        .transformLatest { result ->
            result.onSuccess { driveLink ->
                if (driveLink is DriveLink.File) {
                    emit(driveLink)
                }
            }
        }

    fun getDownloadProgressFlow(driveLink: DriveLink): Flow<Percentage>? = if (driveLink is DriveLink.File) {
        getDownloadProgress(driveLink)
    } else {
        null
    }

    private fun onToggleLayout() {
        viewModelScope.launch { toggleLayoutType(userId = userId, currentLayoutType = layoutType.value) }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }
}
