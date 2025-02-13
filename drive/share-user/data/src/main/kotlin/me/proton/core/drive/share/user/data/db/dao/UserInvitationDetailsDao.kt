/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.share.user.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.user.data.db.entity.UserInvitationDetailsEntity
import me.proton.core.drive.share.user.data.db.entity.UserInvitationIdAndDetailsEntity
import me.proton.core.drive.share.user.data.db.entity.UserInvitationIdEntity

@Dao
abstract class UserInvitationDetailsDao : BaseDao<UserInvitationDetailsEntity>() {

    @Query(
        """
            SELECT 
                UserInvitationIdEntity.user_id, 
                UserInvitationIdEntity.volume_id,
                UserInvitationIdEntity.share_id,
                UserInvitationIdEntity.id,
                UserInvitationDetailsEntity.user_id AS details_user_id,
                UserInvitationDetailsEntity.volume_id AS details_volume_id,
                UserInvitationDetailsEntity.share_id AS details_share_id,
                UserInvitationDetailsEntity.id AS details_id,
                UserInvitationDetailsEntity.invitee_email AS details_invitee_email,
                UserInvitationDetailsEntity.inviter_email AS details_inviter_email,
                UserInvitationDetailsEntity.permissions AS details_permissions,
                UserInvitationDetailsEntity.key_packet AS details_key_packet,
                UserInvitationDetailsEntity.key_packet_signature AS details_key_packet_signature,
                UserInvitationDetailsEntity.create_time AS details_create_time,
                UserInvitationDetailsEntity.passphrase AS details_passphrase,
                UserInvitationDetailsEntity.share_key AS details_share_key,
                UserInvitationDetailsEntity.creator_email AS details_creator_email,
                UserInvitationDetailsEntity.type AS details_type,
                UserInvitationDetailsEntity.link_id AS details_link_id,
                UserInvitationDetailsEntity.name AS details_name,
                UserInvitationDetailsEntity.mime_type AS details_mime_type
            FROM UserInvitationIdEntity
            LEFT JOIN UserInvitationDetailsEntity 
            ON UserInvitationIdEntity.user_id = UserInvitationDetailsEntity.user_id
            AND UserInvitationIdEntity.volume_id = UserInvitationDetailsEntity.volume_id
            AND UserInvitationIdEntity.share_id = UserInvitationDetailsEntity.share_id
            AND UserInvitationIdEntity.id = UserInvitationDetailsEntity.id
            WHERE UserInvitationIdEntity.user_id = :userId
                AND UserInvitationIdEntity.volume_id = :volumeId
                AND UserInvitationIdEntity.share_id = :shareId
                AND UserInvitationIdEntity.id = :id
        """
    )
    abstract suspend fun getInvitation(
        userId: UserId,
        volumeId: String,
        shareId: String,
        id: String,
    ): UserInvitationIdAndDetailsEntity

    @Query(
        """
            SELECT 
                UserInvitationIdEntity.user_id, 
                UserInvitationIdEntity.volume_id,
                UserInvitationIdEntity.share_id,
                UserInvitationIdEntity.id,
                UserInvitationDetailsEntity.user_id AS details_user_id,
                UserInvitationDetailsEntity.volume_id AS details_volume_id,
                UserInvitationDetailsEntity.share_id AS details_share_id,
                UserInvitationDetailsEntity.id AS details_id,
                UserInvitationDetailsEntity.invitee_email AS details_invitee_email,
                UserInvitationDetailsEntity.inviter_email AS details_inviter_email,
                UserInvitationDetailsEntity.permissions AS details_permissions,
                UserInvitationDetailsEntity.key_packet AS details_key_packet,
                UserInvitationDetailsEntity.key_packet_signature AS details_key_packet_signature,
                UserInvitationDetailsEntity.create_time AS details_create_time,
                UserInvitationDetailsEntity.passphrase AS details_passphrase,
                UserInvitationDetailsEntity.share_key AS details_share_key,
                UserInvitationDetailsEntity.creator_email AS details_creator_email,
                UserInvitationDetailsEntity.type AS details_type,
                UserInvitationDetailsEntity.link_id AS details_link_id,
                UserInvitationDetailsEntity.name AS details_name,
                UserInvitationDetailsEntity.mime_type AS details_mime_type
            FROM UserInvitationIdEntity
            LEFT JOIN UserInvitationDetailsEntity 
            ON UserInvitationIdEntity.user_id = UserInvitationDetailsEntity.user_id
            AND UserInvitationIdEntity.volume_id = UserInvitationDetailsEntity.volume_id
            AND UserInvitationIdEntity.share_id = UserInvitationDetailsEntity.share_id
            AND UserInvitationIdEntity.id = UserInvitationDetailsEntity.id
            WHERE UserInvitationIdEntity.user_id = :userId
            ORDER BY UserInvitationDetailsEntity.create_time DESC
            LIMIT :limit
        """
    )
    abstract fun getInvitationsFlow(
        userId: UserId,
        limit: Int,
    ): Flow<List<UserInvitationIdAndDetailsEntity>>
}
