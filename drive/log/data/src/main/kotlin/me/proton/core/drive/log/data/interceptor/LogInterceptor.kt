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

package me.proton.core.drive.log.data.interceptor

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AsyncAnnounceEvent
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.base.domain.log.LogTag
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.util.Locale

class LogInterceptor : Interceptor {
    var userId: UserId? = null
    var announceEvent: AsyncAnnounceEvent? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestOccurredAt = TimestampMs()
        val response = runCatching { chain.proceed(request) }
            .onFailure { error ->
                error.log(
                    tag = LogTag.LOG,
                    message = "Proceed(${request.method.uppercase(Locale.US)} ${request.url.encodedPath}) failed",
                )
            }
            .getOrThrow()
        val responseOccurredAt = TimestampMs()
        runCatching {
            announceEvent(
                networkEvent(
                    request = request,
                    requestOccurredAt = requestOccurredAt,
                    response = response,
                    responseOccurredAt = responseOccurredAt,
                )
            )
        }
            .onFailure { error ->
                error.log(LogTag.LOG, "Announcing network event failed")
            }

        return response
    }

    private fun networkEvent(
        request: Request,
        requestOccurredAt: TimestampMs,
        response: Response,
        responseOccurredAt: TimestampMs,
    ) = Event.Network(
        request = Event.Network.Request(
            occurredAt = requestOccurredAt,
            method = request.method,
            urlPath = request.url.encodedPath,
        ),
        response = Event.Network.Response(
            occurredAt = responseOccurredAt,
            code = response.code,
            message = response.message,
            jsonBody = response.peekBody(byteCount = MAX_BYTE_COUNT.value).jsonString,
        ),
    )

    private fun announceEvent(event: Event) = userId?.let { userId ->
        announceEvent?.invoke(
            userId = userId,
            event = event,
        )
    }

    private val ResponseBody.jsonString: String? get() = takeIf {
        JSON_CONTENT_TYPE.equals(
            other = contentType()?.toString(),
            ignoreCase = true,
        )
    }?.let { runCatching { string() }.getOrNull() }

    companion object {
        private val MAX_BYTE_COUNT = 1.MiB
        private const val JSON_CONTENT_TYPE = "application/json"
    }
}
