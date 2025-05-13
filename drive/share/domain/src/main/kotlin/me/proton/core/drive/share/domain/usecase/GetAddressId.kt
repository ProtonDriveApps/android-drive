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

package me.proton.core.drive.share.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.SHARE
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.usecase.GetAddressId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.usecase.GetOldestActiveVolume
import me.proton.core.drive.volume.domain.usecase.GetVolume
import me.proton.core.user.domain.entity.AddressId
import javax.inject.Inject

class GetAddressId @Inject constructor(
    private val getVolume: GetVolume,
    private val getShare: GetShare,
    private val getAddressId: GetAddressId,
    private val getShareMembership: GetShareMembership,
) {
    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
    ): Result<AddressId> = coRunCatching {
        getVolume(userId, volumeId).toResult()
            .getOrNull(SHARE, "Cannot find volume ${volumeId.id.logId()}")
            ?.let { volume -> ShareId(userId, volume.shareId) }
            ?.let { shareId ->
                getShare(shareId).toResult()
                    .getOrNull("Cannot find volume share: ${shareId.id.logId()}")
            }
            ?.addressId
            ?: getAddressId(userId)
    }

    suspend operator fun invoke(
        shareId: ShareId,
    ): Result<AddressId> = coRunCatching {
        val share = getShare(shareId).toResult().getOrThrow()
        if (share.type == Share.Type.STANDARD) {
            getShareMembership(shareId).toResult().getOrThrow().addressId
        } else {
            share.addressId ?: invoke(
                userId = shareId.userId,
                volumeId = share.volumeId,
            ).getOrThrow()
        }
    }
}
