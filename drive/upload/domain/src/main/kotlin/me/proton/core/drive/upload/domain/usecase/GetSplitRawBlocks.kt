/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.upload.domain.usecase

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.linkupload.domain.entity.RawBlock
import me.proton.core.drive.linkupload.domain.usecase.GetRawUploadBlocks
import me.proton.core.drive.upload.domain.exception.InconsistencyException
import java.io.File
import javax.inject.Inject

class GetSplitRawBlocks @Inject constructor(
    private val getRawBlocks: GetRawUploadBlocks,
) {
    suspend operator fun invoke(
        uploadFileLinkId: Long,
        destinationFolder: File,
    ): Result<List<Pair<Long, File>>> = coRunCatching {
        getRawBlocks(uploadFileLinkId)
            .getOrThrow()
            .map { rawBlock: RawBlock ->
                rawBlock.index to File(destinationFolder, rawBlock.name).also { file ->
                    if (file.exists().not()) throw InconsistencyException()
                }
            }
    }
}
