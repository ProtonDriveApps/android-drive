/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.upload.data.usecase

import me.proton.core.drive.backup.domain.usecase.GetBackupFile
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.UploadTag.logTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.extension.toInitiator
import me.proton.core.drive.observability.data.extension.toShareType
import me.proton.core.drive.observability.domain.metrics.UploadErrorsTotal
import me.proton.core.drive.observability.domain.metrics.UploadSuccessRateTotal
import me.proton.core.drive.observability.domain.metrics.common.BooleanStatus
import me.proton.core.drive.observability.domain.metrics.common.ResultStatus
import me.proton.core.drive.observability.domain.metrics.common.ShareType
import me.proton.core.drive.observability.domain.usecase.EnqueueObservabilityEvent
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.upload.data.extension.toUploadErrorType
import me.proton.core.drive.upload.domain.usecase.UploadMetricsNotifier
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class UploadMetricsNotifierImpl @Inject constructor(
    private val getShare: GetShare,
    private val getBackupFile: GetBackupFile,
    private val enqueueObservabilityEvent: EnqueueObservabilityEvent,
) : UploadMetricsNotifier {

    override suspend operator fun invoke(
        uploadFileLink: UploadFileLink,
        isSuccess: Boolean,
        throwable: Throwable?,
        excludedErrorTypes: Set<UploadErrorsTotal.Type>,
    ) {
        require((isSuccess && throwable == null) || !isSuccess) {
            "When isSuccess is true, throwable must be null"
        }
        val errorType = throwable?.toUploadErrorType()
        if (errorType == null || errorType !in excludedErrorTypes) {
            notifyUploadSuccessRateTotalMetric(
                uploadFileLink = uploadFileLink,
                isSuccess = isSuccess,
            )
        }
    }

    private suspend fun notifyUploadSuccessRateTotalMetric(
        uploadFileLink: UploadFileLink,
        isSuccess: Boolean,
    ) {
        with (uploadFileLink) {
            notifyUploadSuccessRateTotalMetric(
                uploadFileLink = uploadFileLink,
                isSuccess = isSuccess,
                isReattempted = uriString?.let { uri ->
                    getBackupFile(parentLinkId, uri).getOrNull(uploadFileLink.id.logTag(), "Failed getting backup file")
                        ?.let { backupFile ->
                            CoreLogger.d(uploadFileLink.id.logTag(), "Backup file found, attempts ${backupFile.attempts}")
                            backupFile.attempts > 0
                        } ?: false
                } ?: false
            )
        }
    }

    private suspend fun notifyUploadSuccessRateTotalMetric(
        uploadFileLink: UploadFileLink,
        isSuccess: Boolean,
        isReattempted: Boolean,
    ) {
        getShare(uploadFileLink.shareId)
            .toResult()
            .getOrNull(uploadFileLink.id.logTag(), "Failed getting share")
            ?.let { share ->
                coRunCatching {
                    share.type.toShareType()
                }.getOrNull(uploadFileLink.id.logTag(), "Converting share type failed")
            }
            ?.let { shareType ->
                notifyUploadSuccessRateTotalMetric(
                    uploadFileLink = uploadFileLink,
                    isSuccess = isSuccess,
                    isReattempted = isReattempted,
                    shareType = shareType,
                )
            }
    }

    private suspend fun notifyUploadSuccessRateTotalMetric(
        uploadFileLink: UploadFileLink,
        isSuccess: Boolean,
        isReattempted: Boolean,
        shareType: ShareType,
    ) {
        enqueueObservabilityEvent(
            UploadSuccessRateTotal(
                Labels = UploadSuccessRateTotal.LabelsData(
                    status = if (isSuccess) ResultStatus.success else ResultStatus.failure,
                    retry = if (isReattempted) BooleanStatus.`true` else BooleanStatus.`false`,
                    shareType = shareType,
                    initiator = uploadFileLink.toInitiator(),
                )
            )
        )
    }
}
