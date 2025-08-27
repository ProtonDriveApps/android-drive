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

package me.proton.core.drive.drivelink.download.data.usecase

import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.download.data.extension.toDownloadErrorType
import me.proton.core.drive.drivelink.download.domain.usecase.DownloadMetricsNotifier
import me.proton.core.drive.drivelink.download.domain.usecase.GetNumberOfDownloadFileRetries
import me.proton.core.drive.feature.flag.domain.usecase.IsDownloadManagerEnabled
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.observability.data.extension.toShareType
import me.proton.core.drive.observability.domain.metrics.DownloadErrorsTotal
import me.proton.core.drive.observability.domain.metrics.DownloadSuccessRateTotal
import me.proton.core.drive.observability.domain.metrics.common.BooleanStatus
import me.proton.core.drive.observability.domain.metrics.common.ResultStatus
import me.proton.core.drive.observability.domain.metrics.common.ShareType
import me.proton.core.drive.observability.domain.usecase.EnqueueObservabilityEvent
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class DownloadMetricsNotifierImpl @Inject constructor(
    private val enqueueObservabilityEvent: EnqueueObservabilityEvent,
    private val getShare: GetShare,
    private val isDownloadManagerEnabled: IsDownloadManagerEnabled,
    private val getNumberOfDownloadFileRetries: GetNumberOfDownloadFileRetries,
) : DownloadMetricsNotifier {

    override suspend fun invoke(
        fileId: FileId,
        isSuccess: Boolean,
        throwable: Throwable?,
        excludedErrorTypes: Set<DownloadErrorsTotal.Type>,
    ) {
        require((isSuccess && throwable == null) || !isSuccess) {
            "When isSuccess is true, throwable must be null"
        }
        val errorType = throwable?.toDownloadErrorType()
        if (errorType == null || errorType !in excludedErrorTypes) {
            notifyDownloadSuccessRateTotalMetric(
                fileId = fileId,
                isSuccess = isSuccess,
            )
        }
    }

    private suspend fun notifyDownloadSuccessRateTotalMetric(
        fileId: FileId,
        isSuccess: Boolean,
    ) {
        getShare(fileId.shareId)
            .toResult()
            .getOrNull(fileId.logTag, "Failed getting share")
            ?.let { share ->
                notifyDownloadSuccessRateTotalMetric(
                    fileId = fileId,
                    share = share,
                    isSuccess = isSuccess,
                )
            }
    }

    private suspend fun notifyDownloadSuccessRateTotalMetric(
        fileId: FileId,
        share: Share,
        isSuccess: Boolean,
    ) {
        val isReattempted = if (isDownloadManagerEnabled(fileId.userId)) {
            getNumberOfDownloadFileRetries(share.volumeId, fileId)
                .getOrNull(fileId.logTag)
                ?.let { retries -> retries > 0 }
                ?: false

        } else {
            false
        }
        coRunCatching { share.type.toShareType() }
            .getOrNull(fileId.logTag, "Converting share type failed")
            ?.let { shareType ->
                notifyDownloadSuccessRateTotalMetric(
                    shareType = shareType,
                    isSuccess = isSuccess,
                    isReattempted = isReattempted,
                )
            }
    }

    private suspend fun notifyDownloadSuccessRateTotalMetric(
        shareType: ShareType,
        isSuccess: Boolean,
        isReattempted: Boolean,
    ) {
        enqueueObservabilityEvent(
            DownloadSuccessRateTotal(
                Labels = DownloadSuccessRateTotal.LabelsData(
                    status = if (isSuccess) ResultStatus.success else ResultStatus.failure,
                    retry = if (isReattempted) BooleanStatus.`true` else BooleanStatus.`false`,
                    shareType = shareType,
                )
            )
        )
    }

    private val FileId.logTag: String get() = buildString {
        append(LogTag.DOWNLOAD)
        append(".")
        append(id.logId())
    }
}
