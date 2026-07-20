/*
 * Copyright (c) 2025 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.sdk

import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.LogTag.DRIVE_SDK
import me.proton.core.drive.base.domain.usecase.ReportError
import me.proton.core.drive.observability.data.extension.toType
import me.proton.core.drive.observability.data.extension.toVolumeType
import me.proton.core.drive.observability.domain.constraint.MinimumIntervalConstraint
import me.proton.core.drive.observability.domain.metrics.common.Plan
import me.proton.core.drive.observability.domain.metrics.common.ResultStatus
import me.proton.core.drive.observability.domain.metrics.common.VolumeType
import me.proton.core.drive.observability.domain.metrics.sdk.UploadErroringUsersTotal
import me.proton.core.drive.observability.domain.metrics.sdk.UploadErrorsFileSizeHistogram
import me.proton.core.drive.observability.domain.metrics.sdk.UploadErrorsTotal
import me.proton.core.drive.observability.domain.metrics.sdk.UploadErrorsTransferSizeHistogram
import me.proton.core.drive.observability.domain.metrics.sdk.UploadSuccessRateTotal
import me.proton.core.drive.observability.domain.usecase.EnqueueObservabilityEvent
import me.proton.core.drive.user.domain.extension.isWithoutProtonSubscription
import me.proton.drive.sdk.telemetry.UploadError
import me.proton.drive.sdk.telemetry.UploadEvent
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class SdkUploadMetricsNotifier @Inject constructor(
    private val enqueueObservabilityEvent: EnqueueObservabilityEvent,
    private val minimumIntervalConstraint: MinimumIntervalConstraint,
    private val getPrimaryUser: GetPrimaryUser,
    private val reportError: ReportError,
) {

    suspend operator fun invoke(uploadEvent: UploadEvent) {
        val volumeType = uploadEvent.volumeType.toVolumeType()
        val error = uploadEvent.error
        if (error != UploadError.NETWORK_ERROR && error != UploadError.VALIDATION_ERROR) {
            notifyUploadSuccessRateTotalMetric(
                volumeType = volumeType,
                isSuccess = error == null,
            )
        }
        if (error != null) {
            val type = error.toType()
            if (type == UploadErrorsTotal.Type.unknown) {
                reportError(
                    tag = DRIVE_SDK,
                    message = "Unknown upload error: ${uploadEvent.originalError}",
                )
            }
            if (type == UploadErrorsTotal.Type.integrity_error) {
                reportError(
                    tag = DRIVE_SDK,
                    message = "Integrity upload error: ${uploadEvent.originalError}",
                )
            }
            notifyUploadErrorsTotalMetric(
                volumeType = volumeType,
                type = type
            )
            if (error != UploadError.NETWORK_ERROR && error != UploadError.VALIDATION_ERROR) {
                notifyUploadErroringUsersTotalMetric(
                    volumeType = volumeType,
                )
            }
            notifyUploadErrorsTransferSizeHistogramMetric(
                uploadedSize = uploadEvent.approximateUploadedSize
            )
            notifyUploadErrorsFileSizeHistogramMetric(
                fileSize = uploadEvent.approximateExpectedSize
            )
        }
    }

    private suspend fun notifyUploadSuccessRateTotalMetric(
        volumeType: VolumeType,
        isSuccess: Boolean,
    ) {
        enqueueObservabilityEvent(
            UploadSuccessRateTotal(
                Labels = UploadSuccessRateTotal.LabelsData(
                    status = if (isSuccess) ResultStatus.success else ResultStatus.failure,
                    volumeType = volumeType,
                )
            )
        )
    }

    private suspend fun notifyUploadErrorsTotalMetric(
        volumeType: VolumeType,
        type: UploadErrorsTotal.Type,
    ) {
        enqueueObservabilityEvent(
            UploadErrorsTotal(
                Labels = UploadErrorsTotal.LabelsData(
                    volumeType = volumeType,
                    type = type,
                )
            )
        )
    }

    private suspend fun notifyUploadErroringUsersTotalMetric(
        volumeType: VolumeType,
    ) {
        getPrimaryUser()?.let { user ->
            notifyUploadErroringUsersTotalMetric(
                userId = user.userId,
                volumeType = volumeType,
                isFreeUser = user.isWithoutProtonSubscription,
            )
        }
    }

    private suspend fun notifyUploadErroringUsersTotalMetric(
        userId: UserId,
        volumeType: VolumeType,
        isFreeUser: Boolean,
    ) {
        enqueueObservabilityEvent(
            UploadErroringUsersTotal(
                Labels = UploadErroringUsersTotal.LabelsData(
                    volumeType = volumeType,
                    userPlan = if (isFreeUser) Plan.free else Plan.paid,
                )
            ),
            constraint = minimumIntervalConstraint(
                userId = userId,
                schemaId = UploadErroringUsersTotal.SCHEMA_ID,
                interval = 5.minutes,
            )
        )
    }

    private suspend fun notifyUploadErrorsTransferSizeHistogramMetric(
        uploadedSize: Long,
    ) {
        enqueueObservabilityEvent(
            UploadErrorsTransferSizeHistogram(
                Value = uploadedSize
            )
        )
    }

    private suspend fun notifyUploadErrorsFileSizeHistogramMetric(
        fileSize: Long,
    ) {
        enqueueObservabilityEvent(
            UploadErrorsFileSizeHistogram(
                Value = fileSize
            )
        )
    }
}
