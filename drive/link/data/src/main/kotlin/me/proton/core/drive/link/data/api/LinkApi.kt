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
package me.proton.core.drive.link.data.api

import me.proton.core.drive.base.data.api.response.CodeResponse
import me.proton.core.drive.link.data.api.request.CheckAvailableHashesRequest
import me.proton.core.drive.link.data.api.request.GetLinksRequest
import me.proton.core.drive.link.data.api.request.MoveLinkRequest
import me.proton.core.drive.link.data.api.request.RenameLinkRequest
import me.proton.core.drive.link.data.api.response.CheckAvailableHashesResponse
import me.proton.core.drive.link.data.api.response.GetLinkResponse
import me.proton.core.drive.link.data.api.response.GetLinksResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface LinkApi : BaseRetrofitApi {
    @GET("drive/shares/{enc_shareID}/links/{enc_linkID}")
    suspend fun getLink(
        @Path("enc_shareID") shareId: String,
        @Path("enc_linkID") linkId: String
    ): GetLinkResponse

    @POST("drive/shares/{enc_shareID}/links/{enc_linkID}/checkAvailableHashes")
    suspend fun checkAvailableHashes(
        @Path("enc_shareID") shareId: String,
        @Path("enc_linkID") linkId: String,
        @Body request: CheckAvailableHashesRequest,
    ): CheckAvailableHashesResponse

    @PUT("drive/shares/{enc_shareID}/links/{enc_linkID}/move")
    suspend fun moveLink(
        @Path("enc_shareID") shareId: String,
        @Path("enc_linkID") folderLinkId: String,
        @Body request: MoveLinkRequest
    ): CodeResponse

    @PUT("drive/shares/{enc_shareID}/links/{enc_linkID}/rename")
    suspend fun renameLink(
        @Path("enc_shareID") shareId: String,
        @Path("enc_linkID") linkId: String,
        @Body request: RenameLinkRequest,
    ): CodeResponse

    @POST("drive/shares/{enc_shareID}/links/fetch_metadata")
    suspend fun getLinks(
        @Path("enc_shareID") shareId: String,
        @Body request: GetLinksRequest,
    ): GetLinksResponse
}
