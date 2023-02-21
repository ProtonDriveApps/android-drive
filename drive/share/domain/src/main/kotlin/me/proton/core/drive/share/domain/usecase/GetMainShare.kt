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
package me.proton.core.drive.share.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.transformSuccess
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.exception.ShareException
import me.proton.core.drive.share.domain.extension.creationTimeOrMaxLongIfNull
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetMainShare @Inject constructor(
    private val getShares: GetShares,
    private val getShare: GetShare,
    private val deleteLockedShares: DeleteLockedShares,
) {
    operator fun invoke(userId: UserId, volumeId: VolumeId): Flow<DataResult<Share>> =
        getShares(userId, volumeId).transformSuccessToMainShare(userId)

    operator fun invoke(userId: UserId): Flow<DataResult<Share>> =
        getShares(userId).transformSuccessToMainShare(userId)

    private fun Flow<DataResult<List<Share>>>.transformSuccessToMainShare(userId: UserId): Flow<DataResult<Share>> =
        transformSuccess { (_, shares) ->
            val main = shares.filter { share -> share.isMain }.takeIfNotEmpty()
                ?: return@transformSuccess emitNotFound(userId)
            deleteLockedShares(main)
            val unlocked = main.filterNot { share -> share.isLocked }.takeIfNotEmpty()
                ?: return@transformSuccess emitLocked(main)
            val share = unlocked.minByOrNull { share -> share.creationTimeOrMaxLongIfNull.value }
                ?: return@transformSuccess emitIllegalState("minByOrNull returns null on list size ${unlocked.size}")
            emitAll(getShare(share.id))
        }

    private suspend fun <T> FlowCollector<DataResult<T>>.emitNotFound(userId: UserId) =
        emit(
            DataResult.Error.Local(
                message = "Main share for volume is not found",
                cause = ShareException.MainShareNotFound(userId)
            )
        )

    private suspend fun <T> FlowCollector<DataResult<T>>.emitLocked(locked: List<Share>) =
        emit(
            DataResult.Error.Local(
                message = "Main share is locked probably due password reset",
                cause = ShareException.MainShareLocked(locked.map { share -> share.id })
            )
        )

    private suspend fun <T> FlowCollector<DataResult<T>>.emitIllegalState(message: String) =
        emit(
            DataResult.Error.Local(
                message = message,
                cause = IllegalStateException(message)
            )
        )
}
