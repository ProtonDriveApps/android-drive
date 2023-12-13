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
package me.proton.core.drive.file.base.data.api

import me.proton.core.drive.base.data.api.response.CodeResponse
import me.proton.core.drive.file.base.data.api.request.CreateFileRequest
import me.proton.core.drive.file.base.data.api.request.GetThumbnailsUrlsRequest
import me.proton.core.drive.file.base.data.api.request.UpdateRevisionRequest
import me.proton.core.drive.file.base.data.api.response.CreateFileResponse
import me.proton.core.drive.file.base.data.api.response.GetRevisionResponse
import me.proton.core.drive.file.base.data.api.response.GetThumbnailResponse
import me.proton.core.drive.file.base.data.api.response.GetThumbnailsUrlsResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface FileApi : BaseRetrofitApi {
    @POST("drive/shares/@{enc_shareID}/files")
    suspend fun createFile(
        @Path("enc_shareID") shareId: String,
        @Body request: CreateFileRequest
    ): CreateFileResponse

    @GET("drive/shares/@{enc_shareID}/files/@{enc_linkID}/revisions/@{enc_revisionID}")
    suspend fun getRevision(
        @Path("enc_shareID") shareId: String,
        @Path("enc_linkID") linkId: String,
        @Path("enc_revisionID") revisionId: String,
        @Query("FromBlockIndex") fromBlockIndex: Int,
        @Query("PageSize") pageSize: Int,
    ): GetRevisionResponse

    @PUT("drive/shares/@{enc_shareID}/files/@{enc_linkID}/revisions/@{enc_revisionID}")
    suspend fun updateRevision(
        @Path("enc_shareID") shareId: String,
        @Path("enc_linkID") linkId: String,
        @Path("enc_revisionID") revisionId: String,
        @Body request: UpdateRevisionRequest
    ): CodeResponse

    @DELETE("drive/shares/{enc_shareID}/files/{enc_linkID}/revisions/{enc_revisionID}")
    suspend fun deleteRevision(
        @Path("enc_shareID") shareId: String,
        @Path("enc_linkID") linkId: String,
        @Path("enc_revisionID") revisionId: String,
    ): CodeResponse

    @GET("drive/shares/{enc_shareID}/files/{enc_linkID}/revisions/{enc_revisionID}/thumbnail")
    suspend fun getThumbnailUrl(
        @Path("enc_shareID") shareId: String,
        @Path("enc_linkID") linkId: String,
        @Path("enc_revisionID") revisionId: String,
    ): GetThumbnailResponse

    @POST("drive/volumes/{enc_volumeID}/thumbnails")
    suspend fun getThumbnailsUrls(
        @Path("enc_volumeID") volumeId: String,
        @Body request: GetThumbnailsUrlsRequest,
    ): GetThumbnailsUrlsResponse

    @GET
    @Streaming
    suspend fun getFileStream(@Url url: String): Response<ResponseBody>

    @Multipart
    @POST
    suspend fun uploadFile(
        @Url url: String,
        @Part filePart: MultipartBody.Part,
        //@Tag timeoutOverride: TimeoutOverride, // TODO: when timeoutOverride is bigger then ApiClient.timeoutSeconds this does not work
                                                 //      as Core sets timeoutSeconds to Coroutine that executes this API call
    ): Response<ResponseBody>
}
