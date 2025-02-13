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
package me.proton.core.drive.volume.data.api

import me.proton.core.drive.base.data.api.response.CodeResponse
import me.proton.core.drive.volume.data.api.request.CreatePhotoVolumeRequest
import me.proton.core.drive.volume.data.api.request.CreateVolumeRequest
import me.proton.core.drive.volume.data.api.response.GetShareTrashesResponse
import me.proton.core.drive.volume.data.api.response.GetShareUrlsResponse
import me.proton.core.drive.volume.data.api.response.GetVolumeResponse
import me.proton.core.drive.volume.data.api.response.GetVolumesResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VolumeApi : BaseRetrofitApi {
    @GET("drive/volumes")
    suspend fun getVolumes(): GetVolumesResponse

    @GET("drive/volumes/{enc_volumeId}")
    suspend fun getVolume(@Path("enc_volumeId") volumeId: String): GetVolumeResponse

    @POST("drive/volumes")
    suspend fun createVolume(@Body request: CreateVolumeRequest): GetVolumeResponse

    @GET("drive/volumes/{enc_volumeId}/trash")
    suspend fun getShareTrashes(
        @Path("enc_volumeId") volumeId: String,
        @Query("Page") page: Int,
        @Query("PageSize") pageSize: Int,
    ): GetShareTrashesResponse

    @GET("drive/volumes/{enc_volumeId}/urls")
    suspend fun getShareUrls(
        @Path("enc_volumeId") volumeId: String,
        @Query("Page") page: Int,
        @Query("PageSize") pageSize: Int,
    ): GetShareUrlsResponse

    @DELETE("drive/volumes/{volumeId}/trash")
    suspend fun emptyTrash(
        @Path("volumeId") volumeId: String
    ): CodeResponse

    @POST("drive/photos/volumes")
    suspend fun createPhotoVolume(@Body request: CreatePhotoVolumeRequest): GetVolumeResponse
}
