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

package me.proton.core.drive.key.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.api.ProtonApiCode.KEY_GET_ADDRESS_MISSING
import me.proton.core.drive.base.domain.api.ProtonApiCode.KEY_GET_DOMAIN_EXTERNAL
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.isOlderThen
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.repository.BaseRepository
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.key.domain.repository.StalePublicAddressKeyRepository
import me.proton.core.key.domain.entity.key.PublicAddressInfo
import me.proton.core.key.domain.repository.PublicAddressRepository
import me.proton.core.key.domain.repository.Source
import me.proton.core.network.domain.hasProtonErrorCode
import javax.inject.Inject

class GetPublicAddressInfo @Inject constructor(
    private val publicAddressRepository: PublicAddressRepository,
    private val hasStalePublicAddressKeys: HasStalePublicAddressKeys,
    private val removeAllStalePublicAddressKeys: RemoveAllStalePublicAddressKeys,
    private val stalePublicAddressKeyRepository: StalePublicAddressKeyRepository,
    private val baseRepository: BaseRepository,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(
        userId: UserId,
        email: String,
        source: Source,
    ) = coRunCatching {
        publicAddressRepository.getPublicAddressInfo(
            sessionUserId = userId,
            email = email,
            internalOnly = true,
            source = source,
        )
    }.map { publicAddressInfo ->
        publicAddressInfo.takeUnless { publicAddressInfo.address.keys.isEmpty() }
    }.recoverCatching { error ->
        if (error.hasProtonErrorCode(KEY_GET_ADDRESS_MISSING)
            || error.hasProtonErrorCode(KEY_GET_DOMAIN_EXTERNAL)
        ) {
            null
        } else {
            throw error
        }
    }

    suspend operator fun invoke(userId: UserId, email: String): Result<PublicAddressInfo?> = coRunCatching {
        val url = "$publicAddressInfoUrl?email=$email"
        if (hasStalePublicAddressKeys(userId, email).getOrThrow() && isAllowedToFetch(userId, url)) {
            invoke(userId, email, Source.RemoteNoCache).getOrThrow().also {
                baseRepository.setLastFetch(userId, url, TimestampMs())
                removeAllStalePublicAddressKeys(userId, email).getOrNull(LogTag.KEY)
            }
        } else {
            invoke(userId, email, Source.LocalIfAvailable).getOrThrow()
        }
    }

    private suspend fun isAllowedToFetch(userId: UserId, url: String): Boolean = baseRepository
        .getLastFetch(userId, url)
        .isOlderThen(configurationProvider.minimumPublicAddressKeyFetchInterval)

    private val publicAddressInfoUrl: String get() = stalePublicAddressKeyRepository.publicAddressInfoUrl
}
