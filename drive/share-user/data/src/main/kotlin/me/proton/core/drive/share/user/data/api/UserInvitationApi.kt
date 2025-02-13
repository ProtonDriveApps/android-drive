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

package me.proton.core.drive.share.user.data.api

import me.proton.core.drive.base.data.api.response.Response
import me.proton.core.drive.share.user.data.api.request.AcceptUserInvitationRequest
import me.proton.core.drive.share.user.data.api.response.GetUserInvitationResponse
import me.proton.core.drive.share.user.data.api.response.GetUserInvitationsResponse
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface UserInvitationApi : BaseRetrofitApi {

    @GET("drive/v2/shares/invitations")
    suspend fun getInvitations(): GetUserInvitationsResponse

    @GET("drive/v2/shares/invitations/{invitationId}")
    suspend fun getInvitation(
        @Path("invitationId") invitationId: String,
    ): GetUserInvitationResponse

    @POST("drive/v2/shares/invitations/{invitationId}/accept")
    suspend fun acceptInvitation(
        @Path("invitationId") invitationId: String,
        @Body request: AcceptUserInvitationRequest,
    ): Response

    @POST("drive/v2/shares/invitations/{invitationId}/reject")
    suspend fun rejectInvitation(
        @Path("invitationId") invitationId: String,
    ): Response
}
