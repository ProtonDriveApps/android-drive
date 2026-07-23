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
import me.proton.core.drive.link.domain.entity.AlbumId
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
            ERROR_INTEGRITY,
        }

    }

    @Serializable
    data class UploadSpeed(
        val bytes: Bytes,
        val elapsedTime: TimestampMs,
        val usedSdk: Boolean = true,
    ) : Event() {
        override val id: String =
            "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    @Serializable
    data class DownloadSpeed(
        val bytes: Bytes,
        val elapsedTime: TimestampMs,
        val usedSdk: Boolean = true,
    ) : Event() {
        override val id: String =
            "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()
    }

    @Serializable
    data class Backup(
        val folderId: FolderId,
        val state: BackupState,
        val total: Int = 0,
        val preparing: Int = 0,
        val pending: Int = 0,
        val failed: Int = 0,
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
        }
    }

    @Serializable
    data class BackupFolder(
        val folderId: FolderId,
        val bucketId: Int,
        val state: Backup.BackupState,
        val total: Int,
        val preparing: Int,
        val pending: Int,
        val failed: Int,
    ) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}"
        override val occurredAt: TimestampMs = TimestampMs()
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

    @Serializable
    data class DownloadFileProgress(
        val downloadingCount: Int,
        val progress: Percentage,
    ) : Event() {
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
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
            val source: Source = Source.Unknown,
        )

        enum class Source {
            Unknown, Network, Cache,
        }
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
        val currentNetworkStatus: String? = null,
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

    sealed class Album : Event() {

        data class Created(
            val albumId: AlbumId,
        ) : Album() {
            override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_${albumId.id}"
            override val occurredAt: TimestampMs = TimestampMs()
        }

        data class CreationFailed(
            val error: kotlin.Throwable,
        ) : Album() {
            override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
            override val occurredAt: TimestampMs = TimestampMs()
        }
    }

    sealed class Sentry : Event() {
        abstract val message: String
        abstract val tags: List<Pair<String, String>>
        abstract val level: Level
        override val id: String = "$EVENT_ID_PREFIX${this.javaClass.simpleName.uppercase()}_1"
        override val occurredAt: TimestampMs = TimestampMs()

        data class AppPromotion(
            val action: Action? = null,
        ): Sentry() {
            override val message: String get() = "App"
            override val level: Level get() = Level.INFO
            override val tags: List<Pair<String, String>> get() =
                listOfNotNull(
                    Tags.EVENT_TYPE.value to EventType.APP_PROMOTION.name.lowercase(),
                    action?.let { Tags.ACTION.value to action.name.lowercase() },
                )

            enum class Tags(val value: String) {
                EVENT_TYPE("event.type"),
                ACTION("app.promo.action")
            }

            enum class Action {
                SCREEN_SHOWN,
                LIKE_IT,
                COULD_BE_BETTER,
            }
        }

        data class Upsell(
            val action: Payments.Action? = null,
            val result: Payments.Result? = null,
            val failureReason: Payments.FailureReason? = null,
        ) : Sentry() {
            override val message: String get() = "Upsell"
            override val level: Level get() = Level.INFO
            override val tags: List<Pair<String, String>> get() =
                listOfNotNull(
                    Payments.Tags.EVENT_TYPE.value to EventType.UPSELL.name.lowercase(),
                    Payments.Tags.PROMO_ID.value to "upsell",
                    action?.let { Payments.Tags.PROMO_ACTION.value to action.name.lowercase() },
                    result?.let { Payments.Tags.PROMO_RESULT.value to result.name.lowercase() },
                    failureReason?.let { Payments.Tags.PROMO_FAILURE_REASON.value to failureReason.name.lowercase() }
                )
        }

        data class SummerSale2026(
            val action: Payments.Action? = null,
            val result: Payments.Result? = null,
            val failureReason: Payments.FailureReason? = null,
        ) : Sentry() {
            override val message: String get() = "Summer Sale 2026"
            override val level: Level get() = Level.INFO
            override val tags: List<Pair<String, String>> get() =
                listOfNotNull(
                    Payments.Tags.EVENT_TYPE.value to EventType.PROMOTION.name.lowercase(),
                    Payments.Tags.PROMO_ID.value to "summer_2026",
                    action?.let { Payments.Tags.PROMO_ACTION.value to action.name.lowercase() },
                    result?.let { Payments.Tags.PROMO_RESULT.value to result.name.lowercase() },
                    failureReason?.let { Payments.Tags.PROMO_FAILURE_REASON.value to failureReason.name.lowercase() }
                )
        }

        object Payments {
            enum class Tags(val value: String) {
                EVENT_TYPE("event.type"),
                PROMO_ID("promo.id"),
                PROMO_ACTION("promo.action"),
                PROMO_RESULT("promo.result"),
                PROMO_FAILURE_REASON("promo.failure.reason")
            }

            enum class Action {
                SCREEN_SHOWN,
                CLAIM_OFFER
            }

            enum class Result {
                SUCCESS,
                FAILURE
            }

            enum class FailureReason {
                EmptyCustomerId,
                Generic,
                GiapUnredeemed,
                SubscriptionManagedByOtherApp,
                GoogleProductDetailsNotFound,
                PurchaseNotFound,
                RecoverableBillingError,
                UnrecoverableBillingError,
                UnsupportedPaymentProvider,
                UserCancelled
            }
        }

        enum class Level {
            ERROR,
            WARNING,
            DEBUG,
            INFO,
            FATAL,
        }

        enum class EventType {
            PROMOTION,
            UPSELL,
            APP_PROMOTION,
        }
    }

    companion object {
        private const val EVENT_ID_PREFIX = "NOTIFICATION_EVENT_ID_"
    }
}
