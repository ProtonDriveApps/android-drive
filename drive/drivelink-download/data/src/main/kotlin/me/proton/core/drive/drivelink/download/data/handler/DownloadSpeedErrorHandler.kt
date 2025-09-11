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

package me.proton.core.drive.drivelink.download.data.handler

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.drive.base.data.extension.hasConnectivity
import me.proton.core.drive.base.domain.log.LogTag.TRACKING
import me.proton.core.drive.drivelink.download.domain.handler.DownloadErrorHandler
import me.proton.core.drive.drivelink.download.domain.manager.DownloadErrorManager
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkdownload.domain.manager.DownloadSpeedManager
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class DownloadSpeedErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadSpeedManager: DownloadSpeedManager
) : DownloadErrorHandler {
    override suspend fun onError(downloadError: DownloadErrorManager.Error) {
        if (!downloadError.isCancelledByUser
            && downloadSpeedManager.isRunning()
            && !context.hasConnectivity()
        ) {
            CoreLogger.v(TRACKING, "Pausing, no network to download")
            downloadSpeedManager.pause(downloadError.fileId.userId)
        }
    }
}
