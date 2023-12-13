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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.repository.listFetcherEmitOnEmpty
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.repository.ShareRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class GetShares @Inject constructor(
    private val shareRepository: ShareRepository,
) {

    operator fun invoke(
        userId: UserId,
        shareType: Share.Type,
        refresh: Flow<Boolean> = flowOf { !shareRepository.hasShares(userId, shareType) }
    ) =
        refresh.transform { shouldRefresh ->
            if (shouldRefresh) {
                listFetcherEmitOnEmpty<Share> { shareRepository.fetchShares(userId, shareType) }
            }
            emitAll(shareRepository.getSharesFlow(userId, shareType))
        }

    operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        shareType: Share.Type,
        refresh: Flow<Boolean> = flowOf { !shareRepository.hasShares(userId, volumeId, shareType) }
    ) =
        refresh.transform { shouldRefresh ->
            if (shouldRefresh) {
                listFetcherEmitOnEmpty<Share> { shareRepository.fetchShares(userId, shareType) }
            }
            emitAll(shareRepository.getSharesFlow(userId, volumeId))
        }
}
