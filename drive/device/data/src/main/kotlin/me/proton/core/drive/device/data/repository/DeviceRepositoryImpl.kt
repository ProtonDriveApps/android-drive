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

package me.proton.core.drive.device.data.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.device.domain.repository.DeviceRepository
import me.proton.core.drive.device.domain.usecase.UseSdkForDevices
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val legacy: DeviceRepositoryLegacy,
    private val sdk: DeviceRepositorySdk,
    private val useSdkForDevices: UseSdkForDevices,
) : DeviceRepository {

    override fun getDevicesFlow(
        userId: UserId,
        fromIndex: Int,
        count: Int,
    ): Flow<List<Device>> = legacy.getDevicesFlow(userId, fromIndex, count)

    override suspend fun fetchAndStoreDevices(userId: UserId): List<Device> =
        if (useSdkForDevices(userId).getOrElse { false }) {
            sdk.fetchAndStoreDevices(userId)
        } else {
            legacy.fetchAndStoreDevices(userId)
        }

    override fun getDeviceFlow(userId: UserId, deviceId: DeviceId): Flow<Device?> =
        legacy.getDeviceFlow(userId, deviceId)

    override suspend fun renameDevice(userId: UserId, deviceId: DeviceId, name: String) =
        legacy.renameDevice(userId, deviceId, name)
}
