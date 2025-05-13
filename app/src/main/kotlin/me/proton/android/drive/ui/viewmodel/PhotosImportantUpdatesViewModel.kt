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
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.presentation.viewevent.PhotosImportantUpdatesViewEvent
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.log.LogTag.PHOTO
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.photo.domain.manager.PhotoShareMigrationManager
import me.proton.core.drive.photo.domain.repository.PhotoShareMigrationRepository
import javax.inject.Inject

@HiltViewModel
class PhotosImportantUpdatesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoShareMigrationManager: PhotoShareMigrationManager,
    private val repository: PhotoShareMigrationRepository,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    fun viewEvent(
        runAction: RunAction,
    ): PhotosImportantUpdatesViewEvent = object : PhotosImportantUpdatesViewEvent {
        override val onStart = { Unit.also { onStart(runAction) } }
        override val onRemindMeLater = { Unit.also { onRemindMeLater(runAction) } }
    }

    private fun onStart(runAction: RunAction) = viewModelScope.launch {
        photoShareMigrationManager.start(userId)
            .onFailure { error ->
                error.log(PHOTO, "Failed starting photo share migration")
            }
            .getOrNull()
        runAction {}
    }

    private fun onRemindMeLater(runAction: RunAction) = viewModelScope.launch {
        repository.setPhotosImportantUpdatesLastShown(userId, TimestampMs())
        runAction {}
    }
}
