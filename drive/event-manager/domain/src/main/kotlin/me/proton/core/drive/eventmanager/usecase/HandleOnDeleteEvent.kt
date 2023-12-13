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

import kotlinx.coroutines.flow.first
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLinks
import me.proton.core.drive.drivelink.download.domain.usecase.CancelDownload
import me.proton.core.drive.drivelink.offline.domain.usecase.DeleteLocalContent
import me.proton.core.drive.folder.domain.usecase.GetAllFolderChildren
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.ids
import me.proton.core.drive.link.domain.usecase.DeleteLinks
import me.proton.core.drive.photo.domain.usecase.DeletePhotoListings
import me.proton.core.drive.share.crypto.domain.usecase.DeleteLocalShares
import javax.inject.Inject

class HandleOnDeleteEvent @Inject constructor(
    private val deleteLinks: DeleteLinks,
    private val getDriveLinks: GetDriveLinks,
    private val cancelDownload: CancelDownload,
    private val deleteLocalContent: DeleteLocalContent,
    private val getChildren: GetAllFolderChildren,
    private val deleteLocalShares: DeleteLocalShares,
    private val deletePhotoListings: DeletePhotoListings,
) {

    suspend operator fun invoke(linkIds: List<LinkId>) {
        if (linkIds.isEmpty()) {
            return
        }
        getDriveLinks(linkIds).first()
            .forEach { driveLink ->
                cancelDownload(driveLink)
                when (driveLink) {
                    is DriveLink.File -> deleteLocalContent(driveLink)
                    is DriveLink.Folder -> getChildren(driveLink.id, false)
                        .onSuccess { children ->
                            invoke(children.ids)
                        }
                }

            }
        deleteLocalShares(linkIds)
        deleteLinks(linkIds)
        deletePhotoListings(linkIds)
    }
}
