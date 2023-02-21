/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.volume.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.api.runCatchingApiException
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.extension.asSuccessOrNullAsError
import me.proton.core.drive.volume.data.api.VolumeApiDataSource
import me.proton.core.drive.volume.data.db.VolumeDao
import me.proton.core.drive.volume.data.db.VolumeEntity
import me.proton.core.drive.volume.data.extension.asVolume
import me.proton.core.drive.volume.data.extension.toCreateVolumeRequest
import me.proton.core.drive.volume.data.extension.toVolumeEntity
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.entity.VolumeInfo
import me.proton.core.drive.volume.domain.repository.VolumeRepository
import javax.inject.Inject

class VolumeRepositoryImpl @Inject constructor(
    private val api: VolumeApiDataSource,
    private val db: VolumeDao,
) : VolumeRepository {

    override fun getVolumesFlow(userId: UserId): Flow<DataResult<List<Volume>>> =
        db.getAllFlow(userId)
            .map { volumeEntities ->
                volumeEntities.map { volumeEntity -> volumeEntity.asVolume }.asSuccess
            }

    override suspend fun hasVolumes(userId: UserId): Boolean =
        db.hasVolumeEntities(userId)

    override suspend fun fetchVolumes(userId: UserId) =
        with(api.getVolumes(userId).map { volume -> volume.toVolumeEntity(userId) }) {
            db.insertOrUpdate(*toTypedArray())
            map { volumeEntity -> volumeEntity.asVolume }
        }

    override fun getVolumeFlow(userId: UserId, volumeId: VolumeId): Flow<DataResult<Volume>> =
        db.getDistinctFlow(userId, volumeId.id)
            .map { volumeEntity: VolumeEntity? ->
                volumeEntity?.asVolume.asSuccessOrNullAsError()
            }

    override suspend fun hasVolume(userId: UserId, volumeId: VolumeId): Boolean =
        db.hasVolumeEntity(userId, volumeId.id)

    override suspend fun fetchVolume(userId: UserId, volumeId: VolumeId) =
        db.insertOrUpdate(
            api.getVolume(userId, volumeId.id).toVolumeEntity(userId)
        )

    override fun createVolume(userId: UserId, volumeInfo: VolumeInfo): Flow<DataResult<Volume>> = flow {
        emit(DataResult.Processing(ResponseSource.Remote))
        emit(
            runCatchingApiException {
                with(api.createVolume(userId, volumeInfo.toCreateVolumeRequest()).toVolumeEntity(userId)) {
                    db.insertOrUpdate(this)
                    asVolume
                }
            }
        )
    }

    override suspend fun removeVolume(userId: UserId, volumeId: VolumeId) =
        db.delete(userId, volumeId.id)
}
