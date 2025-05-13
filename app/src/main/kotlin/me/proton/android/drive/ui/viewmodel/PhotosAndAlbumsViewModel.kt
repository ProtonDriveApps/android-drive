/*
 * Copyright (c) 2025 Proton AG.
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import me.proton.android.drive.ui.viewevent.PhotosAndAlbumsViewEvent
import me.proton.android.drive.ui.viewstate.PhotosAndAlbumsViewState
import me.proton.android.drive.ui.viewstate.PhotosAndAlbumsViewState.Tab
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.volume.domain.usecase.HasPhotoVolume
import javax.inject.Inject

@HiltViewModel
class PhotosAndAlbumsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    hasPhotoVolume: HasPhotoVolume,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val selectedTab = MutableStateFlow(Tab.PHOTOS)

    val initialViewState: PhotosAndAlbumsViewState = PhotosAndAlbumsViewState(
        selectedTab = Tab.PHOTOS,
        isAlbumsTabVisible = false,
    )

    val viewState: Flow<PhotosAndAlbumsViewState> = combine(
        selectedTab,
        hasPhotoVolume(userId),
    ) { selectedTab, hasPhotoVolume ->
        initialViewState.copy(
            selectedTab = selectedTab,
            isAlbumsTabVisible = hasPhotoVolume,
        )
    }

    fun viewEvent(): PhotosAndAlbumsViewEvent = object : PhotosAndAlbumsViewEvent {
        override val onSelectTab = { selected: Tab ->
            selectedTab.tryEmit(selected)
            Unit
        }
    }
}
