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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.viewevent.ConfirmEmptyTrashViewEvent
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag.TRASH
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.trash.domain.usecase.EmptyTrash
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

@HiltViewModel
@ExperimentalCoroutinesApi
class ConfirmEmptyTrashDialogViewModel @Inject constructor(
    private val emptyTrash: EmptyTrash,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val volumeId: VolumeId = VolumeId(savedStateHandle.require(VOLUME_ID))

    fun viewEvent(dismiss: () -> Unit) = object : ConfirmEmptyTrashViewEvent {
        override val onConfirm = {
            viewModelScope.launch {
                emptyTrash(userId, volumeId).onFailure { error ->
                    error.log(TRASH, "Cannot empty trash volumeId: ${volumeId.id.logId()}")
                }
                dismiss()
            }
            Unit
        }
    }


    companion object {
        const val VOLUME_ID = "volumeId"
    }
}
