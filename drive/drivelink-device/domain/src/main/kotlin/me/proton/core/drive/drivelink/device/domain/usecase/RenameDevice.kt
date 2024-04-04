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

package me.proton.core.drive.drivelink.device.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.device.domain.extension.name
import me.proton.core.drive.device.domain.usecase.GetDevice
import me.proton.core.drive.device.domain.usecase.RenameDevice
import me.proton.core.drive.drivelink.rename.domain.usecase.RenameLink
import me.proton.core.drive.link.domain.usecase.ValidateLinkName
import javax.inject.Inject

class RenameDevice @Inject constructor(
    private val getDevice: GetDevice,
    private val renameDevice: RenameDevice,
    private val renameLink: RenameLink,
    private val configurationProvider: ConfigurationProvider,
) {

    suspend operator fun invoke(userId: UserId, deviceId: DeviceId, name: String): Result<Unit> = coRunCatching {
        val deviceName = validateDeviceName(name)
        val device = getDevice(userId, deviceId).toResult().getOrThrow()
        renameLink(
            rootFolderId = device.rootLinkId,
            folderName = deviceName,
            shouldValidateName = false,
        ).getOrThrow()
        if (device.name.isNotEmpty()) {
            // Non-empty device name should be replaced by empty one, as real device name is part of device share
            // root folder
            renameDevice(userId, deviceId, DEFAULT_DEVICE_NAME).getOrThrow()
        }
    }

    private fun validateDeviceName(name: String): String {
        val trimmedName = name.trim()
        val maxLength = configurationProvider.linkMaxNameLength
        when {
            trimmedName.isEmpty() -> throw ValidateLinkName.Invalid.Empty
            trimmedName.length > maxLength -> throw ValidateLinkName.Invalid.ExceedsMaxLength(maxLength)
        }
        return trimmedName
    }

    companion object {
        private const val DEFAULT_DEVICE_NAME = ""
    }
}
