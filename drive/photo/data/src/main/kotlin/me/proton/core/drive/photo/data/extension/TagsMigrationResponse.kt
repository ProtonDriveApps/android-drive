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

package me.proton.core.drive.photo.data.extension

import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.data.api.response.TagsMigrationResponse
import me.proton.core.drive.photo.domain.entity.TagsMigrationAnchor
import me.proton.core.drive.photo.domain.entity.TagsMigrationStatus
import me.proton.core.drive.share.domain.entity.ShareId

fun TagsMigrationResponse.toEntity(shareId: ShareId) = TagsMigrationStatus(
    finished = finished,
    anchor = anchor?.let { anchor ->
        TagsMigrationAnchor(
            lastProcessedLinkId = FileId(shareId, anchor.lastProcessedLinkId),
            lastProcessedCaptureTime = TimestampS(anchor.lastProcessedCaptureTime),
            currentTimestamp = TimestampS(anchor.lastMigrationTimestamp),
            clientUid = anchor.lastClientUid,
        )
    }
)
