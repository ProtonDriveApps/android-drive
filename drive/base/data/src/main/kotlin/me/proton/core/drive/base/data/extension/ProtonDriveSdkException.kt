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
import me.proton.core.drive.base.data.entity.LoggerLevel
import me.proton.drive.sdk.ProtonDriveSdkException
import me.proton.drive.sdk.ProtonSdkError
import me.proton.core.drive.i18n.R as I18N

fun ProtonDriveSdkException.getDefaultMessage(
    context: Context,
    useExceptionMessage: Boolean,
): String {
    val cause = error
    return when (cause?.domain) {
        ProtonSdkError.ErrorDomain.Serialization -> context.getString(I18N.string.common_error_internal)
        ProtonSdkError.ErrorDomain.Network,
        ProtonSdkError.ErrorDomain.Transport -> context.getString(I18N.string.common_error_no_internet)

        ProtonSdkError.ErrorDomain.Api -> when (cause.secondaryCode?.toInt()) {
            in 500..599 -> context.getString(I18N.string.common_error_http_5xx)
            else -> cause.message
        }

        else -> (cause?.message ?: message)?.takeIf { useExceptionMessage }?.let { msg ->
            context.getString(I18N.string.common_error_sdk_with_message, msg)
        } ?: context.getString(I18N.string.common_error_sdk)
    }
}

fun ProtonDriveSdkException.log(
    tag: String,
    message: String? = null,
    level: LoggerLevel? = LoggerLevel.ERROR,
): ProtonDriveSdkException = also {
    loggerLevel(level).log(tag, this, message)
}

internal fun ProtonDriveSdkException.loggerLevel(default: LoggerLevel? = null): LoggerLevel? =
    when(error?.domain) {
        ProtonSdkError.ErrorDomain.Serialization -> LoggerLevel.ERROR
        ProtonSdkError.ErrorDomain.Api -> error?.secondaryCode?.toInt()?.httpCodeLoggerLevel()
        ProtonSdkError.ErrorDomain.Network,
        ProtonSdkError.ErrorDomain.Transport,
        ProtonSdkError.ErrorDomain.SuccessfulCancellation, -> LoggerLevel.DEBUG
        else -> default
    }
