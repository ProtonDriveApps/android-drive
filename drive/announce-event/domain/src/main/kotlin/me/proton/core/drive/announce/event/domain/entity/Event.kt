/*
 * Copyright (c) 2022-2024 Proton AG.
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
package me.proton.core.drive.announce.event.domain.entity

import kotlinx.serialization.Serializable
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.key.domain.entity.key.PublicKey
import java.util.UUID

@Serializable
sealed class Event {
    abstract val id: String
    abstract val occurredAt: TimestampMs

    @Serializable
    data class StorageFull(val needed: Bytes) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    @Serializable
    data class Upload(
        val state: UploadState,
        val uploadFileLinkId: Long,
        val percentage: Percentage,
        val shouldShow: Boolean,
        val reason: Reason? = null,
    ) : Event() {
        override val id: String =
            "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_$uploadFileLinkId"
        override val occurredAt: TimestampMs = TimestampMs()

        enum class UploadState {
            NEW_UPLOAD,
            UPLOADING,
            UPLOAD_COMPLETE,
            UPLOAD_FAILED,
            UPLOAD_CANCELLED,
        }

        enum class Reason {
            ERROR_OTHER,
            ERROR_PERMISSIONS,
            ERROR_DRIVE_STORAGE,
            ERROR_LOCAL_STORAGE,
            ERROR_NOT_ALLOWED,
        }

    }

    @Serializable
    data class Backup(
        val folderId: FolderId,
        val state: BackupState,
        val totalBackupPhotos: Int,
        val pendingBackupPhotos: Int,
    ) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}"
        override val occurredAt: TimestampMs = TimestampMs()

        enum class BackupState {
            IN_PROGRESS,
            COMPLETE,
            FAILED,
            FAILED_CONNECTIVITY,
            FAILED_WIFI_CONNECTIVITY,
            FAILED_PERMISSION,
            FAILED_LOCAL_STORAGE,
            FAILED_DRIVE_STORAGE,
            FAILED_PHOTOS_UPLOAD_NOT_ALLOWED,
            PAUSED_DISABLED,
            UNCOMPLETED,
            PAUSE_BACKGROUND_RESTRICTIONS,
            PREPARING,
            FAILED_DUE_PHOTO_SHARE_MIGRATION,
        }
    }

    data class BackupEnabled(val folderId: FolderId) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    data class BackupDisabled(val folderId: FolderId) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    data class BackupStarted(val folderId: FolderId) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    data class BackupStopped(val folderId: FolderId, val state: Backup.BackupState) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    data class BackupCompleted(val folderId: FolderId) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    data class BackupSync(val folderId: FolderId, val bucketId: Int) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    @Serializable
    data class Download(val downloadId: String, val downloadedFiles: Int, val totalFiles: Int) :
        Event() {
        override val id: String =
            "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_$downloadId"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    data object ForcedSignOut : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    data object NoSpaceLeftOnDevice : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    data class Throwable(
        val message: String,
        val throwable: kotlin.Throwable,
        val level: Logger.Level,
    ) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    data class Network(
        val request: Request,
        val response: Response,
    ) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()

        data class Request(
            val occurredAt: TimestampMs,
            val method: String,
            val urlPath: String,
        )

        data class Response(
            val occurredAt: TimestampMs,
            val code: Int,
            val message: String,
            val jsonBody: String? = null,
        )
    }

    data class Logger(
        val tag: String,
        val message: String,
        val level: Level,
    ) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()

        enum class Level {
            ERROR,
            WARNING,
            DEBUG,
            INFO,
            VERBOSE,
        }
    }

    data class Screen(
        val source: String,
        val name: String?,
    ) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    data class ApplicationState(
        val inForeground: Boolean,
        val connectivity: String,
    ) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    data class Workers(
        val infos: List<Infos>,
    ) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()

        data class Infos(
            val id: UUID,
            val name: String,
            val state: State,
            val attempts: Int,
        ) {
            enum class State {
                ENQUEUED,
                RUNNING,
                SUCCEEDED,
                FAILED,
                BLOCKED,
                CANCELLED,
            }
        }
    }

    data class SignatureVerificationFailed(
        val usedPublicKeys: List<PublicKey>,
    ) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    data object TransferData : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    companion object {
        private const val EVENT_ID_PREFIX = "NOTIFICATION_EVENT_ID_"
    }
}
