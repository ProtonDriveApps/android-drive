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

package me.proton.core.drive.drivelink.crypto.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccess
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toDataResult
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import javax.inject.Inject

class GetDecryptedDriveLink @Inject constructor(
    private val getDriveLink: GetDriveLink,
    private val decryptDriveLink: DecryptDriveLink,
) {
    operator fun invoke(
        userId: UserId,
        folderId: FolderId?,
        failOnDecryptionError: Boolean = true,
    ): Flow<DataResult<DriveLink.Folder>> =
        getDriveLink(userId, folderId = folderId)
            .mapSuccess { (_, driveLink) ->
                decryptDriveLink(driveLink, failOnDecryptionError).toDataResult()
            }

    operator fun invoke(
        fileId: FileId,
        failOnDecryptionError: Boolean = true,
    ): Flow<DataResult<DriveLink.File>> =
        getDriveLink(fileId = fileId)
            .mapSuccess { (_, driveLink) ->
                decryptDriveLink(driveLink, failOnDecryptionError).toDataResult()
            }

    operator fun invoke(
        linkId: LinkId,
        failOnDecryptionError: Boolean = true,
    ): Flow<DataResult<DriveLink>> =
        getDriveLink(linkId = linkId)
            .mapSuccess { (_, driveLink) ->
                decryptDriveLink(driveLink = driveLink, failOnDecryptionError).toDataResult()
            }
}
