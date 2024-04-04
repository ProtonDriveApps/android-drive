/*
 * Copyright (c) 2022-2024 Proton AG.
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
package me.proton.core.drive.shareurl.crypto.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.extension.toDataResult
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.crypto.domain.usecase.share.CreateShareUrlInfo
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrl
import me.proton.core.drive.shareurl.base.domain.usecase.GetShareUrl
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class GetOrCreateShareUrl @Inject constructor(
    private val getShareUrl: GetShareUrl,
    private val createShareUrlInfo: CreateShareUrlInfo,
    private val createShareUrl: CreateShareUrl,
    private val getLink: GetLink,
) {
    suspend operator fun invoke(volumeId: VolumeId, share: Share, linkId: LinkId): Flow<DataResult<ShareUrl>> = flow {
        val shareUrl = getShareUrlId(linkId)?.let { shareUrlId ->
            getShareUrl(
                volumeId = volumeId,
                shareUrlId = shareUrlId,
                refresh = flowOf { true },
            ).toResult().getOrNull()
        } ?: createShareUrl(
            volumeId = volumeId,
            shareId = share.id,
            shareUrlInfo = createShareUrlInfo(share)
                .toDataResult()
                .onFailure { error ->
                    return@flow emit(error)
                }
                .toResult()
                .getOrThrow()
        )
            .toDataResult()
            .onFailure { error ->
                return@flow emit(error)
            }
            .toResult()
            .getOrThrow()
        emitAll(
            getShareUrl(
                volumeId = volumeId,
                shareUrlId = shareUrl.id,
                refresh = flowOf { false },
            )
        )
    }

    private suspend fun getShareUrlId(
        linkId: LinkId,
    ) = getLink(linkId).toResult().getOrNull()?.sharingDetails?.shareUrlId
}
