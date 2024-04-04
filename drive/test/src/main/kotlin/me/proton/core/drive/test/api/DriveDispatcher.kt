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
    var handlers: List<RecordedRequest.() -> MockResponse?> = emptyList()
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
)

inline fun <reified T> RequestContext.request() : T {
    return Json.decodeFromString(recordedRequest.body.readUtf8())
}

private fun RoutingConfiguration.route(
    method: String,
    path: String,
    block: RequestContext.() -> MockResponse,
) {
    handlers = handlers + {
        if (this.method == method) {
            val segmentsSelector = path.split("/")
            val segmentsQuery = this.path?.split("/").orEmpty()
            val matches = segmentsSelector.mapIndexed { index, segment ->
                if (segment.startsWith("{") && segment.endsWith("}")) {
                    segmentsQuery.getOrNull(index) != null
                } else {
                    segmentsQuery.getOrNull(index) == segment
                }
            }.fold(true) { acc, value -> acc && value }
            if (matches) {
                val parameters = segmentsSelector.mapIndexedNotNull { index, segment ->
                    if (segment.startsWith("{") && segment.endsWith("}")) {
                        segment.substring(1, segment.lastIndex) to segmentsQuery[index]
                    } else {
                        null
                    }
                }.associate { it }
                RequestContext(this, parameters).block()
            } else {
                null
            }
        } else {
            null
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
