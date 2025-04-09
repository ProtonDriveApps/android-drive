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
package me.proton.core.drive.drivelink.shared.domain.usecase

import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.usecase.DeleteShare
import javax.inject.Inject

class DeleteShareUrlAndShare @Inject constructor(
    private val getLink: GetLink,
    private val deleteShareUrl: me.proton.core.drive.shareurl.base.domain.usecase.DeleteShareUrl,
    private val deleteShare: DeleteShare,
    private val updateEventAction: UpdateEventAction,
) {
    suspend operator fun invoke(linkId: LinkId): Result<Unit> = coRunCatching {
        updateEventAction(linkId.shareId) {
            val sharingDetails = getLink(linkId).toResult().getOrThrow().sharingDetails
            deleteShareUrl(
                shareUrlId = requireNotNull(sharingDetails?.shareUrlId) {
                    "ShareUrlId not found"
                },
            ).getOrThrow()
            deleteShare(requireNotNull(sharingDetails?.shareId), force = false)
                .getOrNull(SHARING, "Cannot delete standard share")
        }
    }
}
