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

import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.download.domain.usecase.CancelDownload
import me.proton.core.drive.drivelink.download.domain.usecase.Download
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
) {
    suspend operator fun invoke(driveLink: DriveLink, isRetryable: Boolean = true) {
        val isAvailableOffline = !isLinkOrAnyAncestorMarkedAsOffline(driveLink.id)
        toggleOffline(driveLink.id)
        if (isAvailableOffline) {
            download(driveLink, isRetryable)
        } else {
            val isAnyAncestorMarkedAsOffline = isAnyAncestorMarkedAsOffline(driveLink.id)
                .onFailure { error ->
                    CoreLogger.d(
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
