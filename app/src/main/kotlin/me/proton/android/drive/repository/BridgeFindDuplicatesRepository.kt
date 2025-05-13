/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.repository

import kotlinx.coroutines.flow.first
import me.proton.android.drive.photos.data.repository.PhotoFindDuplicatesRepository
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.backup.data.repository.FolderFindDuplicatesRepository
import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.backup.domain.repository.FindDuplicatesRepository
import me.proton.core.drive.base.domain.entity.ClientUid
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class BridgeFindDuplicatesRepository @Inject constructor(
    private val getShare: GetShare,
    private val configurationProvider: ConfigurationProvider,
    private val folder: FolderFindDuplicatesRepository,
    private val photo: PhotoFindDuplicatesRepository,
) : FindDuplicatesRepository {
    override suspend fun findDuplicates(
        parentId: ParentId,
        nameHashes: List<String>,
        clientUids: List<ClientUid>,
    ): List<BackupDuplicate> {
        val share = getShare(parentId.shareId)
            .filterSuccessOrError().mapSuccessValueOrNull().first()
        requireNotNull(share) { "Cannot find share for folder: ${parentId.id.logId()}" }
        return if (share.type == Share.Type.PHOTO) {
            photo.findDuplicates(parentId, nameHashes, clientUids)
        } else {
            folder.findDuplicates(parentId, nameHashes, clientUids)
        }.let { backupDuplicates ->
            if (configurationProvider.allowBackupDeletedFilesEnabled) {
                backupDuplicates.filterNot { duplicate ->
                    duplicate.linkState == null
                }
            } else {
                backupDuplicates
            }
        }
    }
}
