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

package me.proton.core.drive.drivelink.download.data.handler

import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.download.data.extension.toDownloadErrorType
import me.proton.core.drive.drivelink.download.domain.handler.DownloadErrorHandler
import me.proton.core.drive.drivelink.download.domain.manager.DownloadErrorManager
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.observability.data.extension.toShareType
import me.proton.core.drive.observability.domain.constraint.CountConstraint
import me.proton.core.drive.observability.domain.constraint.MinimumIntervalConstraint
import me.proton.core.drive.observability.domain.metrics.DownloadErroringUsersTotal
import me.proton.core.drive.observability.domain.metrics.DownloadErrorsTotal
import me.proton.core.drive.observability.domain.metrics.DownloadInitiator
import me.proton.core.drive.observability.domain.metrics.common.ShareType
import me.proton.core.drive.observability.domain.usecase.EnqueueObservabilityEvent
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.user.domain.extension.isFree
import me.proton.core.user.domain.usecase.GetUser
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class ObservabilityDownloadErrorHandler @Inject constructor(
    private val getShare: GetShare,
    private val enqueueObservabilityEvent: EnqueueObservabilityEvent,
    private val countConstraint: CountConstraint,
    private val minimumIntervalConstraint: MinimumIntervalConstraint,
    private val getUser: GetUser,
) : DownloadErrorHandler {

    override suspend fun onError(downloadError: DownloadErrorManager.Error) {
        coRunCatching {
            val shareType = getShareType(downloadError.fileId.shareId)
            notifyDownloadErrorsTotalMetric(downloadError, shareType)
            notifyDownloadErroringUsersTotalMetric(downloadError, shareType)
        }.onFailure { error ->
            error.log(
                tag = downloadError.logTag,
                message = "Cannot handle error for: ${downloadError.fileId.id.logId()}",
            )
        }
    }

    private suspend fun getShareType(shareId: ShareId): ShareType =
        getShare(shareId)
            .toResult()
            .onFailure { error -> error.log(LogTag.DOWNLOAD, "Failed getting share") }
            .getOrThrow()
            .type.toShareType()

    private suspend fun notifyDownloadErrorsTotalMetric(
        downloadError: DownloadErrorManager.Error,
        shareType: ShareType,
    ) {
        if (downloadError.isCancelledByUser.not()) {
            enqueueObservabilityEvent(
                DownloadErrorsTotal(
                    Labels = DownloadErrorsTotal.LabelsData(
                        type = downloadError.throwable.toDownloadErrorType(),
                        shareType = shareType,
                        initiator = DownloadInitiator.download,
                    )
                ),
                constraint = countConstraint(
                    userId = downloadError.fileId.userId,
                    key = downloadError.key,
                    maxCount = 1,
                )
            )
        }
    }

    private suspend fun notifyDownloadErroringUsersTotalMetric(
        downloadError: DownloadErrorManager.Error,
        shareType: ShareType,
        excludedErrorTypes: Set<DownloadErrorsTotal.Type> = setOf(
            DownloadErrorsTotal.Type.network_error,
        )
    ) {
        if (downloadError.throwable.toDownloadErrorType() !in excludedErrorTypes &&
            downloadError.isCancelledByUser.not()
        ) {
            val user = getUser(downloadError.fileId.userId, refresh = false)
            notifyDownloadErroringUsersTotalMetric(downloadError, shareType, user.isFree)
        }
    }

    private suspend fun notifyDownloadErroringUsersTotalMetric(
        downloadError: DownloadErrorManager.Error,
        shareType: ShareType,
        isFreeUser: Boolean,
    ) {
        enqueueObservabilityEvent(
            DownloadErroringUsersTotal(
                Labels = DownloadErroringUsersTotal.LabelsData(
                    plan = if (isFreeUser) DownloadErroringUsersTotal.Plan.free else DownloadErroringUsersTotal.Plan.paid,
                    shareType = shareType,
                )
            ),
            constraint = minimumIntervalConstraint(
                userId = downloadError.fileId.userId,
                schemaId = DownloadErroringUsersTotal.SCHEMA_ID,
                interval = 5.minutes,
            )
        )
    }

    private val DownloadErrorManager.Error.key: String get() = buildString {
        append(fileId.key)
        append(".")
        append(throwable.toDownloadErrorType().toString())
    }

    private val FileId.key: String get() = buildString {
        append(shareId.id)
        append(".")
        append(id)
    }

    private val DownloadErrorManager.Error.logTag: String get() = buildString {
        append(LogTag.DOWNLOAD)
        append(".")
        append(fileId.id.logId())
    }
}
