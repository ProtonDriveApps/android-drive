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
package me.proton.core.drive.base.data.extension

import android.content.Context
import android.database.SQLException
import android.system.ErrnoException
import kotlinx.coroutines.CancellationException
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.drive.base.data.entity.LoggerLevel
import me.proton.core.drive.base.domain.api.ProtonApiCode.FEATURE_DISABLED
import me.proton.core.drive.base.domain.exception.BackupStopException
import me.proton.core.drive.base.domain.exception.BackupSyncException
import me.proton.core.drive.base.domain.exception.InvalidFieldException
import me.proton.core.network.data.ProtonErrorException
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable
import retrofit2.HttpException
import java.io.FileNotFoundException
import java.io.IOException
import me.proton.core.drive.i18n.R as I18N

fun Throwable.getDefaultMessage(
    context: Context,
    useExceptionMessage: Boolean,
    unhandled: String = context.getString(I18N.string.common_error_internal),
): String = if (useExceptionMessage) {
    "${this.javaClass.simpleName}: $message"
} else {
    when (this) {
        is ApiException -> getDefaultMessage(context)
        is BackupStopException -> getDefaultMessage(context)
        is BackupSyncException -> getDefaultMessage(context)
        is CancellationException -> getDefaultMessage(context)
        is CryptoException -> getDefaultMessage(context)
        is HttpException -> getDefaultMessage(context)
        is IllegalArgumentException -> getDefaultMessage(context)
        is IllegalStateException -> getDefaultMessage(context)
        is InvalidFieldException -> getDefaultMessage(context)
        is ProtonErrorException -> getDefaultMessage(context)
        is SQLException -> getDefaultMessage(context)
        is UnsupportedOperationException -> getDefaultMessage(context)
        is IOException -> getDefaultMessage(context)
        is SecurityException -> getDefaultMessage(context)
        is RuntimeException -> {
            cause?.getDefaultMessage(context, useExceptionMessage)
                ?: getDefaultMessage(context)
        }
        else -> unhandled
    }
}

fun Throwable.log(tag: String, message: String? = null, level: LoggerLevel? = null): Throwable = this.also {
    when (this) {
        is ApiException -> log(tag, message, level)
        is BackupStopException -> log(tag, message, level)
        is BackupSyncException -> log(tag, message, level)
        is CancellationException -> log(tag, message, level)
        is CryptoException -> log(tag, message, level)
        is HttpException -> log(tag, message, level)
        is IllegalArgumentException -> log(tag, message, level)
        is IllegalStateException -> log(tag, message, level)
        is ProtonErrorException -> log(tag, message, level)
        is SQLException -> log(tag, message, level)
        is UnsupportedOperationException -> log(tag, message, level)
        is IOException -> log(tag, message, level)
        is SecurityException -> log(tag, message, level)
        is RuntimeException -> log(tag, message, level)
        else -> level.log(tag, this, message)
    }
}

fun <T : Throwable>T.logDefaultMessage(
    context: Context,
    tag: String,
    useExceptionMessage: Boolean = false,
    unhandled: String = context.getString(I18N.string.common_error_internal),
): String = getDefaultMessage(
    context = context,
    useExceptionMessage = useExceptionMessage,
    unhandled = unhandled,
).also { defaultMessage -> log(tag, defaultMessage) }

val Throwable.isRetryable: Boolean
    get() = when (this) {
        is RuntimeException -> {
            val runtimeCause = cause
            if (runtimeCause is ApiException) {
                runtimeCause.isDriveRetryable()
            } else {
                false
            }
        }

        is ApiException -> isDriveRetryable()
        else -> false
    }

private fun ApiException.isDriveRetryable() =
    if (this.error.cause is FileNotFoundException) {
        false
    } else {
        this.error.isRetryable() || when (val error = this.error) {
            is ApiResult.Error.Timeout,
            is ApiResult.Error.NoInternet,
            -> true

            is ApiResult.Error.Http -> error.httpCode == 424 && error.proton?.code == FEATURE_DISABLED

            else -> false
        }
    }

fun Throwable.isErrno(errno: Int): Boolean = if (cause is ErrnoException) {
    errno == (cause as ErrnoException).errno
} else {
    cause?.isErrno(errno) ?: false
}

fun <T> ApiResult<T>.isHttpError(range: IntRange): Boolean {
    val httpError = this as? ApiResult.Error.Http
    return httpError?.httpCode in range
}
