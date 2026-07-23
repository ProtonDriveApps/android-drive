/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.folder.create.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ProtonDriveClientProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.link.domain.usecase.ValidateLinkName
import me.proton.drive.sdk.entity.FolderNode
import me.proton.drive.sdk.entity.NodeUid
import java.time.Instant
import javax.inject.Inject

class CreateFolderSdk @Inject constructor(
    private val protonDriveClientProvider: ProtonDriveClientProvider,
    private val validateLinkName: ValidateLinkName,
    private val updateEventAction: UpdateEventAction,
) {
    suspend operator fun invoke(
        userId: UserId,
        parentFolderUid: NodeUid,
        folderName: String,
        modificationTime: Instant? = Instant.now(),
        shouldUpdateEvent: Boolean = true,
    ): Result<Pair<String, FolderNode>> = coRunCatching {
        val validatedName = validateLinkName(folderName).getOrThrow()

        val block: suspend () -> Pair<String, FolderNode> = {
            val folderNode = protonDriveClientProvider
                .getOrCreate(userId)
                .getOrThrow()
                .createFolder(
                    parentFolderUid = parentFolderUid,
                    name = validatedName,
                    lastModificationTime = modificationTime,
                )

            validatedName to folderNode
        }

        if (shouldUpdateEvent) {
            updateEventAction(
                userId = userId,
                nodeUid = parentFolderUid,
            ) {
                block.invoke()
            }
        } else {
            block.invoke()
        }
    }
}
