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
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.effect.HomeEffect
import me.proton.android.drive.ui.effect.HomeTabViewModel
import me.proton.android.drive.ui.viewevent.PhotosAndAlbumsViewEvent
import me.proton.android.drive.ui.viewstate.PhotosAndAlbumsViewState
import me.proton.android.drive.ui.viewstate.PhotosAndAlbumsViewState.Tab
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import javax.inject.Inject

@HiltViewModel
class PhotosAndAlbumsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val selectedTab = MutableStateFlow(Tab.PHOTOS)

    val initialViewState: PhotosAndAlbumsViewState = PhotosAndAlbumsViewState(
        selectedTab = Tab.PHOTOS,
    )

    val viewState: Flow<PhotosAndAlbumsViewState> = selectedTab.map { selectedTab ->
        initialViewState.copy(
            selectedTab = selectedTab,
        )
    }

    fun viewEvent(): PhotosAndAlbumsViewEvent = object : PhotosAndAlbumsViewEvent {
        override val onSelectTab = { selected: Tab ->
            selectedTab.tryEmit(selected)
            Unit
        }
    }
}
