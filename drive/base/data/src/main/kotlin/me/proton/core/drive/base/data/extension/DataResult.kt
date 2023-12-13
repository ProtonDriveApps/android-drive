/*
 * Copyright (c) 2022-2023 Proton AG.
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
import kotlinx.coroutines.CancellationException
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.i18n.R
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.isRetryable
import retrofit2.HttpException

val DataResult.Error.isRetryable: Boolean
    get() = when (this) {
        is DataResult.Error.Local -> false
        is DataResult.Error.Remote -> {
            when (val exception = cause) {
                is ApiException -> {
                    when (exception.error) {
                        is ApiResult.Error.Timeout,
                        is ApiResult.Error.NoInternet -> true
                        else -> exception.isRetryable()
                    }
                }
                is HttpException -> exception.code() in 500..599
                is CancellationException -> true
                else -> false
            }
        }
    }

val Map<*, DataResult.Error>.areRetryable: Boolean
    get() = all { (_, error) -> error.isRetryable }

fun DataResult.Error.getDefaultMessage(
    context: Context,
    useExceptionMessage: Boolean,
    unhandled: String = context.getString(R.string.common_error_internal),
): String =
    this.cause?.getDefaultMessage(context, useExceptionMessage) ?: unhandled

fun DataResult.Error.log(tag: String, message: String? = null): DataResult.Error = also {
    message?.let { this.cause?.log(tag, message) } ?: this.cause?.log(tag)
}

fun DataResult.Error.logDefaultMessage(
    context: Context,
    useExceptionMessage: Boolean,
    tag: String,
    unhandled: String = context.getString(R.string.common_error_internal)
): String = getDefaultMessage(
    context = context,
    useExceptionMessage = useExceptionMessage,
    unhandled = unhandled,
).also { defaultMessage -> log(tag, defaultMessage) }
