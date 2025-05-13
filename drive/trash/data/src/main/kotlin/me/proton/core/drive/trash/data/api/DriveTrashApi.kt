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
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface DriveTrashApi : BaseRetrofitApi {

    @POST("drive/v2/volumes/{volumeID}/trash_multiple")
    suspend fun sendToTrash(
        @Path("volumeID") volumeId: String,
        @Body request: LinkIDsRequest,
    ): LinkResponses

    @PUT("drive/v2/volumes/{volumeID}/trash/restore_multiple")
    suspend fun restoreFromTrash(
        @Path("volumeID") shareId: String,
        @Body request: LinkIDsRequest,
    ): LinkResponses

    @POST("drive/v2/volumes/{volumeID}/trash/delete_multiple")
    suspend fun deleteItemsFromTrash(
        @Path("volumeID") shareId: String,
        @Body request: LinkIDsRequest,
    ): LinkResponses
}
