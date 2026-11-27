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

import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.drive.sdk.SdkHttpClientException

// The Drive SDK maps SdkHttpClientException to an error domain with codes; any other exception reaches the app without them.
internal fun ApiResult.Error.toSdkHttpClientException(): SdkHttpClientException = when (this) {
    is ApiResult.Error.Http -> SdkHttpClientException.Http(
        statusCode = httpCode,
        protonCode = proton?.code,
        retryAfter = retryAfter,
        message = proton?.error ?: message.takeIf { it.isNotEmpty() },
    )
    is ApiResult.Error.Timeout -> SdkHttpClientException.Timeout(cause)
    is ApiResult.Error.Connection -> SdkHttpClientException.Connection(cause)
    is ApiResult.Error.Parse -> SdkHttpClientException.Parse(cause)
}

internal inline fun <T> mappingCoreErrors(block: () -> T): T = try {
    block()
} catch (exception: ApiException) {
    throw exception.error.toSdkHttpClientException().also { it.addSuppressed(exception) }
}
