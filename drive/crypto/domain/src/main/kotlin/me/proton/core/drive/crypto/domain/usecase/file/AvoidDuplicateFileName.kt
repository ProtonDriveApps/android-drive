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
package me.proton.core.drive.crypto.domain.usecase.file

import me.proton.core.drive.base.domain.extension.avoidDuplicateFileName
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.HmacSha256
import me.proton.core.drive.folder.domain.usecase.HasFolderChildrenWithHash
import me.proton.core.drive.key.domain.entity.NodeHashKey
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject

class AvoidDuplicateFileName @Inject constructor(
    private val hmacSha256: HmacSha256,
    private val hasFolderChildrenWithHash: HasFolderChildrenWithHash,
) {
    suspend operator fun invoke(
        fileName: String,
        parentFolderId: FolderId,
        folderHashKey: NodeHashKey
    ): Result<String> = coRunCatching {
        var name = fileName
        while (hasFolderChildrenWithHash(parentFolderId, hmacSha256(folderHashKey, name).getOrThrow())) {
            name = name.avoidDuplicateFileName()
        }
        name
    }
}
