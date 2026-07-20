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
import me.proton.core.drive.observability.domain.constraint.CountConstraint
import me.proton.core.drive.observability.domain.constraint.MinimumIntervalConstraint
import me.proton.core.drive.observability.domain.metrics.common.Plan
import me.proton.core.drive.observability.domain.metrics.common.ResultStatus
import me.proton.core.drive.observability.domain.metrics.common.VolumeType
import me.proton.core.drive.observability.domain.metrics.sdk.DownloadErroringUsersTotal
import me.proton.core.drive.observability.domain.metrics.sdk.DownloadErrorsFileSizeHistogram
import me.proton.core.drive.observability.domain.metrics.sdk.DownloadErrorsTotal
import me.proton.core.drive.observability.domain.metrics.sdk.DownloadErrorsTransferSizeHistogram
import me.proton.core.drive.observability.domain.metrics.sdk.DownloadSuccessRateTotal
import me.proton.core.drive.observability.domain.usecase.EnqueueObservabilityEvent
import me.proton.core.drive.user.domain.extension.isWithoutProtonSubscription
import me.proton.drive.sdk.telemetry.DownloadError
import me.proton.drive.sdk.telemetry.DownloadEvent
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class SdkDownloadMetricsNotifier @Inject constructor(
    private val enqueueObservabilityEvent: EnqueueObservabilityEvent,
    private val minimumIntervalConstraint: MinimumIntervalConstraint,
    private val countConstraint: CountConstraint,
    private val getPrimaryUser: GetPrimaryUser,
    private val reportError: ReportError,
){

    suspend operator fun invoke(downloadEvent: DownloadEvent) {
        val volumeType = downloadEvent.volumeType.toVolumeType()
        val error = downloadEvent.error
        if (error != DownloadError.NETWORK_ERROR && error != DownloadError.VALIDATION_ERROR) {
            notifyDownloadSuccessRateTotalMetric(
                volumeType = volumeType,
                isSuccess = error == null,
            )
        }
        if (error != null) {
            val type = error.toType()
            if (type == DownloadErrorsTotal.Type.unknown) {
                reportError(
                    tag = DRIVE_SDK,
                    message = "Unknown download error: ${downloadEvent.originalError}",
                )
            }
            if (type == DownloadErrorsTotal.Type.integrity_error) {
                reportError(
                    tag = DRIVE_SDK,
                    message = "Integrity download error: ${downloadEvent.originalError}",
                )
            }
            notifyDownloadErrorsTotalMetric(
                volumeType = volumeType,
                type = type
            )

            if (error != DownloadError.NETWORK_ERROR && error != DownloadError.VALIDATION_ERROR) {
                notifyDownloadErroringUsersTotalMetric(
                    volumeType = volumeType,
                )
            }
            notifyDownloadErrorsTransferSizeHistogramMetric(
                downloadedSize = downloadEvent.approximateDownloadedSize
            )
            notifyDownloadErrorsFileSizeHistogramMetric(
                fileSize = downloadEvent.approximateClaimedFileSize
            )
        }
    }

    private suspend fun notifyDownloadSuccessRateTotalMetric(
        volumeType: VolumeType,
        isSuccess: Boolean,
    ) {
        enqueueObservabilityEvent(
            DownloadSuccessRateTotal(
                Labels = DownloadSuccessRateTotal.LabelsData(
                    status = if (isSuccess) ResultStatus.success else ResultStatus.failure,
                    volumeType = volumeType,
                )
            )
        )
    }

    private suspend fun notifyDownloadErrorsTotalMetric(
        volumeType: VolumeType,
        type: DownloadErrorsTotal.Type,
    ) {
        enqueueObservabilityEvent(
            DownloadErrorsTotal(
                Labels = DownloadErrorsTotal.LabelsData(
                    volumeType = volumeType,
                    type = type,
                )
            )
        )
    }

    private suspend fun notifyDownloadErroringUsersTotalMetric(
        volumeType: VolumeType,
    ) {
        getPrimaryUser()?.let { user ->
            notifyDownloadErroringUsersTotalMetric(
                userId = user.userId,
                volumeType = volumeType,
                isFreeUser = user.isWithoutProtonSubscription,
            )
        }
    }

    private suspend fun notifyDownloadErroringUsersTotalMetric(
        userId: UserId,
        volumeType: VolumeType,
        isFreeUser: Boolean,
    ) {
        enqueueObservabilityEvent(
            DownloadErroringUsersTotal(
                Labels = DownloadErroringUsersTotal.LabelsData(
                    volumeType = volumeType,
                    userPlan = if (isFreeUser) Plan.free else Plan.paid,
                )
            ),
            constraint = minimumIntervalConstraint(
                userId = userId,
                schemaId = DownloadErroringUsersTotal.SCHEMA_ID,
                interval = 5.minutes,
            )
        )
    }

    private suspend fun notifyDownloadErrorsTransferSizeHistogramMetric(
        downloadedSize: Long,
    ) {
        enqueueObservabilityEvent(
            DownloadErrorsTransferSizeHistogram(
                Value = downloadedSize
            )
        )
    }

    private suspend fun notifyDownloadErrorsFileSizeHistogramMetric(
        fileSize: Long,
    ) {
        enqueueObservabilityEvent(
            DownloadErrorsFileSizeHistogram(
                Value = fileSize
            )
        )
    }
}
