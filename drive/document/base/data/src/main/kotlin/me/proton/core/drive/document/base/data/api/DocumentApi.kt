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

package me.proton.core.drive.document.base.data.api

import me.proton.core.drive.document.base.data.api.request.CreateDocumentRequest
import me.proton.core.drive.document.base.data.api.response.CreateDocumentResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface DocumentApi : BaseRetrofitApi {

    @POST("drive/shares/{shareID}/documents")
    suspend fun createDocument(
        @Path("shareID") shareId: String,
        @Body request: CreateDocumentRequest
    ): CreateDocumentResponse
}
