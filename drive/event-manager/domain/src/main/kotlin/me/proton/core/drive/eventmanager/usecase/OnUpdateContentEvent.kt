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
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.download.domain.usecase.Download
import me.proton.core.drive.drivelink.offline.domain.usecase.DeleteLocalContent
import me.proton.core.drive.eventmanager.entity.LinkEventVO
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.usecase.HasLink
import me.proton.core.drive.link.domain.usecase.InsertOrUpdateLinks
import me.proton.core.drive.linkoffline.domain.usecase.GetFirstMarkedOfflineLink
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class OnUpdateContentEvent @Inject constructor(
    private val getFirstMarkedOfflineLink: GetFirstMarkedOfflineLink,
    private val getDriveLink: GetDriveLink,
    private val deleteLocalContent: DeleteLocalContent,
    private val download: Download,
    private val insertOrUpdateLinks: InsertOrUpdateLinks,
    private val hasLink: HasLink,
) {

    suspend operator fun invoke(vos: List<LinkEventVO>) {
        CoreLogger.d(LogTag.EVENTS, "OnUpdateContentEvent: ${vos.joinToString { vo -> vo.link.id.id.logId() }}")
        val links = vos.map { vo -> vo.link }
        links.forEach { link -> link.deleteLocalContent() }
        val linksWithParentInCache = links.filter { link: Link ->
            link.parentId?.let { parentId -> hasLink(parentId).first() } == true
        }
        if (linksWithParentInCache.isNotEmpty()) {
            insertOrUpdateLinks(linksWithParentInCache)
            download(
                linksWithParentInCache.mapNotNull { link ->
                    getFirstMarkedOfflineLink(link.id)?.let { getDriveLink(it.id).toResult().getOrNull() }
                }
            )
        }
    }

    private suspend fun Link.deleteLocalContent() =
        getDriveLink(id).toResult().onSuccess { driveLink ->
            if (driveLink is DriveLink.File) {
                deleteLocalContent(driveLink)
            }
        }
}
