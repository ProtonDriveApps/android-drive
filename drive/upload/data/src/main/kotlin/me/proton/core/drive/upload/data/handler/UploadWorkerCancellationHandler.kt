/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.upload.data.handler

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import me.proton.core.drive.linkupload.domain.extension.toInitiator
import me.proton.core.drive.observability.domain.metrics.UploadWorkerCancellationTotal
import me.proton.core.drive.observability.domain.metrics.common.Pipeline
import me.proton.core.drive.observability.domain.usecase.EnqueueObservabilityEvent
import me.proton.core.drive.upload.data.exception.UploadWorkerException
import me.proton.core.drive.upload.data.extension.getFileSize
import me.proton.core.drive.upload.data.extension.toReason
import me.proton.core.drive.upload.data.worker.UploadFileSdkWorker
import me.proton.core.drive.upload.domain.handler.UploadErrorHandler
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import javax.inject.Inject

class UploadWorkerCancellationHandler @Inject constructor(
    private val enqueueObservabilityEvent: EnqueueObservabilityEvent,
) : UploadErrorHandler {

    override suspend fun onError(uploadError: UploadErrorManager.Error) {
        val error = uploadError.throwable
        if (error !is UploadWorkerException || error.name != WORKER_NAME) {
            return
        }
        if (VERSION.SDK_INT < VERSION_CODES.S) {
            return
        }
        val stopReason = error.stopReason ?: return
        val uploadFileLink = uploadError.uploadFileLink
        enqueueObservabilityEvent(
            UploadWorkerCancellationTotal(
                Labels = UploadWorkerCancellationTotal.LabelsData(
                    pipeline = Pipeline.default,
                    initiator = uploadFileLink.toInitiator(),
                    reason = stopReason.toReason(),
                    fileSize = uploadFileLink.getFileSize(),
                )
            )
        )
    }

    private companion object {
        val WORKER_NAME = UploadFileSdkWorker::class.java.simpleName
    }
}
