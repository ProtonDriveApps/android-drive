/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.telemetry.domain.event

import me.proton.core.drive.telemetry.domain.entity.DriveTelemetryEvent
import me.proton.core.drive.telemetry.domain.extension.toYesOrNo

@Suppress("FunctionNaming")
object PhotosEvent {

    enum class Reason(internal val group: String, internal val key: String) {
        COMPLETED("completed", "completed"),
        FAILED_OTHER("failed", "other"),
        FAILED_PERMISSIONS("failed", "permissions"),
        FAILED_DRIVE_STORAGE("failed", "out of drive storage"),
        FAILED_LOCAL_STORAGE("failed", "out of local storage"),
        FAILED_NOT_ALLOWED("failed", "feature flag"),
        PAUSED_CONNECTIVITY("paused", "no connection"),
        PAUSED_DISABLED("paused", "disabled by user"),
        PAUSED_BACKGROUND_RESTRICTIONS("paused", "background mode expired"),
    }

    const val group = "drive.any.photos"
    fun SettingDisabled() = DriveTelemetryEvent(
        group = group,
        name = "setting.disabled",
    )

    fun SettingEnabled() = DriveTelemetryEvent(
        group = group,
        name = "setting.enabled",
    )

    fun UploadDone(duration: Long, sizeKB: Long, reason: Reason) = DriveTelemetryEvent(
        group = group,
        name = "upload.done",
        values = mapOf(
            "duration_seconds" to duration.toFloat(),
            "kilobytes_uploaded" to sizeKB.toFloat(),
        ),
        dimensions = mapOf(
            "reason_group" to reason.group,
            "reason" to reason.key,
        )
    )

    fun BackupStopped(
        duration: Long,
        files: Long,
        size: Long,
        reason: Reason,
        isInitialBackup: Boolean,
    ) =
        DriveTelemetryEvent(
            group = group,
            name = "backup.stopped",
            values = mapOf(
                "duration_seconds" to duration.toFloat(),
                "files_uploaded" to files.toFloat(),
                "bytes_uploaded" to size.toFloat(),
            ),
            dimensions = mapOf(
                "is_initial_backup" to isInitialBackup.toYesOrNo(),
                "reason_group" to reason.group,
                "reason" to reason.key,
            )
        )

    fun UpsellPhotosAccepted() = UpsellPhotos("accepted")

    fun UpsellPhotosDeclined() = UpsellPhotos("declined")

    internal fun UpsellPhotos(answer: String) = DriveTelemetryEvent(
        group = group,
        name = "upsell_photos",
        dimensions = mapOf(
            "answer" to answer
        )
    )
}
