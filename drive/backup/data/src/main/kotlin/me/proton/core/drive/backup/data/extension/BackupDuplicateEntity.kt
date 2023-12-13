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

package me.proton.core.drive.backup.data.extension

import me.proton.core.drive.backup.data.db.entity.BackupDuplicateEntity
import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId

fun BackupDuplicateEntity.toBackupDuplicate(): BackupDuplicate =
    ShareId(userId, shareId).let { shareId ->
        BackupDuplicate(
            id = id,
            parentId = FolderId(shareId, parentId),
            hash = hash,
            contentHash = contentHash,
            linkId = linkId?.let { linkId -> FileId(shareId, linkId) },
            linkState = linkState,
            revisionId = revisionId,
            clientUid = clientUid,
        )
    }
