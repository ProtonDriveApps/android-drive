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

package me.proton.core.drive.drivelink.download.domain.usecase

import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.linkdownload.domain.usecase.GetDownloadBlocks
import java.io.File
import javax.inject.Inject

class DeleteDownloadedBlocks @Inject constructor(
    private val getDownloadBlocks: GetDownloadBlocks,
) {
    suspend operator fun invoke(driveLink: DriveLink.File) = coRunCatching {
        getDownloadBlocks(driveLink.link).getOrThrow().forEach { block ->
            runCatching {
                File(block.url).delete()
            }.getOrNull(LogTag.DOWNLOAD, "Cannot delete file: ${block.url}")
        }
    }
}
