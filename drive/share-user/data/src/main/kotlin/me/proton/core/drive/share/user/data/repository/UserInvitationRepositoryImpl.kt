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

package me.proton.core.drive.share.user.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.user.data.api.UserInvitationApiDataSource
import me.proton.core.drive.share.user.data.api.request.AcceptUserInvitationRequest
import me.proton.core.drive.share.user.data.db.ShareUserDatabase
import me.proton.core.drive.share.user.data.db.entity.UserInvitationIdEntity
import me.proton.core.drive.share.user.data.extension.toEntity
import me.proton.core.drive.share.user.data.extension.toShareTargetTypeDtos
import me.proton.core.drive.share.user.data.extension.toUserInvitationId
import me.proton.core.drive.share.user.domain.entity.UserInvitation
import me.proton.core.drive.share.user.domain.entity.UserInvitationId
import me.proton.core.drive.share.user.domain.entity.ShareTargetType
import me.proton.core.drive.share.user.domain.repository.UserInvitationRepository
import me.proton.core.drive.share.user.domain.extension.userId
import javax.inject.Inject

class UserInvitationRepositoryImpl @Inject constructor(
    private val api: UserInvitationApiDataSource,
    private val db: ShareUserDatabase,
) : UserInvitationRepository {
    override suspend fun hasInvitations(userId: UserId, types: Set<ShareTargetType>): Boolean {
        return db.userInvitationIdDao.hasInvitations(userId)
    }

    override suspend fun fetchAndStoreInvitations(
        userId: UserId,
        types: Set<ShareTargetType>,
    ): List<UserInvitationId> {
        val dtoTypes = types.toShareTargetTypeDtos()
        val entities = mutableListOf<UserInvitationIdEntity>()
        var anchorId: String? = null
        var hasMore = true
        while (hasMore) {
            val response = api.getInvitations(
                userId = userId,
                anchorId = anchorId,
            )
            anchorId = response.anchorId
            hasMore = response.hasMore
            entities.addAll(
                response
                    .invitations
                    .map { userInvitationIdDto -> userInvitationIdDto.toEntity(userId) }
            )
        }
        db.inTransaction {
            db.userInvitationIdDao.deleteAll(
                userId = userId,
                types = dtoTypes,
                includeNullType = types.contains(ShareTargetType.Album).not(),
            )
            db.userInvitationIdDao.insertOrUpdate(*entities.toTypedArray())
        }
        return entities.map { entity -> entity.toUserInvitationId() }
    }

    override suspend fun fetchAndStoreInvitation(userId: UserId, invitationId: String) {
        val entity = api.getInvitation(userId, invitationId).toEntity(userId)
        db.userInvitationDetailsDao.insertOrUpdate(entity)
    }

    override suspend fun getInvitation(
        id: UserInvitationId,
    ): UserInvitation = db.userInvitationDetailsDao.getInvitation(
        userId = id.shareId.userId,
        volumeId = id.volumeId.id,
        shareId = id.shareId.id,
        id = id.invitationId
    ).toUserInvitationId()

    override fun getInvitationsFlow(
        userId: UserId,
        limit: Int,
        types: Set<ShareTargetType>,
    ): Flow<List<UserInvitation>> =
        db.userInvitationDetailsDao.getInvitationsFlow(
            userId = userId,
            limit = limit,
            types = types.toShareTargetTypeDtos(),
            includeNullType = types.contains(ShareTargetType.Album).not(),
        ).map { entities ->
            entities.map { entity -> entity.toUserInvitationId() }
        }

    override fun getInvitationsCountFlow(
        userId: UserId,
        types: Set<ShareTargetType>,
    ): Flow<Int> =
        db.userInvitationIdDao.getInvitationsCountFlow(
            userId = userId,
            types = types.toShareTargetTypeDtos(),
            includeNullType = types.contains(ShareTargetType.Album).not(),
        )

    override suspend fun acceptInvitation(
        invitationId: UserInvitationId,
        sessionKeySignature: String,
    ) {
        api.acceptInvitation(
            userId = invitationId.userId,
            invitationId = invitationId.invitationId,
            request = AcceptUserInvitationRequest(sessionKeySignature),
        )
        db.userInvitationIdDao.deleteInvitation(
            userId = invitationId.userId,
            volumeId = invitationId.volumeId.id,
            shareId = invitationId.shareId.id,
            invitationId = invitationId.invitationId,
        )
    }

    override suspend fun rejectInvitation(invitationId: UserInvitationId) {
        api.rejectInvitation(
            userId = invitationId.userId,
            invitationId = invitationId.invitationId,
        )
        db.userInvitationIdDao.deleteInvitation(
            userId = invitationId.userId,
            volumeId = invitationId.volumeId.id,
            shareId = invitationId.shareId.id,
            invitationId = invitationId.invitationId,
        )
    }
}
