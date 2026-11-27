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

package me.proton.android.drive.sdk

import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.data.ProtonErrorException
import me.proton.core.network.domain.ApiResult
import me.proton.drive.sdk.SdkHttpClient
import me.proton.drive.sdk.SdkHttpRequest
import me.proton.drive.sdk.SdkHttpRequestType
import me.proton.drive.sdk.SdkHttpResponse
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.source
import retrofit2.Response
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sends every Drive SDK request through the legacy Core's `ApiProvider`. The Drive SDK retries 429 and 5xx
 * itself, so nothing is retried here.
 */
class CoreSdkHttpClient(
    private val call: suspend (suspend HttpSdkApi.() -> Response<ResponseBody>) -> ApiResult<Response<ResponseBody>>
) : SdkHttpClient {

    override suspend fun execute(request: SdkHttpRequest): SdkHttpResponse {
        // Core can run the call again (token refresh, human verification, failover), so the body is read once, here.
        val requestBody = request.toRequestBody()
        return call { send(request, requestBody) }.toSdkResponse()
    }

    companion object {
        operator fun invoke(userId: UserId, apiProvider: ApiProvider) = CoreSdkHttpClient { block ->
            apiProvider.get<HttpSdkApi>(userId).invoke(forceNoRetryOnConnectionErrors = true, block = block)
        }
    }
}

// Only a block upload is streamed. Core's interceptors may read any other body before it is sent, so those are
// buffered, as the old bridge did.
private fun SdkHttpRequest.toRequestBody(): RequestBody? = when {
    body != null && type == SdkHttpRequestType.STORAGE_UPLOAD -> InputStreamRequestBody(body!!, contentLength)
    body != null -> body!!.readBytes().toRequestBody()
    // POST, PUT and DELETE are declared with a body, and Retrofit rejects null for it.
    !method.equals("GET", ignoreCase = true) -> ByteArray(0).toRequestBody()
    else -> null
}

private suspend fun HttpSdkApi.send(request: SdkHttpRequest, requestBody: RequestBody?): Response<ResponseBody> =
    with(request) {
        when (method.uppercase()) {
            "GET" -> if (type == SdkHttpRequestType.STORAGE_DOWNLOAD) getStreaming(url, headers) else get(url, headers)
            "POST" -> post(url, headers, requestBody)
            "PUT" -> put(url, headers, requestBody)
            "DELETE" -> delete(url, headers, requestBody)
            else -> throw IllegalArgumentException("Unsupported method: $method")
        }
    }

// Streams the body in chunks; a negative length makes OkHttp send it chunked.
private class InputStreamRequestBody(
    private val stream: InputStream,
    private val length: Long
) : RequestBody() {
    override fun contentType(): MediaType? = null
    override fun contentLength(): Long = length
    override fun isOneShot(): Boolean = true
    private val sent = AtomicBoolean(false)

    override fun writeTo(sink: BufferedSink) {
        if (sent.getAndSet(true)) throw IOException("The upload stream was already sent and cannot be sent again")
        sink.writeAll(stream.source())
    }
}

// A response with a Proton error body is still a response: the SDK reads its status and body.
private fun ApiResult<Response<ResponseBody>>.toSdkResponse(): SdkHttpResponse = when (this) {
    is ApiResult.Success -> SdkHttpResponse(
        statusCode = value.code(),
        headers = value.headers().toMultimapKeepingCase(),
        body = (value.body() ?: value.errorBody())?.byteStream()
    )
    is ApiResult.Error.Http -> {
        val error = cause
        if (error is ProtonErrorException) {
            SdkHttpResponse(
                statusCode = error.response.code,
                headers = error.response.headers.toMultimapKeepingCase(),
                body = error.response.body?.byteStream()
            )
        } else {
            throw toSdkHttpClientException().also { exception -> error?.let(exception::addSuppressed) }
        }
    }
    is ApiResult.Error.Timeout, is ApiResult.Error.Connection, is ApiResult.Error.Parse -> throw toSdkHttpClientException()
}

// toMultimap() lowercases the names; the old bridge handed them over as sent.
private fun Headers.toMultimapKeepingCase(): Map<String, List<String>> = names().associateWith { values(it) }
