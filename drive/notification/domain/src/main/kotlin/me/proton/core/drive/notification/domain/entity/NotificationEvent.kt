/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.notification.domain.entity

import kotlinx.serialization.Serializable
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.entity.TimestampMs

@Serializable
sealed class NotificationEvent {
    abstract val id: String
    abstract val occurredAt: TimestampMs
    @Serializable
    data class StorageFull(val needed: Bytes) : NotificationEvent() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs(System.currentTimeMillis())
    }

    @Serializable
    data class Upload(val state: UploadState, val uploadFileLinkId: Long, val percentage: Percentage) : NotificationEvent() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_$uploadFileLinkId"
        override val occurredAt: TimestampMs = TimestampMs(System.currentTimeMillis())
        enum class UploadState {
            NEW_UPLOAD,
            UPLOADING,
            UPLOAD_COMPLETE,
            UPLOAD_FAILED,
            UPLOAD_CANCELLED,
        }
    }

    @Serializable
    data class Download(val downloadId: String, val downloadedFiles: Int, val totalFiles: Int) : NotificationEvent() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_$downloadId"
        override val occurredAt: TimestampMs = TimestampMs(System.currentTimeMillis())
    }

    object ForcedSignOut : NotificationEvent() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs(System.currentTimeMillis())
    }

    object NoSpaceLeftOnDevice : NotificationEvent() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs(System.currentTimeMillis())
    }

    companion object {
        private const val EVENT_ID_PREFIX = "NOTIFICATION_EVENT_ID_"
    }
}
