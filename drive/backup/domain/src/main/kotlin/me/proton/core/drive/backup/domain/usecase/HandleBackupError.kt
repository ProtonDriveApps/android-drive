/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.backup.domain.usecase

import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupErrorType
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.folder.domain.usecase.DeleteLocalContent
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class HandleBackupError @Inject constructor(
    private val stopBackup: StopBackup,
    private val addBackupError: AddBackupError,
    private val getShare: GetShare,
    private val deleteLocalContent: DeleteLocalContent,
) {
    suspend operator fun invoke(folderId: FolderId, backupError: BackupError) = coRunCatching {
        if (backupError.type == BackupErrorType.MIGRATION) {
            stopBackup(folderId, backupError).getOrThrow()
            deleteLocalContent(
                volumeId = getShare(folderId.shareId).toResult().getOrThrow().volumeId,
                folderId = folderId,
            ).getOrNull(BACKUP, "Cannot delete local content: ${folderId.id.logId()}")
        } else {
            addBackupError(folderId, backupError).getOrThrow()
        }
    }
}
