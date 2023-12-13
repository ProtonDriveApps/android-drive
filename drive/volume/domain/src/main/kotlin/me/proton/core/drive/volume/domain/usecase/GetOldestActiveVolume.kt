/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.volume.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.transformSuccess
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.entity.isActive
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetOldestActiveVolume @Inject constructor(
    private val getVolumes: GetVolumes,
    private val getVolume: GetVolume,
) {
    operator fun invoke(userId: UserId): Flow<DataResult<Volume>> =
        getVolumes(userId).transformSuccessToOldestActiveVolume(userId)

    private fun Flow<DataResult<List<Volume>>>.transformSuccessToOldestActiveVolume(
        userId: UserId,
    ): Flow<DataResult<Volume>> =
        transformSuccess { (_, volumes) ->
            val activeVolumes = volumes.filter { volume -> volume.isActive }.takeIfNotEmpty()
                ?: return@transformSuccess emitNotFound()
            val volume = activeVolumes.minBy { volume -> volume.creationTime.value }
            emitAll(getVolume(userId, volume.id))
        }

    private suspend fun <T> FlowCollector<DataResult<T>>.emitNotFound() =
        emit(
            DataResult.Error.Local(
                message = "No active volume found",
                cause = NoSuchElementException("No active volume found")
            )
        )
}
