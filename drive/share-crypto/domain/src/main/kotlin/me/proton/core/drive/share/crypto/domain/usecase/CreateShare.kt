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
package me.proton.core.drive.share.crypto.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.drive.crypto.domain.usecase.share.CreateShareInfo
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.repository.ShareRepository
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class CreateShare @Inject constructor(
    private val shareRepository: ShareRepository,
    private val createShareInfo: CreateShareInfo,
    private val getShare: GetShare,
) {
    operator fun invoke(
        volumeId: VolumeId,
        linkId: LinkId,
        name: String = DEFAULT_SHARE_NAME,
    ): Flow<DataResult<Share>> = flow {
        emit(DataResult.Processing(ResponseSource.Remote))
        val shareInfo = createShareInfo(linkId, name)
            .onFailure { cause ->
                return@flow emit(DataResult.Error.Local("Failed creating share info", cause))
            }
            .getOrThrow()
        val shareId = shareRepository.createShare(
            userId = linkId.shareId.userId,
            volumeId = volumeId,
            shareInfo = shareInfo,
        )
            .onFailure { cause ->
                return@flow emit(DataResult.Error.Remote("Failed creating share", cause))
            }
            .getOrThrow()
        emitAll(getShare(shareId))
    }

    companion object {
        const val DEFAULT_SHARE_NAME = "New Share"
    }
}
