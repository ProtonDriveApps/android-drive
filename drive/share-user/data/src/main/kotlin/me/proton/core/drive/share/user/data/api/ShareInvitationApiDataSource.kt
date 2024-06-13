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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.user.data.api.request.CreateShareInvitationRequest
import me.proton.core.drive.share.user.data.api.request.UpdateShareInvitationRequest
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareInvitationApiDataSource @Inject constructor(
    private val apiProvider: ApiProvider,
) {

    @Throws(ApiException::class)
    suspend fun getInvitations(
        userId: UserId,
        shareId: String,
    ) =
        apiProvider.get<ShareInvitationApi>(userId).invoke {
            getInvitations(shareId)
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun postInvitation(
        userId: UserId,
        shareId: String,
        request: CreateShareInvitationRequest,
    ) =
        apiProvider.get<ShareInvitationApi>(userId).invoke {
            postInvitation(shareId, request)
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun updateInvitation(
        userId: UserId,
        shareId: String,
        invitationId: String,
        request: UpdateShareInvitationRequest,
    ) =
        apiProvider.get<ShareInvitationApi>(userId).invoke {
            updateInvitation(shareId, invitationId, request)
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun deleteInvitation(
        userId: UserId,
        shareId: String,
        invitationId: String,
    ) =
        apiProvider.get<ShareInvitationApi>(userId).invoke {
            deleteInvitation(shareId, invitationId)
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun sendEmail(
        userId: UserId,
        shareId: String,
        invitationId: String,
    ) =
        apiProvider.get<ShareInvitationApi>(userId).invoke {
            sendEmail(shareId, invitationId)
        }.valueOrThrow

}
