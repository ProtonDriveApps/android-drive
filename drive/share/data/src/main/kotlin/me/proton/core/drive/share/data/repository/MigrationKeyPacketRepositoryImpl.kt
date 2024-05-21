/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.share.data.repository

import androidx.datastore.preferences.core.edit
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.data.datastore.migrationKeyPacketLastUpdate
import me.proton.core.drive.base.data.extension.get
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.share.data.api.ShareApiDataSource
import me.proton.core.drive.share.data.api.request.PassphraseNodeKeyPacket
import me.proton.core.drive.share.data.api.request.ShareAccessWithNodeRequest
import me.proton.core.drive.share.data.api.response.UpdateUnmigratedSharesResponse
import me.proton.core.drive.share.domain.entity.ShareAccessWithNode
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.repository.MigrationKeyPacketRepository
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class MigrationKeyPacketRepositoryImpl @Inject constructor(
    private val api: ShareApiDataSource,
    private val getUserDataStore: GetUserDataStore,
) : MigrationKeyPacketRepository {
    override suspend fun getAll(
        userId: UserId,
    ): Result<List<ShareId>> = coRunCatching {
        api.getUnmigratedShares(userId).map { id -> ShareId(userId, id) }
    }

    override suspend fun update(
        userId: UserId,
        shareAccessWithNode: ShareAccessWithNode,
    ): Result<List<String>> = coRunCatching {
        api.updateUnmigratedShares(
            userId, ShareAccessWithNodeRequest(
                passphraseNodeKeyPackets = shareAccessWithNode.passphraseNodeKeyPackets.map { (shareId, passphrase) ->
                    PassphraseNodeKeyPacket(shareId.id, passphrase)
                },
                unreadableShareIds = shareAccessWithNode.unreadableShareIds.map { shareId -> shareId.id },
            )
        ).let { response: UpdateUnmigratedSharesResponse ->
            if (response.errors.isEmpty()) {
                response.shareIds
            } else {
                response.errors.forEach { e ->
                    CoreLogger.w(SHARING, "${e.code} ${e.error}")
                }
                val error = response.errors.first()
                error("${response.errors.count()} errors during migration, first: ${error.code} ${error.error}")
            }
        }
    }

    override suspend fun getLastUpdate(userId: UserId): TimestampS? =
        getUserDataStore(userId).get(migrationKeyPacketLastUpdate)?.let(::TimestampS)

    override suspend fun setLastUpdate(userId: UserId, date: TimestampS) {
        getUserDataStore(userId).edit { preferences ->
            preferences[migrationKeyPacketLastUpdate] = date.value
        }
    }
}
