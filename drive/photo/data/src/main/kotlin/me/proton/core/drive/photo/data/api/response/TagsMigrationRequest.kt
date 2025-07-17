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

package me.proton.core.drive.photo.data.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.ANCHOR
import me.proton.core.drive.base.data.api.Dto.CLIENT_UID
import me.proton.core.drive.base.data.api.Dto.CURRENT_TIMESTAMP
import me.proton.core.drive.base.data.api.Dto.FINISHED
import me.proton.core.drive.base.data.api.Dto.LAST_PROCESSED_CAPTURE_TIME
import me.proton.core.drive.base.data.api.Dto.LAST_PROCESSED_LINK_ID

@Serializable
data class TagsMigrationRequest(
    @SerialName(FINISHED)
    val finished: Boolean,
    @SerialName(ANCHOR)
    val anchor: Anchor? = null,
) {
    @Serializable
    data class Anchor(
        @SerialName(LAST_PROCESSED_LINK_ID)
        val lastProcessedLinkId: String,
        @SerialName(LAST_PROCESSED_CAPTURE_TIME)
        val lastProcessedCaptureTime: Long,
        @SerialName(CURRENT_TIMESTAMP)
        val currentTimestamp: Long,
        @SerialName(CLIENT_UID)
        val clientUid: String? = null,
    )
}
