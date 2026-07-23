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
import kotlinx.coroutines.flow.toList
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ProtonDriveClientProvider
import me.proton.core.drive.device.data.db.DeviceDatabase
import me.proton.core.drive.device.data.extension.toDevice
import me.proton.core.drive.device.data.extension.toDeviceEntity
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.device.domain.repository.DeviceRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class DeviceRepositorySdk @Inject constructor(
    private val protonDriveClientProvider: ProtonDriveClientProvider,
    private val db: DeviceDatabase,
    private val getShare: GetShare,
    private val getLink: GetLink,
) : DeviceRepository {

    override fun getDevicesFlow(
        userId: UserId,
        fromIndex: Int,
        count: Int,
    ): Flow<List<Device>> = throw UnsupportedOperationException("Use legacy implementation to get devices")

    override suspend fun fetchAndStoreDevices(userId: UserId): List<Device> {
        val deviceEntities = protonDriveClientProvider.getOrCreate(userId).getOrThrow()
            .enumerateDevices()
            .toList()
            .map { device -> device.toDeviceEntity(userId) }

        deviceEntities.forEach { deviceEntity ->
            fetchShareAndFolder(
                FolderId(ShareId(userId, deviceEntity.shareId), deviceEntity.linkId)
            )
        }
        db.inTransaction {
            db.deviceDao.deleteAll(userId)
            db.deviceDao.insertOrIgnore(*deviceEntities.toTypedArray())
        }
        return deviceEntities.map { deviceEntity -> deviceEntity.toDevice() }
    }

    override fun getDeviceFlow(userId: UserId, deviceId: DeviceId): Flow<Device?> =
        throw UnsupportedOperationException("Use legacy implementation to get device")

    override suspend fun renameDevice(userId: UserId, deviceId: DeviceId, name: String) =
        throw UnsupportedOperationException("SDK should be call directly in a use case")

    private suspend fun fetchShareAndFolder(folderId: FolderId) {
        getShare(folderId.shareId).toResult().getOrThrow()
        getLink(folderId).toResult().getOrThrow()
    }
}
