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
import androidx.paging.CombinedLoadStates
import androidx.paging.PagingData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.common.onClick
import me.proton.android.drive.ui.effect.HomeEffect
import me.proton.android.drive.ui.effect.HomeTabViewModel
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.effect.ListEffect
import me.proton.core.drive.base.presentation.state.ListContentAppendingState
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.base.presentation.viewmodel.onLoadState
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.shared.domain.usecase.SharedDriveLinks
import me.proton.core.drive.drivelink.shared.presentation.entity.SharedItem
import me.proton.core.drive.drivelink.shared.presentation.viewevent.SharedViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewstate.SharedViewState
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.user.domain.entity.SharedLinkId
import kotlin.time.Duration.Companion.milliseconds

abstract class CommonSharedViewModel(
    savedStateHandle: SavedStateHandle,
    private val appContext: Context,
    private val configurationProvider: ConfigurationProvider,
    private val sharedDriveLinks: SharedDriveLinks,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle), HomeTabViewModel {
    private var fetchingJob: Job? = null
    private val listContentState = MutableStateFlow<ListContentState>(ListContentState.Loading)
    private val listContentAppendingState = MutableStateFlow<ListContentAppendingState>(
        ListContentAppendingState.Idle
    )
    abstract val driveLinks: Flow<PagingData<SharedItem>>
    protected abstract val emptyState: ListContentState.Empty
    private val _listEffect = MutableSharedFlow<ListEffect>()
    val listEffect: Flow<ListEffect>
        get() = _listEffect.asSharedFlow()
    private val _homeEffect = MutableSharedFlow<HomeEffect>()
    override val homeEffect: Flow<HomeEffect>
        get() = _homeEffect.asSharedFlow()
    val driveLinksMap: Flow<Map<String, DriveLink>> = sharedDriveLinks
        .getSharedDriveLinksMapFlow()
        .map { map -> map.mapKeys { (key, _) -> key.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val initialViewState = SharedViewState(
        listContentState = ListContentState.Loading,
        listContentAppendingState = ListContentAppendingState.Idle,
    )
    val viewState: Flow<SharedViewState> = combine(
        listContentState,
        listContentAppendingState
    ) { contentState, contentAppendingState ->
        initialViewState.copy(
            listContentState = contentState,
            listContentAppendingState = contentAppendingState,
        )
    }

    fun viewEvent(
        navigateToFiles: (FolderId, String?) -> Unit,
        navigateToPreview: (fileId: FileId) -> Unit,
        navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
    ): SharedViewEvent = object : SharedViewEvent {

        private val driveLinkShareFlow = MutableSharedFlow<DriveLink>(extraBufferCapacity = 1).also { flow ->
            viewModelScope.launch {
                flow.take(1).collect { driveLink ->
                    driveLink.onClick(navigateToFiles, navigateToPreview)
                }
            }
        }

        override val onLoadState: (CombinedLoadStates, Int) -> Unit = onLoadState(
            appContext = appContext,
            useExceptionMessage = configurationProvider.useExceptionMessage,
            listContentState = listContentState,
            listAppendContentState = listContentAppendingState,
            coroutineScope = viewModelScope,
            emptyState = emptyState,
        ) { message ->
            viewModelScope.launch {
                _homeEffect.emit(HomeEffect.ShowSnackbar(message))
            }
        }

        override val onDriveLink = { driveLink: DriveLink ->
            driveLinkShareFlow.tryEmit(driveLink)
            Unit
        }

        override val onScroll: (Set<LinkId>) -> Unit = this@CommonSharedViewModel::onScroll

        override val onRefresh = this@CommonSharedViewModel::onRefresh

        override val onMoreOptions = { driveLink: DriveLink -> navigateToFileOrFolderOptions(driveLink.id) }

        override val onErrorAction = this@CommonSharedViewModel::onErrorAction
    }

    private fun onScroll(driveLinkIds: Set<LinkId>) {
        if (driveLinkIds.isNotEmpty()) {
            fetchingJob?.cancel()
            fetchingJob = viewModelScope.launch {
                delay(300.milliseconds)
                sharedDriveLinks.load(driveLinkIds)
            }
        }
    }

    private fun onRefresh() {
        viewModelScope.launch {
            _listEffect.emit(ListEffect.REFRESH)
            val linkIds = getAllIds().getOrNull(SHARING, "Cannot get ids")
                .orEmpty().map { it.linkId }.toSet()
            sharedDriveLinks.refresh(linkIds).getOrNull(SHARING, "Cannot refresh drive links")
        }
    }

    private suspend fun retryList() {
        _listEffect.emit(ListEffect.RETRY)
    }

    private fun onErrorAction() {
        viewModelScope.launch {
            retryList()
        }
    }

    abstract suspend fun getAllIds(): Result<List<SharedLinkId>>
}
