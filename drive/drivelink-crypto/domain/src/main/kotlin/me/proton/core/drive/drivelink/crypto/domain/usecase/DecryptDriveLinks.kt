/*
 * Copyright (c) 2022-2023 Proton AG.
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

package me.proton.core.drive.drivelink.crypto.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.cryptobase.domain.usecase.UnlockKey
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetLinkParentKey
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.util.kotlin.CoreLogger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class DecryptDriveLinks @Inject constructor(
    private val getLinkParentKey: GetLinkParentKey,
    private val unlockKey: UnlockKey,
    private val decryptDriveLink: DecryptDriveLink,
    private val configurationProvider: ConfigurationProvider,
) {

    suspend operator fun invoke(driveLinks: List<DriveLink>): List<DriveLink> {
        val decryptedLinks = ConcurrentHashMap<LinkId, DriveLink?>()
        driveLinks.groupBy { driveLink -> driveLink.parentId }.forEach { (_, links) ->
            links.firstOrNull()?.let { firstLink -> getLinkParentKey(firstLink) }?.map { parentKey ->
                unlockKey(parentKey.keyHolder) { unlockedKey ->
                    supervisorScope {
                        val deferred = links.chunkedInto(configurationProvider.decryptionInParallel).map { driveLinks ->
                            async {
                                driveLinks.forEach { link ->
                                    decryptedLinks[link.id] = decryptDriveLink(unlockedKey, link)
                                        .onFailure { error ->
                                            CoreLogger.e(
                                                LogTag.ENCRYPTION,
                                                error,
                                                "There was an error decrypting drive link: ${link.id.id.logId()}"
                                            )
                                        }.getOrNull()
                                }
                            }
                        }
                        deferred.awaitAll()
                    }
                }
            }
        }
        return driveLinks.map { driveLink -> decryptedLinks[driveLink.id] ?: driveLink }
    }

    private fun<T> List<T>.chunkedInto(numberOfSublist: Int): List<List<T>> = chunked(
        Math.ceil(size.toDouble() / numberOfSublist).toInt()
    )
}
