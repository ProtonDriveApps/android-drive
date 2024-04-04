/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.drivelink.device.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.device.domain.extension.isNameEncrypted
import me.proton.core.drive.device.domain.extension.name
import me.proton.core.drive.drivelink.device.domain.usecase.GetDecryptedDevice
import me.proton.core.drive.drivelink.device.domain.usecase.RenameDevice
import me.proton.core.drive.drivelink.rename.presentation.RenameEffect
import me.proton.core.drive.drivelink.rename.presentation.viewmodel.RenameViewModel
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class RenameDeviceViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    savedStateHandle: SavedStateHandle,
    getDevice: GetDecryptedDevice,
    private val renameDevice: RenameDevice,
    private val broadcastMessages: BroadcastMessages,
) : RenameViewModel(appContext, savedStateHandle) {
    private val deviceId = DeviceId(savedStateHandle.require(KEY_DEVICE_ID))
    override val titleResId: Int get() = I18N.string.computers_rename_title

    private val unused = getDevice(userId, deviceId)
        .filterSuccessOrError()
        .map { deviceResult ->
            name.emit(
                savedStateHandle.get<String>(KEY_FILENAME)
                    ?: deviceResult.name
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override suspend fun doRenameFile(name: String) {
        renameDevice(
            userId = userId,
            deviceId = deviceId,
            name,
        )
            .onFailure { error ->
                error.log(LogTag.RENAME, "Cannot rename device: ${deviceId.id.logId()}")
                error.handle()
            }
            .onSuccess {
                _renameEffect.emit(RenameEffect.Dismiss)
                broadcastMessages(
                    userId = userId,
                    message = appContext.getString(I18N.string.computers_rename_success),
                    type = BroadcastMessage.Type.INFO,
                )
            }
    }

    private val DataResult<Device>.name get() =
        toResult()
            .getOrNull()
            ?.takeUnless { device -> device.isNameEncrypted }
            ?.name
            ?: ""

    companion object {
        const val KEY_DEVICE_ID = "deviceId"
    }
}
