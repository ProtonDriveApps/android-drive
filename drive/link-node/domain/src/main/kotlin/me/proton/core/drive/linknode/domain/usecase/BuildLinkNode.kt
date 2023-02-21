/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.linknode.domain.usecase

import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccess
import me.proton.core.drive.base.domain.extension.asSuccessOrNullAsError
import me.proton.core.drive.base.domain.extension.firstSuccessOrError
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.linknode.domain.entity.LinkNode
import javax.inject.Inject

class BuildLinkNode @Inject constructor(
    private val getLinkAncestors: GetLinkAncestors,
) {
    suspend operator fun invoke(linkId: LinkId): DataResult<LinkNode> =
        getLinkAncestors(linkId)
            .mapSuccess { result ->
                result.value
                    .transformToLinkNode()
                    .asSuccessOrNullAsError()
            }
            .firstSuccessOrError()

    private fun List<Link>?.transformToLinkNode(): LinkNode? = this?.fold(null as? LinkNode?) { parent, link ->
        require(link.parentId == parent?.link?.id) { "Invalid ancestor" }
        LinkNode(parent, null, link).also { child ->
            parent?.child = child
        }
    }
}
