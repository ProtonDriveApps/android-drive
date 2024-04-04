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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.drivelink.device.domain.usecase.GetDecryptedDevice
import me.proton.core.drive.drivelink.device.presentation.options.DeviceOptionEntry
import me.proton.core.drive.drivelink.device.presentation.options.RenameDeviceOption
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject

@HiltViewModel
class ComputerOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getDecryptedDevice: GetDecryptedDevice,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val deviceId = DeviceId(savedStateHandle.require(KEY_DEVICE_ID))
    val device: Flow<Device?> = getDecryptedDevice(
        userId = userId,
        deviceId = deviceId,
    )
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun entries(
        runAction: (suspend () -> Unit) -> Unit,
        navigateToRenameComputer: (DeviceId, FolderId) -> Unit,
    ): List<DeviceOptionEntry> = listOf(
        RenameDeviceOption { device ->
            runAction { navigateToRenameComputer(device.id, device.rootLinkId) }
        }
    )

    companion object {
        const val KEY_DEVICE_ID = "deviceId"
    }
}
