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
package me.proton.core.drive.eventmanager.usecase

import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.eventmanager.entity.LinkEventVO
import me.proton.core.drive.share.domain.usecase.DeleteShare
import me.proton.core.drive.shareurl.base.domain.usecase.DeleteShareUrl
import me.proton.core.drive.shareurl.base.domain.usecase.GetShareUrl
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class HandleDeletedShareUrlIds @Inject constructor(
    private val getShareUrl: GetShareUrl,
    private val deleteShareUrl: DeleteShareUrl,
) {
    suspend operator fun invoke(vos: List<LinkEventVO>) {
        vos.forEach { vo ->
            vo.deletedShareUrlIds.forEach { deletedShareUrlId ->
                getShareUrl(
                    userId = vo.link.id.shareId.userId,
                    shareUrlId = deletedShareUrlId,
                )?.let { shareUrl ->
                    deleteShareUrl(shareUrl.id).onFailure {error ->
                        CoreLogger.w(SHARING, error, "Cannot delete share url")
                    }
                }
            }
        }
    }
}
