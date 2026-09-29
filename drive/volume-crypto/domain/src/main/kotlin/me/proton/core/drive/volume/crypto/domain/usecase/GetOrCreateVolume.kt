/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.volume.crypto.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.extension.asSuccessOrNullAsError
import me.proton.core.drive.base.domain.extension.transformSuccess
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.entity.isActive
import me.proton.core.drive.volume.domain.usecase.GetOldestActiveVolume
import me.proton.core.drive.volume.domain.usecase.GetVolumes
import me.proton.core.network.domain.hasProtonErrorCode
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetOrCreateVolume @Inject constructor(
    private val getVolumes: GetVolumes,
    private val getOldestActiveVolume: GetOldestActiveVolume,
    private val createVolume: CreateVolume,
) {
    @ExperimentalCoroutinesApi
    operator fun invoke(userId: UserId, type: Volume.Type): Flow<DataResult<Volume>> =
        getVolumes(userId).transformSuccess { (_, volumes) ->
            val activeVolumes = volumes.filter { volume ->
                volume.type == type && volume.isActive
            }
            if (activeVolumes.isEmpty()) {
                emitAll(
                    createOrGetVolume(userId, type)
                )
            } else {
                emit(activeVolumes
                    .minByOrNull { volume -> volume.createTime.value }
                    .asSuccessOrNullAsError()
                )
            }
        }

    @ExperimentalCoroutinesApi
    private fun createOrGetVolume(
        userId: UserId,
        type: Volume.Type
    ): Flow<DataResult<Volume>> = createVolume(userId, type).transform { dataResult ->
        val cause = (dataResult as? DataResult.Error)?.cause
        if (cause?.hasProtonErrorCode(ProtonApiCode.ALREADY_EXISTS) == true) {
            CoreLogger.w(
                LogTag.VOLUME,
                cause,
                "Volume already exists, fetching volumes",
            )
            emitAll(getOldestActiveVolume(userId, type, refresh = flowOf(true)))
        } else {
            emit(dataResult)
        }
    }
}
