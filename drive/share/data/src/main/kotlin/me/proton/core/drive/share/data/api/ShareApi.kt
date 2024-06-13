/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.share.data.api

import me.proton.core.drive.share.data.api.request.CreateShareRequest
import me.proton.core.drive.share.data.api.request.ShareAccessWithNodeRequest
import me.proton.core.drive.share.data.api.response.CreateShareResponse
import me.proton.core.drive.share.data.api.response.GetShareBootstrapResponse
import me.proton.core.drive.share.data.api.response.GetSharesResponse
import me.proton.core.drive.share.data.api.response.GetUnmigratedSharesResponse
import me.proton.core.drive.share.data.api.response.UpdateUnmigratedSharesResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ShareApi : BaseRetrofitApi {

    @GET("drive/shares")
    suspend fun getShares(
        @Query("ShareType") shareType: Long,
    ): GetSharesResponse

    @GET("drive/shares/@{enc_shareID}")
    suspend fun getShareBootstrap(@Path("enc_shareID") shareId: String): GetShareBootstrapResponse

    @POST("drive/volumes/{enc_volumeID}/shares")
    suspend fun createShare(
        @Path("enc_volumeID") volumeId: String,
        @Body request: CreateShareRequest
    ): CreateShareResponse

    @DELETE("drive/shares/@{enc_shareID}")
    suspend fun deleteShare(
        @Path("enc_shareID") shareId: String,
        @Query("Force") force: Long,
    )

    @GET("drive/migrations/shareaccesswithnode/unmigrated")
    suspend fun getUnmigratedShares() : GetUnmigratedSharesResponse

    @POST("drive/migrations/shareaccesswithnode")
    suspend fun updateUnmigratedShares(@Body request: ShareAccessWithNodeRequest) : UpdateUnmigratedSharesResponse
}
