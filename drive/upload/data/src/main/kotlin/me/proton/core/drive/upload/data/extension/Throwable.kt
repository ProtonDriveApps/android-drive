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

import android.system.OsConstants
import me.proton.android.drive.verifier.data.extension.log
import me.proton.android.drive.verifier.domain.exception.VerifierException
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.data.extension.isErrno
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.observability.domain.metrics.UploadErrorsTotal
import me.proton.core.drive.upload.domain.exception.InconsistencyException
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.hasProtonErrorCode
import me.proton.core.network.domain.isHttpError
import me.proton.core.drive.base.data.extension.log as baseLog

internal val Throwable.isRetryable: Boolean
    get() = when (this) {
        is VerifierException -> this.cause.isRetryable
        is InconsistencyException -> true
        else -> isRetryable
    }

internal fun Throwable.log(tag: String, message: String? = null): Throwable = this.also {
    when (this) {
        is VerifierException -> this.log(tag, message.orEmpty())
        is InconsistencyException -> this.log(tag, message.orEmpty())
        else -> this.baseLog(tag, message)
    }
}

internal fun Throwable.toEventUploadReason(): Event.Upload.Reason = when (this) {
    is SecurityException -> Event.Upload.Reason.ERROR_PERMISSIONS
    is ApiException -> when {
        hasProtonErrorCode(ProtonApiCode.EXCEEDED_QUOTA) -> Event.Upload.Reason.ERROR_DRIVE_STORAGE
        else -> Event.Upload.Reason.ERROR_OTHER
    }

    else -> if (isErrno(OsConstants.ENOSPC)) {
        Event.Upload.Reason.ERROR_LOCAL_STORAGE
    } else {
        Event.Upload.Reason.ERROR_OTHER
    }
}

fun Throwable.toUploadErrorType(): UploadErrorsTotal.Type = when(this) {
    is ApiException -> when {
        hasProtonErrorCode(ProtonApiCode.EXCEEDED_QUOTA) -> UploadErrorsTotal.Type.free_space_exceeded
        hasProtonErrorCode(ProtonApiCode.TOO_MANY_CHILDREN) -> UploadErrorsTotal.Type.too_many_children
        isHttpError(429) -> UploadErrorsTotal.Type.rate_limited
        isHttpError(400..499) -> UploadErrorsTotal.Type.http_4xx
        isHttpError(500..599) -> UploadErrorsTotal.Type.http_5xx
        else -> UploadErrorsTotal.Type.unknown
    }
    is VerifierException -> UploadErrorsTotal.Type.integrity_error
    else -> UploadErrorsTotal.Type.unknown
}

internal fun ApiException.isHttpError(range: IntRange): Boolean =
    error.isHttpError(range)

internal fun <T> ApiResult<T>.isHttpError(range: IntRange): Boolean {
    val httpError = this as? ApiResult.Error.Http
    return httpError?.httpCode in range
}
