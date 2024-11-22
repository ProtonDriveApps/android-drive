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

package me.proton.core.drive.linkoffline.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linknode.domain.entity.LinkNode
import me.proton.core.drive.linknode.domain.extension.ancestors
import me.proton.core.drive.linknode.domain.usecase.GetLinkNode
import me.proton.core.drive.linkoffline.domain.repository.LinkOfflineRepository
import javax.inject.Inject

class IsAnyAncestorMarkedAsOffline @Inject constructor(
    private val linkOfflineRepository: LinkOfflineRepository,
    private val getLinkNode: GetLinkNode,
) {

    suspend operator fun invoke(linkNode: LinkNode): Result<Boolean> = coRunCatching {
        linkOfflineRepository.isAnyMarkedOffline(linkNode.ancestors.map { node -> node.link.id }.toSet())
    }

    suspend operator fun invoke(linkId: LinkId): Result<Boolean> = coRunCatching {
        getLinkNode(linkId)
            .toResult()
            .getOrThrow()
            .let { linkNode ->
                invoke(linkNode).getOrThrow()
            }
    }
}
