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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetAllMembershipId
import me.proton.core.drive.share.user.data.api.ShareMemberApiDataSource
import me.proton.core.drive.share.user.data.api.request.UpdateShareMemberRequest
import me.proton.core.drive.share.user.data.db.ShareUserDatabase
import me.proton.core.drive.share.user.data.extension.toEntity
import me.proton.core.drive.share.user.data.extension.toShareUserMember
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.share.user.domain.repository.ShareMemberRepository
import javax.inject.Inject

class ShareMemberRepositoryImpl @Inject constructor(
    private val api: ShareMemberApiDataSource,
    private val db: ShareUserDatabase,
) : ShareMemberRepository {
    override suspend fun hasMembers(shareId: ShareId): Boolean =
        db.shareMemberDao.hasMembers(shareId.userId, shareId.id)

    override suspend fun fetchAndStoreMembers(shareId: ShareId, ignoredIds: List<String>): List<ShareUser.Member> {
        val members = api.getMembers(shareId.userId, shareId.id).members.map { dto ->
            dto.toShareUserMember()
        }.filter { member -> member.id !in ignoredIds }
        db.inTransaction {
            db.shareMemberDao.deleteAll(shareId.userId, shareId.id)
            db.shareMemberDao.insertOrUpdate(*members.map { member ->
                member.toEntity(shareId)
            }.toTypedArray())
        }
        return members
    }

    override fun getMembersFlow(
        shareId: ShareId,
        limit: Int,
    ): Flow<List<ShareUser.Member>> =
        db.shareMemberDao.getMembersFlow(
            userId = shareId.userId,
            shareId = shareId.id,
            limit = limit,
        ).map { entities ->
            entities.map { entity -> entity.toShareUserMember() }
        }

    override fun getMemberFlow(
        shareId: ShareId,
        memberId: String,
    ): Flow<ShareUser.Member> =
        db.shareMemberDao.getMemberFlow(
            userId = shareId.userId,
            shareId = shareId.id,
            memberId = memberId,
        ).filterNotNull().map { member ->
            member.toShareUserMember()
        }

    override suspend fun updateMember(
        shareId: ShareId,
        memberId: String,
        permissions: Permissions,
    ) {
        api.updateMember(
            userId = shareId.userId,
            shareId = shareId.id,
            memberId = memberId,
            request = UpdateShareMemberRequest(
                permissions = permissions.value
            )
        )
        db.shareMemberDao.updatePermission(
            userId = shareId.userId,
            shareId = shareId.id,
            memberId = memberId,
            permissions = permissions.value,
        )
    }

    override suspend fun deleteMember(shareId: ShareId, memberId: String) {
        api.deleteMember(
            userId = shareId.userId,
            shareId = shareId.id,
            memberId = memberId,
        )
        db.shareMemberDao.deleteMember(
            userId = shareId.userId,
            shareId = shareId.id,
            memberId = memberId,
        )
    }
}
