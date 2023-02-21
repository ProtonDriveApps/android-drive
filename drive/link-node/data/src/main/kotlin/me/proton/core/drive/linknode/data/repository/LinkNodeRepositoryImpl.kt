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
package me.proton.core.drive.linknode.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.link.data.extension.toLink
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linknode.data.db.LinkAncestorDao
import me.proton.core.drive.linknode.domain.entity.LinkNode
import me.proton.core.drive.linknode.domain.repository.LinkNodeRepository
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class LinkNodeRepositoryImpl @Inject constructor(
    private val db: LinkAncestorDao
) : LinkNodeRepository {

    override fun addLinkNode(linkNode: LinkNode) {
        // do nothing
    }

    override fun getLinkNode(linkId: LinkId): LinkNode? {
        return null
    }

    override fun getAncestors(linkId: LinkId): Flow<DataResult<List<Link>>> =
        db.getLinkWithPropertiesAncestors(linkId.userId, linkId.shareId.id, linkId.id)
            .mapLatest { list ->
                list.map { linkWithProperties -> linkWithProperties.toLink() }.asSuccess
            }
}
