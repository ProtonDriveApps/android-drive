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

package me.proton.core.drive.share.user.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.entity.ShareUser

interface ShareMemberRepository {
    suspend fun hasMembers(shareId: ShareId): Boolean
    suspend fun fetchAndStoreMembers(
        shareId: ShareId,
        ignoredIds: List<String> = emptyList()
    ): List<ShareUser.Member>

    fun getMembersFlow(shareId: ShareId, limit: Int): Flow<List<ShareUser.Member>>
    fun getMemberFlow(shareId: ShareId, memberId: String): Flow<ShareUser.Member>
    suspend fun updateMember(shareId: ShareId, memberId: String, permissions: Permissions)
    suspend fun deleteMember(shareId: ShareId, memberId: String)
}
