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

package me.proton.core.drive.share.user.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.share.user.domain.entity.SharedListing
import me.proton.core.drive.share.user.domain.extension.toPairSharedListingSaveAction
import me.proton.core.drive.share.user.domain.repository.SharedRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class FetchSharedByMe @Inject constructor(
    private val repository: SharedRepository,
    private val fetchAllSharedByMeOnPhotoVolume: FetchAllSharedByMeOnPhotoVolume,
) {

    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        anchorId: String?,
    ): Result<Pair<SharedListing, SaveAction>> = coRunCatching {
        listOfNotNull(
            takeIf { anchorId == null }
                ?.let {
                    fetchAllSharedByMeOnPhotoVolume(userId)
                        .getOrNull(LogTag.SHARING, "Fetching shared by me on photo volume failed")
                },
            repository.fetchSharedByMeListing(
                userId = userId,
                volumeId = volumeId,
                anchorId = anchorId,
            ),
        ).toPairSharedListingSaveAction()
    }
}
