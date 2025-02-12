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
package me.proton.core.drive.drivelink.download.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.download.domain.manager.DownloadWorkManager
import me.proton.core.drive.link.domain.entity.LinkId
import javax.inject.Inject

class Download @Inject constructor(
    private val downloadWorkManager: DownloadWorkManager,
    private val getDriveLink: GetDriveLink,
) {

    suspend operator fun invoke(linkId: LinkId, retryable: Boolean = true) =
        getDriveLink(linkId).toResult().getOrThrow().let { driveLink ->
            invoke(driveLink, retryable)
        }

    suspend operator fun invoke(driveLink: DriveLink, retryable: Boolean = true) =
        invoke(listOf(driveLink), retryable)

    suspend operator fun invoke(driveLinks: List<DriveLink>, retryable: Boolean = true) =
        driveLinks.forEach { driveLink ->
            downloadWorkManager.download(driveLink, retryable)
        }
}
