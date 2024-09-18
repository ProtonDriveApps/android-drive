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
package me.proton.core.drive.drivelink.download.domain.extension

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import java.io.File
import kotlin.coroutines.CoroutineContext

suspend fun DriveLink.isDownloaded(coroutineContext: CoroutineContext = Dispatchers.IO): Boolean {
    val downloadState = downloadState as? DownloadState.Downloaded ?: return false
    return withContext(coroutineContext) {
        downloadState.blocks.all { block -> File(block.url).exists() }
    }
}
