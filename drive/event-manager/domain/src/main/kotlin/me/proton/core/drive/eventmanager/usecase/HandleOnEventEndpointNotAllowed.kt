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

package me.proton.core.drive.eventmanager.usecase

import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.api.ProtonApiCode.NOT_EXISTS
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.DeleteShare
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.share.domain.usecase.GetShares
import me.proton.core.drive.share.user.domain.usecase.DeleteLocalSharedWithMe
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.hasProtonErrorCode
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

class HandleOnEventEndpointNotAllowed @Inject constructor(
    private val getShares: GetShares,
    private val getShare: GetShare,
    private val deleteShare: DeleteShare,
    private val deleteLocalSharedWithMe: DeleteLocalSharedWithMe,
) {

    suspend operator fun invoke(userId: UserId, volumeId: VolumeId) = coRunCatching {
        getShares(userId, Share.Type.STANDARD, flowOf(false))
            .toResult()
            .getOrNull()
            ?.let { shares ->
                shares.filter { share -> share.volumeId == volumeId }
            }
            ?.takeIfNotEmpty()
            ?.let { shares ->
                shares.forEach { share ->
                    getShare(share.id, flowOf(true))
                        .toResult()
                        .onFailure { error ->
                            val apiError = error as? ApiException ?: error.cause as? ApiException
                            if (apiError != null && apiError.hasProtonErrorCode(NOT_EXISTS)) {
                                deleteShare(
                                    shareId = share.id,
                                    locallyOnly = true,
                                ).getOrNull(LogTag.EVENTS)
                                deleteLocalSharedWithMe(
                                    volumeId = share.volumeId,
                                    linkId = FileId(share.id, share.rootLinkId),
                                ).getOrNull(LogTag.EVENTS)
                            }
                        }
                }
            }
    }
}
