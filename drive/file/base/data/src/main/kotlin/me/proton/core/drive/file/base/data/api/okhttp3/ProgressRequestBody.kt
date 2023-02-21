/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.file.base.data.api.okhttp3

import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.internal.http.CallServerInterceptor
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer

class ProgressRequestBody(
    private val requestBody: RequestBody,
    val progress: MutableStateFlow<Long>,
) : RequestBody() {

    override fun contentType(): MediaType? = requestBody.contentType()

    override fun writeTo(sink: BufferedSink) {
        if (isCalledByCallServerInterceptor()) writeToWithProgress(sink)
        else requestBody.writeTo(sink)
    }

    override fun isOneShot(): Boolean = true

    private fun writeToWithProgress(sink: BufferedSink) {
        var totalBytes = 0L
        val bufferedSink = object : ForwardingSink(sink) {
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                totalBytes += byteCount
                progress.value = totalBytes
            }
        }.buffer()
        requestBody.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    /**
     * Provides information if current function call into [ProgressRequestBody] is done by [CallServerInterceptor].
     * [writeTo] can be called multiple times, it depends how many interceptors are in the chain. Usually we can have
     * [HttpLoggingInterceptor], [OkHttpProfilerInterceptor] or some other. We want to update progress only when
     * [writeTo] is called from [CallServerInterceptor].
     */
    private fun isCalledByCallServerInterceptor() = Thread.currentThread().stackTrace.any { stackTraceElement ->
        stackTraceElement.className == CallServerInterceptor::class.java.canonicalName
    }
}
