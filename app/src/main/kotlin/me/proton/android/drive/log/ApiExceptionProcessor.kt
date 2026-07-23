/*
 * Copyright (c) 2026 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.log

import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.SentryEvent
import me.proton.core.drive.base.domain.extension.findThrowable
import me.proton.core.network.data.ProtonErrorException
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import retrofit2.HttpException

class ApiExceptionProcessor : EventProcessor {

    override fun process(event: SentryEvent, hint: Hint): SentryEvent {
        event.throwable
            .findThrowable<ApiException>()
            ?.enrichEvent(event)
        return event
    }

    private fun ApiException.enrichEvent(
        event: SentryEvent,
    ) {
        (error as? ApiResult.Error.Http)?.let { error ->
            val errorCause = error.cause
            if (errorCause is ProtonErrorException) {
                errorCause.response.let { response ->
                    response.header(HEADER_PM_CODE)
                        ?.let { code -> event.setTagCode(code) }
                    response.header(HEADER_PM_ROUTE_PATTERN)
                        ?.let { route -> event.setTagRoutePattern(route) }
                }
            }
            if (errorCause is HttpException) {
                errorCause.response()?.let { response ->
                    response.headers()[HEADER_PM_CODE]
                        ?.let { code -> event.setTagCode(code) }
                    response.headers()[HEADER_PM_ROUTE_PATTERN]
                        ?.let { route -> event.setTagRoutePattern(route) }
                }
            }
            event.setTag(TAG_HTTP_STATUS_CODE, error.httpCode.toString())
        }
    }

    private fun SentryEvent.setTagCode(code: String) {
        setTag(TAG_PM_CODE, code)
    }

    private fun SentryEvent.setTagRoutePattern(route: String) {
        setTag(TAG_PM_ROUTE_PATTERN, route)
    }

    private companion object {
        const val HEADER_PM_CODE = "X-Pm-Code"
        const val HEADER_PM_ROUTE_PATTERN = "X-PM-Route-Pattern"
        const val TAG_PM_CODE = "pm_code"
        const val TAG_PM_ROUTE_PATTERN = "pm_route_pattern"
        const val TAG_HTTP_STATUS_CODE = "http_status_code"
    }
}
