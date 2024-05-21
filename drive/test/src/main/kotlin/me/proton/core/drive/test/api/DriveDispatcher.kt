/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.test.api

import kotlinx.serialization.json.Json
import me.proton.core.util.kotlin.serialize
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.net.HttpURLConnection

class DriveDispatcher : Dispatcher() {
    var handlers: List<RecordedRequest.() -> MockResponse?> = emptyList()

    override fun dispatch(request: RecordedRequest): MockResponse {
        return handlers.firstNotNullOfOrNull { handler -> handler(request) }
            ?: MockResponse().apply {
                setStatus("HTTP/1.1 501 ${request.method}: ${request.path} not implemented")
            }
    }
}

@DslMarker
annotation class ApiDriveDsl

@ApiDriveDsl
class RoutingConfiguration {
    var handlers: List<(RecordedRequest) -> MockResponse?> = emptyList()
}

fun routing(block: RoutingConfiguration.() -> Unit): RoutingConfiguration =
    RoutingConfiguration().apply {
        block()
    }

fun MockWebServer.routing(
    vararg configurations: RoutingConfiguration,
    block: RoutingConfiguration.() -> Unit,
) {
    val routingConfiguration = RoutingConfiguration().apply {
        block()
    }
    val internalDispatcher = dispatcher
    if (internalDispatcher is DriveDispatcher) {
        internalDispatcher.handlers += (configurations.asList() + routingConfiguration).flatMap { it.handlers }
    }
}

fun MockWebServer.clear() {
    val internalDispatcher = dispatcher
    if (internalDispatcher is DriveDispatcher) {
        internalDispatcher.handlers = emptyList()
    }
}

fun RoutingConfiguration.get(path: String, block: RequestContext.() -> MockResponse) =
    route("GET", path, block)

fun RoutingConfiguration.post(path: String, block: RequestContext.() -> MockResponse) =
    route("POST", path, block)

fun RoutingConfiguration.put(path: String, block: RequestContext.() -> MockResponse) =
    route("PUT", path, block)

fun RoutingConfiguration.delete(path: String, block: RequestContext.() -> MockResponse) =
    route("DELETE", path, block)

@ApiDriveDsl
data class RequestContext(
    val recordedRequest: RecordedRequest,
    val parameters: Map<String, String>,
    val times: Int,
)

inline fun <reified T> RequestContext.request(): T {
    return Json.decodeFromString(recordedRequest.body.readUtf8())
}

private val parameterRegex = """@?\{(.*)\}""".toRegex()

private fun RoutingConfiguration.route(
    method: String,
    path: String,
    block: RequestContext.() -> MockResponse,
) {
    handlers = handlers + Handler(method, path, block)
}

data class Handler(
    private val method: String,
    private val path: String,
    private val block: RequestContext.() -> MockResponse,
) : (RecordedRequest) -> MockResponse? {

    private var times = 0
    override operator fun invoke(
        request: RecordedRequest,
    ): MockResponse? {
        if (request.method != method) {
            return null
        }

        val segmentsSelector = path.split("/")
        val segmentsQuery = request.path?.substringBefore("?")?.split("/").orEmpty()

        if (segmentsSelector.count() != segmentsQuery.count()) {
            return null
        }

        val segmentsMatches = segmentsSelector.mapIndexed { index, segment ->
            if (segment.matches(parameterRegex)) {
                segmentsQuery.getOrNull(index) != null
            } else {
                segmentsQuery.getOrNull(index) == segment
            }
        }.fold(true) { acc, value -> acc && value }

        if (!segmentsMatches) {
            return null
        }

        val parameters = segmentsSelector.mapIndexedNotNull { index, segment ->
            parameterRegex.matchEntire(segment)?.let { matchResult ->
                matchResult.groupValues[1] to segmentsQuery[index].removePrefix("@")
            }
        }.associate { it }
        return RequestContext(request, parameters, ++times).block().apply {
            addHeader("date", System.currentTimeMillis())
        }
    }
}

fun RequestContext.response(
    status: Int = 200,
    headers: Map<String, Any> = emptyMap(),
    body: RequestContext.() -> String = { "" },
) = MockResponse().apply {
    setResponseCode(status)
    headers.forEach { (name, value) ->
        setHeader(name, value)
    }
    setBody(body())
}

inline fun <reified T : Any> RequestContext.jsonResponse(
    status: Int = 200,
    headers: Map<String, Any> = emptyMap(),
    crossinline body: RequestContext.() -> T,
) = response(status, headers) { body().serialize() }

fun RequestContext.retryableErrorResponse() = response(HttpURLConnection.HTTP_CLIENT_TIMEOUT)

fun RequestContext.errorResponse() = response(HttpURLConnection.HTTP_BAD_REQUEST)
