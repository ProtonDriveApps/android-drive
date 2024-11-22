/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.upload.data.extension

import androidx.work.ListenableWorker.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.drive.base.data.entity.LoggerLevel.WARNING
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.upload.data.exception.UploadCleanupException

fun UploadFileLink.logTag() = with(LogTag.UploadTag) { id.logTag() }

@ExperimentalCoroutinesApi
fun UploadFileLink.retryOrAbort(
    retryable: Boolean,
    canRetry: Boolean,
    error: Throwable,
    message: String,
): Result {
    return if (retryable && canRetry) {
        error.log(logTag(), "$message, will retry", WARNING)
        Result.retry()
    } else {
        error.log(logTag(), "$message retryable $retryable, max retries reached ${!canRetry}")
        throw UploadCleanupException(error, name)
    }
}
