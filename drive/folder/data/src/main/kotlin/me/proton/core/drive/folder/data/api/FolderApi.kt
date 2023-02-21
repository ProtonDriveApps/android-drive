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
package me.proton.core.drive.folder.data.api

import me.proton.core.drive.folder.data.api.request.CreateFolderRequest
import me.proton.core.drive.folder.data.api.request.DeleteFolderChildrenRequest
import me.proton.core.drive.folder.data.api.response.CreateFolderResponse
import me.proton.core.drive.folder.data.api.response.GetFolderChildrenResponse
import me.proton.core.drive.link.data.api.response.LinkResponses
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FolderApi : BaseRetrofitApi {
    @GET("drive/shares/@{enc_shareID}/folders/@{enc_linkID}/children")
    suspend fun getFolderChildren(
        @Path("enc_shareID") shareId: String,
        @Path("enc_linkID") linkId: String,
        @Query("Page") page: Int,
        @Query("PageSize") pageSize: Int,
        @Query("Sort") sortingBy: String,
        @Query("Desc") isDescending: Int
    ): GetFolderChildrenResponse

    @POST("drive/shares/@{enc_shareID}/folders")
    suspend fun createFolder(
        @Path("enc_shareID") shareId: String,
        @Body request: CreateFolderRequest,
    ): CreateFolderResponse

    @POST("drive/shares/@{enc_shareID}/folders/@{enc_linkID}/delete_multiple")
    suspend fun deleteFolderChildren(
        @Path("enc_shareID") shareId: String,
        @Path("enc_linkID") folderLinkId: String,
        @Body request: DeleteFolderChildrenRequest,
    ): LinkResponses
}
