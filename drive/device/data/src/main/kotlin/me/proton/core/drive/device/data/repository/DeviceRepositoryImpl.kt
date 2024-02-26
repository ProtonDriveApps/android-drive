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

package me.proton.core.drive.device.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.device.data.api.DeviceApiDataSource
import me.proton.core.drive.device.data.db.DeviceDatabase
import me.proton.core.drive.device.data.extension.toDevice
import me.proton.core.drive.device.data.extension.toDeviceEntity
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.repository.DeviceRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val api: DeviceApiDataSource,
    private val db: DeviceDatabase,
    private val getShare: GetShare,
    private val getLink: GetLink,
) : DeviceRepository {

    override fun getDevicesFlow(
        userId: UserId,
        fromIndex: Int,
        count: Int,
    ): Flow<List<Device>> = db.deviceDao.getDevicesFlow(
        userId = userId,
        limit = count,
        offset = fromIndex,
    ).map { devices ->
        devices.map { deviceEntity -> deviceEntity.toDevice() }
    }

    override suspend fun fetchAndStoreDevices(userId: UserId): List<Device> {
        val devices = fetchDevicesDtos(userId)
        devices.forEach { device ->
            fetchShareAndFolder(
                FolderId(ShareId(userId, device.share.shareId), device.share.linkId)
            )
        }
        val deviceEntities = devices.map { deviceDto -> deviceDto.toDeviceEntity(userId) }
        db.inTransaction {
            db.deviceDao.deleteAll(userId)
            db.deviceDao.insertOrIgnore(*deviceEntities.toTypedArray())
        }
        return deviceEntities.map { deviceEntity -> deviceEntity.toDevice() }
    }

    private suspend fun fetchDevicesDtos(userId: UserId) = api.getDevices(userId)

    private suspend fun fetchShareAndFolder(folderId: FolderId) {
        getShare(folderId.shareId).toResult().getOrThrow()
        getLink(folderId).toResult().getOrThrow()
    }
}
