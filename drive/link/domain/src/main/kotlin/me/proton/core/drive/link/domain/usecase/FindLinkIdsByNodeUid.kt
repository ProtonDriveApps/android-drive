/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.link.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.linkId
import me.proton.core.drive.volume.domain.extension.volumeId
import me.proton.drive.sdk.entity.NodeUid
import javax.inject.Inject

class FindLinkIdsByNodeUid @Inject constructor(
    private val findLinkIds: FindLinkIds,
) {

    suspend operator fun invoke(
        userId: UserId,
        nodeUid: NodeUid,
    ): Result<List<LinkId>> = coRunCatching {
        findLinkIds(userId, nodeUid.volumeId, nodeUid.linkId).getOrThrow()
    }
}
