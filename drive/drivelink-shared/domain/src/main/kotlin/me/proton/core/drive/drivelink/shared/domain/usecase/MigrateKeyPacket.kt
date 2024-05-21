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

package me.proton.core.drive.drivelink.shared.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.hasHttpCode
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.base.ReencryptKeyPacket
import me.proton.core.drive.drivelink.shared.domain.extension.passphrase
import me.proton.core.drive.drivelink.shared.domain.extension.sharingDetails
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareAccessWithNode
import me.proton.core.drive.share.domain.repository.MigrationKeyPacketRepository
import me.proton.core.drive.share.domain.usecase.GetShares
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.filterNullValues
import javax.inject.Inject

class MigrateKeyPacket @Inject constructor(
    private val repository: MigrationKeyPacketRepository,
    private val getShares: GetShares,
    private val getSharedDriveLinks: GetSharedDriveLinks,
    private val reencryptKeyPacket: ReencryptKeyPacket,
    private val getNodeKey: GetNodeKey,
) {

    suspend operator fun invoke(userId: UserId) = coRunCatching {
        val shares = getShares(userId, Share.Type.STANDARD)
            .filterSuccessOrError()
            .toResult()
            .getOrThrow()
        while (true) {
            val shareIds = repository.getAll(userId).onFailure { error ->
                if (error.hasHttpCode(404)) {
                    CoreLogger.d(SHARING, "No shares to migrate")
                    return@coRunCatching Result.success(Unit)
                }
            }.getOrThrow()

            if (shareIds.isEmpty()) {
                CoreLogger.d(SHARING, "No shares to migrate")
                repository.setLastUpdate(userId, TimestampS())
                return@coRunCatching Result.success(Unit)
            }

            val volumeIds = shares.filter { share -> share.id in shareIds }.map { share ->
                share.volumeId
            }.distinct()
            val links = volumeIds.flatMap { volumeId ->
                getSharedDriveLinks(userId, volumeId).filterSuccessOrError().toResult().getOrThrow()
            }

            val result = shareIds.associateWith { shareId ->
                coRunCatching {
                    val share =
                        requireNotNull(shares.firstOrNull { share -> share.id == shareId }) {
                            "Share not found: $shareId"
                        }

                    val link = links.first { link ->
                        link.id.id == share.rootLinkId && link.sharingDetails?.shareId == shareId
                    }

                    reencryptKeyPacket(
                        message = link.passphrase,
                        linkId = link.id,
                        shareKey = getNodeKey(link.id).getOrThrow(),
                    ).getOrThrow()
                }.fold(
                    onFailure = { error ->
                        CoreLogger.e(
                            SHARING,
                            error,
                            "Cannot generate passphrase node key packets for $shareId"
                        )
                        null
                    },
                    onSuccess = { passphraseNodeKeyPacket ->
                        passphraseNodeKeyPacket
                    }
                )
            }

            val migrated = repository.update(
                userId, ShareAccessWithNode(
                    passphraseNodeKeyPackets = result.filterNullValues(),
                    unreadableShareIds = result.filter { (_, value) -> value == null }.keys.toList()
                )
            ).getOrThrow()
            CoreLogger.d(SHARING, "${migrated.count()} shares migrated")
        }
    }
}
