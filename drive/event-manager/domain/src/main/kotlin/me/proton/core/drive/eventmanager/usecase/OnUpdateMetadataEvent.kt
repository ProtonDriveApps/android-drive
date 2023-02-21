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

package me.proton.core.drive.eventmanager.usecase

import kotlinx.coroutines.flow.first
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.eventmanager.entity.LinkEventVO
import me.proton.core.drive.link.domain.usecase.HasLink
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

class OnUpdateMetadataEvent @Inject constructor(
    private val hasLink: HasLink,
    private val handleEventLinks: HandleCreateOrUpdateLinksEvent,
    private val handleDeletedShareUrlIds: HandleDeletedShareUrlIds,
    private val deleteLinks: HandleOnDeleteEvent,
) {

    suspend operator fun invoke(vos: List<LinkEventVO>) {
        val links = vos.map { vo -> vo.link }
        CoreLogger.d(LogTag.EVENTS, "onUpdateMetadataEvent: ${links.joinToString { link -> link.id.id.logId() }}")
        val grouped = vos.groupBy { vo ->
            val parentId = vo.link.parentId ?: return@groupBy true
            hasLink(parentId).first()
        }
        grouped[true]?.takeIfNotEmpty()?.let { linksWithParentInCache ->
            handleEventLinks(
                vos = linksWithParentInCache,
            )
        }
        grouped[false]?.takeIfNotEmpty()?.let { linksWithoutParentInCache ->
            deleteLinks(linksWithoutParentInCache.map { vo -> vo.link.id })
        }
        handleDeletedShareUrlIds(vos)
    }
}
