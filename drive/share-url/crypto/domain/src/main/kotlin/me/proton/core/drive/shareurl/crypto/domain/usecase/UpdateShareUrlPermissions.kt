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
package me.proton.core.drive.shareurl.crypto.domain.usecase

import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.share.domain.usecase.GetMainShare
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrl
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.shareurl.base.domain.repository.ShareUrlRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class UpdateShareUrlPermissions @Inject constructor(
    private val shareUrlRepository: ShareUrlRepository,
    private val updateEventAction: UpdateEventAction,
    private val getMainShare: GetMainShare,
) {
    suspend operator fun invoke(
        volumeId: VolumeId,
        shareUrlId: ShareUrlId,
        permissions: Permissions,
    ): Result<ShareUrl> = coRunCatching {
        updateEventAction(getMainShare(shareUrlId.shareId.userId).toResult().getOrThrow().id) {
            shareUrlRepository.updateShareUrlPermissions(
                volumeId = volumeId,
                shareUrlId = shareUrlId,
                permissions = permissions,
            ).getOrThrow()
        }
    }
}
