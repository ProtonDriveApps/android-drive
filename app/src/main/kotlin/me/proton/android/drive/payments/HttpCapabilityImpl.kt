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

package me.proton.android.drive.payments

import kotlinx.serialization.json.JsonObject
import me.proton.android.payment.capability.HttpCapability
import me.proton.android.payment.capability.model.HttpResponse
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpCapabilityImpl @Inject constructor(
    private val apiProvider: ApiProvider,
) : HttpCapability {

    @Volatile
    private var userId: UserId? = null

    fun setUserId(userId: UserId) {
        this.userId = userId
    }

    fun resetUserId(userId: UserId) {
        if (this.userId == userId) {
            this.userId = null
        }
    }

    override suspend fun get(endpoint: String): HttpResponse {
        return apiProvider.get<PaymentsUfcApi>(userId).invoke {
            get(endpoint)
        }.toHttpResponse()
    }

    override suspend fun post(endpoint: String, body: ByteArray?): HttpResponse {
        val requestBody = body?.toRequestBody(JSON_MEDIA_TYPE)

        return apiProvider.get<PaymentsUfcApi>(userId).invoke {
            post(endpoint, requestBody)
        }.toHttpResponse()
    }

    private fun ApiResult<Response<JsonObject>>.toHttpResponse(): HttpResponse {
        return when (this) {
            is ApiResult.Success -> HttpResponse(status = value.code(), body = value.body())
            is ApiResult.Error.Http -> HttpResponse(status = httpCode, body = body)
            is ApiResult.Error -> throw ApiException(this)
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
