/*
 * Copyright (c) 2022-2023 Proton AG.
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

package me.proton.core.drive.shareurl.base.data.api

import me.proton.core.drive.shareurl.base.data.api.request.DeleteShareUrlsRequest
import me.proton.core.drive.shareurl.base.data.api.request.ShareUrlRequest
import me.proton.core.drive.shareurl.base.data.api.request.UpdateCustomPasswordShareUrlRequest
import me.proton.core.drive.shareurl.base.data.api.request.UpdateExpirationDurationShareUrlRequest
import me.proton.core.drive.shareurl.base.data.api.request.UpdatePermissionsShareUrlRequest
import me.proton.core.drive.shareurl.base.data.api.request.UpdateShareUrlRequest
import me.proton.core.drive.shareurl.base.data.api.response.GetShareUrlResponse
import me.proton.core.drive.shareurl.base.data.api.response.GetShareUrlsResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ShareUrlApi : BaseRetrofitApi {

    @GET("drive/shares/{enc_shareID}/urls")
    suspend fun getAllShareUrls(
        @Path("enc_shareID") shareId: String,
        @Query("Page") page: Int,
        @Query("PageSize") pageSize: Int,
        @Query("Recursive") recursive: Int,
    ): GetShareUrlsResponse

    @POST("drive/shares/{enc_shareID}/urls")
    suspend fun createShareUrl(
        @Path("enc_shareID") shareId: String,
        @Body request: ShareUrlRequest,
    ): GetShareUrlResponse

    @PUT("drive/shares/{enc_shareID}/urls/{enc_urlID}")
    suspend fun updateShareUrl(
        @Path("enc_shareID") shareId: String,
        @Path("enc_urlID") urlId: String,
        @Body request: UpdateShareUrlRequest,
    ): GetShareUrlResponse

    @PUT("drive/shares/{enc_shareID}/urls/{enc_urlID}")
    suspend fun updateShareUrl(
        @Path("enc_shareID") shareId: String,
        @Path("enc_urlID") urlId: String,
        @Body request: UpdateCustomPasswordShareUrlRequest,
    ): GetShareUrlResponse

    @PUT("drive/shares/{enc_shareID}/urls/{enc_urlID}")
    suspend fun updateShareUrl(
        @Path("enc_shareID") shareId: String,
        @Path("enc_urlID") urlId: String,
        @Body request: UpdateExpirationDurationShareUrlRequest,
    ): GetShareUrlResponse

    @PUT("drive/shares/{enc_shareID}/urls/{enc_urlID}")
    suspend fun updateShareUrl(
        @Path("enc_shareID") shareId: String,
        @Path("enc_urlID") urlId: String,
        @Body request: UpdatePermissionsShareUrlRequest,
    ): GetShareUrlResponse

    @DELETE("drive/shares/{enc_shareID}/urls/{enc_urlID}")
    suspend fun deleteShareUrl(
        @Path("enc_shareID") shareId: String,
        @Path("enc_urlID") urlId: String,
    )

    @POST("drive/shares/{enc_shareID}/urls")
    suspend fun deleteShareUrls(
        @Path("enc_shareID") shareId: String,
        @Body request: DeleteShareUrlsRequest,
    )
}
