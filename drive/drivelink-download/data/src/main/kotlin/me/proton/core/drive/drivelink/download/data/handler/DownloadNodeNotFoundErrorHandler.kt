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

package me.proton.core.drive.drivelink.download.data.handler

import kotlinx.coroutines.flow.flowOf
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.drivelink.download.domain.handler.DownloadErrorHandler
import me.proton.core.drive.drivelink.download.domain.manager.DownloadErrorManager
import me.proton.core.drive.link.domain.extension.isNodeNotFound
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.handler.LinkNotFoundHandler
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

/**
 * Removes a file locally when its download fails because the backend no longer has it.
 *
 * Only an SDK error naming the file counts: a bare not-exists error from a download can also mean
 * that the cached revision was replaced while the file still exists. The file's volume comes from
 * its cached share; without it nothing is removed.
 */
class DownloadNodeNotFoundErrorHandler @Inject constructor(
    private val linkNotFoundHandler: LinkNotFoundHandler,
    private val getShare: GetShare,
) : DownloadErrorHandler {
    override suspend fun onError(downloadError: DownloadErrorManager.Error) {
        val fileId = downloadError.fileId
        val volumeId = getShare(fileId.shareId, refresh = flowOf(false)).toResult().getOrNull()?.volumeId
            ?: return
        if (downloadError.throwable.isNodeNotFound(fileId.nodeUid(volumeId))) {
            linkNotFoundHandler.onLinkNotFound(fileId)
        }
    }
}
