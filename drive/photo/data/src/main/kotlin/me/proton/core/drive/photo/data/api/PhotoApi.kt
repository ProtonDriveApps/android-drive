/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.photo.data.api

import me.proton.core.drive.base.data.api.response.CodeResponse
import me.proton.core.drive.photo.data.api.request.AddToAlbumRequest
import me.proton.core.drive.photo.data.api.request.CreateAlbumRequest
import me.proton.core.drive.photo.data.api.request.CreatePhotoRequest
import me.proton.core.drive.photo.data.api.request.FavoriteRequest
import me.proton.core.drive.photo.data.api.request.FindDuplicatesRequest
import me.proton.core.drive.photo.data.api.request.RemoveFromAlbumRequest
import me.proton.core.drive.photo.data.api.request.TagRequest
import me.proton.core.drive.photo.data.api.request.UpdateAlbumRequest
import me.proton.core.drive.photo.data.api.response.AddToRemoveFromAlbumResponse
import me.proton.core.drive.photo.data.api.response.CreateAlbumResponse
import me.proton.core.drive.photo.data.api.response.CreatePhotoResponse
import me.proton.core.drive.photo.data.api.response.FindDuplicatesResponse
import me.proton.core.drive.photo.data.api.response.GetAlbumListingsResponse
import me.proton.core.drive.photo.data.api.response.GetAlbumPhotoListingResponse
import me.proton.core.drive.photo.data.api.response.GetPhotoListingResponse
import me.proton.core.drive.photo.data.api.response.MigrationStatusResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PhotoApi : BaseRetrofitApi {
    @POST("drive/volumes/{enc_volumeID}/photos/share")
    suspend fun createPhotoShareWithRootLink(
        @Path("enc_volumeID") volumeId: String,
        @Body request: CreatePhotoRequest
    ): CreatePhotoResponse

    @GET("drive/volumes/{enc_volumeID}/photos")
    suspend fun getPhotoListings(
        @Path("enc_volumeID") volumeId: String,
        @Query("Desc") descending: Int,
        @Query("PageSize") pageSize: Int,
        @Query("PreviousPageLastLinkID") previousPageLastLinkId: String?,
        @Query("MinimumCaptureTime") minimumCaptureTime: Long,
        @Query("Tag") tag: Long? = null,
    ): GetPhotoListingResponse

    @POST("drive/volumes/{enc_volumeID}/photos/duplicates")
    suspend fun findDuplicates(
        @Path("enc_volumeID") volumeId: String,
        @Body request: FindDuplicatesRequest
    ): FindDuplicatesResponse

    @POST("drive/photos/volumes/{volumeID}/albums")
    suspend fun createAlbum(
        @Path("volumeID") volumeId: String,
        @Body request: CreateAlbumRequest,
    ): CreateAlbumResponse

    @PUT("drive/photos/volumes/{volumeID}/albums/{linkID}")
    suspend fun updateAlbum(
        @Path("volumeID") volumeId: String,
        @Path("linkID") linkId: String,
        @Body request: UpdateAlbumRequest,
    ): CodeResponse

    @GET("drive/photos/volumes/{volumeID}/albums")
    suspend fun getAlbumListings(
        @Path("volumeID") volumeId: String,
        @Query("AnchorID") anchorId: String? = null,
    ): GetAlbumListingsResponse

    @GET("drive/photos/albums/shared-with-me")
    suspend fun getAlbumSharedWithMeListings(
        @Query("AnchorID") anchorId: String? = null,
    ): GetAlbumListingsResponse

    @GET("drive/photos/volumes/{volumeID}/albums/{linkID}/children")
    suspend fun getAlbumPhotoListings(
        @Path("volumeID") volumeId: String,
        @Path("linkID") linkId: String,
        @Query("AnchorID") anchorId: String?,
        @Query("Sort") sort: String?,
        @Query("Desc") descending: Int?,
        @Query("OnlyChildren") onlyDirectChildren: Int = 0,
        @Query("IncludeTrashed") includeTrashedChildren: Int = 0,
    ): GetAlbumPhotoListingResponse

    @POST("drive/photos/volumes/{volumeID}/albums/{linkID}/add-multiple")
    suspend fun addToAlbum(
        @Path("volumeID") volumeId: String,
        @Path("linkID") albumId: String,
        @Body request: AddToAlbumRequest,
    ): AddToRemoveFromAlbumResponse

    @POST("drive/photos/volumes/{volumeID}/albums/{linkID}/remove-multiple")
    suspend fun removeFromAlbum(
        @Path("volumeID") volumeId: String,
        @Path("linkID") albumId: String,
        @Body request: RemoveFromAlbumRequest,
    ): AddToRemoveFromAlbumResponse

    @DELETE("drive/photos/volumes/{volumeID}/albums/{linkID}")
    suspend fun deleteAlbum(
        @Path("volumeID") volumeId: String,
        @Path("linkID") albumId: String,
        @Query("DeleteAlbumPhotos") deleteAlbumPhotos: Int,
    ): CodeResponse

    @POST("drive/photos/volumes/{volumeID}/links/{linkID}/favorite")
    suspend fun addFavorite(
        @Path("volumeID") volumeId: String,
        @Path("linkID") linkId: String,
        @Body request: FavoriteRequest,
    ): CodeResponse

    @POST("drive/photos/volumes/{volumeID}/links/{linkID}/tags")
    suspend fun addTags(
        @Path("volumeID") volumeId: String,
        @Path("linkID") linkId: String,
        @Body request: TagRequest,
    ): CodeResponse

    @HTTP(method = "DELETE", path = "/drive/photos/volumes/{volumeID}/links/{linkID}/tags", hasBody = true)
    suspend fun deleteTags(
        @Path("volumeID") volumeId: String,
        @Path("linkID") linkId: String,
        @Body request: TagRequest,
    ): CodeResponse

    @GET("drive/photos/migrate-legacy")
    suspend fun getPhotoShareMigrationStatus(): MigrationStatusResponse

    @POST("drive/photos/migrate-legacy")
    suspend fun startPhotoShareMigration(): CodeResponse
}
