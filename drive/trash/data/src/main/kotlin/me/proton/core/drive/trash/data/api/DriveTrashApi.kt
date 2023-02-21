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

package me.proton.core.drive.trash.data.api

import me.proton.core.drive.link.data.api.response.LinkResponses
import me.proton.core.drive.trash.data.api.request.LinkIDsRequest
import me.proton.core.drive.trash.data.api.response.GetTrashChildrenResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface DriveTrashApi : BaseRetrofitApi {

    @GET("drive/shares/{enc_shareID}/trash")
    suspend fun getTrash(
        @Path("enc_shareID") shareId: String,
        @Query("Page") page: Int,
        @Query("PageSize") pageSize: Int,
    ): GetTrashChildrenResponse

    @POST("drive/shares/{enc_shareID}/folders/{enc_linkID}/trash_multiple")
    suspend fun sendToTrash(
        @Path("enc_shareID") shareId: String,
        @Path("enc_linkID") folderId: String,
        @Body request: LinkIDsRequest,
    ): LinkResponses

    @PUT("drive/shares/{enc_shareID}/trash/restore_multiple")
    suspend fun restoreFromTrash(
        @Path("enc_shareID") shareId: String,
        @Body request: LinkIDsRequest,
    ): LinkResponses

    @DELETE("drive/shares/{enc_shareID}/trash")
    suspend fun emptyTrash(@Path("enc_shareID") shareId: String)

    @POST("drive/shares/{enc_shareID}/trash/delete_multiple")
    suspend fun deleteItemsFromTrash(
        @Path("enc_shareID") shareId: String,
        @Body request: LinkIDsRequest,
    ): LinkResponses
}
