/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.drivelink.offline.domain.usecase

import kotlinx.coroutines.flow.first
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLinks
import me.proton.core.drive.drivelink.download.domain.usecase.CancelDownload
import me.proton.core.drive.drivelink.download.domain.usecase.Download
import me.proton.core.drive.file.base.domain.usecase.MoveToCache
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkoffline.domain.usecase.GetFirstMarkedOfflineLink
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

class UpdateOfflineContent @Inject constructor(
    private val moveToCache: MoveToCache,
    private val getFirstMarkedOfflineLink: GetFirstMarkedOfflineLink,
    private val getDriveLinks: GetDriveLinks,
    private val getDriveLink: GetDriveLink,
    private val download: Download,
    private val cancelDownload: CancelDownload,
) {
    suspend operator fun invoke(linkId: LinkId) = invoke(listOf(linkId))

    // JvmName is required otherwise there is a signature clash with the other invoke
    @JvmName("updateContentForIds")
    suspend operator fun invoke(linkIds: List<LinkId>) = linkIds.takeIfNotEmpty()?.let {
        invoke(getDriveLinks(linkIds).first())
    }

    suspend operator fun invoke(driveLinks: List<DriveLink>) {
        driveLinks.forEach { driveLink ->
            val firstMarkedOfflineLink = getFirstMarkedOfflineLink(driveLink.id)
            if (firstMarkedOfflineLink != null) {
                getDriveLink(firstMarkedOfflineLink.id).toResult().onSuccess { parent ->
                    download(parent)
                }
            } else if (driveLink is DriveLink.File) {
                cancelDownload(driveLink)
                moveToCache(driveLink.id.userId, driveLink.volumeId, driveLink.activeRevisionId)
            }
        }
    }
}
