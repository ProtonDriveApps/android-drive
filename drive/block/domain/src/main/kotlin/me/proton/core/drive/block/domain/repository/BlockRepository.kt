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
package me.proton.core.drive.block.domain.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.block.domain.entity.UploadBlocksUrl
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.linkupload.domain.entity.UploadBlock
import me.proton.core.user.domain.entity.AddressId

interface BlockRepository {
    suspend fun getUploadBlocksUrl(
        userId: UserId,
        addressId: AddressId,
        fileId: FileId,
        revisionId: String,
        uploadBlocks: List<UploadBlock>,
        uploadThumbnail: UploadBlock?,
    ): Result<UploadBlocksUrl>
}
