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
package me.proton.core.drive.drivelink.offline.domain.usecase

import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.download.domain.usecase.CancelDownload
import me.proton.core.drive.drivelink.download.domain.usecase.Download
import me.proton.core.drive.drivelink.download.domain.usecase.MoveFileIfExists
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkoffline.domain.usecase.IsAnyAncestorMarkedAsOffline
import me.proton.core.drive.linkoffline.domain.usecase.IsLinkOrAnyAncestorMarkedAsOffline
import me.proton.core.drive.linkoffline.domain.usecase.ToggleOffline
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class ToggleOffline @Inject constructor(
    private val toggleOffline: ToggleOffline,
    private val isLinkOrAnyAncestorMarkedAsOffline: IsLinkOrAnyAncestorMarkedAsOffline,
    private val isAnyAncestorMarkedAsOffline: IsAnyAncestorMarkedAsOffline,
    private val download: Download,
    private val cancelDownload: CancelDownload,
    private val moveFileIfExists: MoveFileIfExists,
    private val announceEvent: AnnounceEvent,
) {
    suspend operator fun invoke(driveLink: DriveLink, isRetryable: Boolean = true) = coRunCatching {
        val isAvailableOffline = !isLinkOrAnyAncestorMarkedAsOffline(driveLink.id)
        toggleOffline(driveLink.id)
        if (driveLink is DriveLink.File) {
            moveFileIfExists(driveLink).getOrThrow()
        }
        if (isAvailableOffline) {
            if (driveLink.downloadState == DownloadState.Ready) {
                announceEvent(driveLink.id.userId, Event.RecentlyAccessed.MarkedAsOffline(driveLink.id))
            }
            download(driveLink, isRetryable).getOrThrow()
        } else {
            val isAnyAncestorMarkedAsOffline = isAnyAncestorMarkedAsOffline(driveLink.id)
                .onFailure { error ->
                    CoreLogger.w(
                        tag = LogTag.DEFAULT,
                        e = error,
                        message = "Failed getting isAnyAncestorMarkedAsOffline",
                    )
                }
                .getOrDefault(false)
            if (!isAnyAncestorMarkedAsOffline) {
                cancelDownload(driveLink)
            }
        }
    }
}
