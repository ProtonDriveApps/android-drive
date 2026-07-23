/*
 * Copyright (c) 2026 Proton AG.
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
import me.proton.core.drive.base.domain.provider.ProtonDriveClientProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.device.domain.extension.deviceUid
import me.proton.core.drive.device.domain.usecase.GetDevice
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.usecase.ValidateLinkNameSize
import javax.inject.Inject

class RenameDeviceSdk @Inject constructor(
    private val getDevice: GetDevice,
    private val protonDriveClientProvider: ProtonDriveClientProvider,
    private val updateEventAction: UpdateEventAction,
    private val validateLinkNameSize: ValidateLinkNameSize,
) {

    suspend operator fun invoke(userId: UserId, deviceId: DeviceId, name: String): Result<Unit> = coRunCatching {
        // TODO: Remove when SDK validates name
        val validatedName = validateLinkNameSize(name).getOrThrow()
        val device = getDevice(userId, deviceId).toResult().getOrThrow()
        updateEventAction(
            userId = userId,
            nodeUid = device.rootLinkId.nodeUid(device.volumeId),
        ) {
            protonDriveClientProvider
                .getOrCreate(userId)
                .getOrThrow()
                .renameDevice(
                    deviceUid = device.deviceUid,
                    name = validatedName,
                )
        }
    }
}
