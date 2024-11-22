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
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.drive.i18n.R as I18N

fun ApiException.getDefaultMessage(context: Context): String = when (val cause = error) {
    is ApiResult.Error.Certificate,
    is ApiResult.Error.Parse -> context.getString(I18N.string.common_error_internal)
    is ApiResult.Error.Connection -> context.getString(I18N.string.common_error_no_internet)
    is ApiResult.Error.Http -> when (cause.httpCode) {
        in 500..599 -> context.getString(I18N.string.common_error_http_5xx)
        else -> cause.proton?.error ?: cause.message
    }
}

fun ApiException.log(
    tag: String,
    message: String? = null,
    level: LoggerLevel? = null
): ApiException = also {
    val loggerLevel = level ?: when (val cause = error) {
        is ApiResult.Error.Certificate -> LoggerLevel.ERROR
        is ApiResult.Error.Http -> when (cause.httpCode) {
            502, 503 -> LoggerLevel.ERROR
            else -> LoggerLevel.DEBUG
        }
        is ApiResult.Error.Parse -> LoggerLevel.ERROR
        else -> LoggerLevel.DEBUG
    }
    loggerLevel.log(tag, this, message)
}
