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
import me.proton.core.drive.base.data.entity.LoggerLevel
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.Locale

class LogInterceptor : Interceptor {
    var userId: UserId? = null
    var announceEvent: AsyncAnnounceEvent? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestOccurredAt = TimestampMs()
        val response = coRunCatching { chain.proceed(request) }
            .onFailure { error ->
                error.log(
                    tag = LogTag.LOG,
                    message = "Proceed(${request.method.uppercase(Locale.US)} ${request.url.encodedPath}) failed",
                    level = LoggerLevel.DEBUG,
                )
            }
            .getOrThrow()
        val responseOccurredAt = TimestampMs()
        coRunCatching {
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
            jsonBody = coRunCatching {
                response.peekJsonBody(byteCount = MAX_BYTE_COUNT.value)
            }.getOrNull(LogTag.LOG, "Cannot read body"),
            source = response.source,
        ),
    )

    private fun announceEvent(event: Event) = userId?.let { userId ->
        announceEvent?.invoke(
            userId = userId,
            event = event,
        )
    }

    private fun Response.peekJsonBody(byteCount: Long): String? {
        val contentType = body?.contentType() ?: return null
        if (!contentType.type.equals(JSON_CONTENT_TYPE, ignoreCase = true)
            || !contentType.subtype.equals(JSON_CONTENT_SUB_TYPE, ignoreCase = true)) {
            return null
        }
        return peekBody(byteCount).string()
    }

    private val Response.source: Event.Network.Source get() = when {
        networkResponse != null -> Event.Network.Source.Network
        cacheResponse != null -> Event.Network.Source.Cache
        else -> Event.Network.Source.Unknown
    }

    companion object {
        private const val JSON_CONTENT_TYPE = "application"
        private const val JSON_CONTENT_SUB_TYPE = "json"
        private val MAX_BYTE_COUNT = 1.MiB
    }
}
