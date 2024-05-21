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

import android.util.LruCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.extension.keyHolder
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.entity.PublicAddressKeys
import me.proton.core.key.domain.entity.key.PublicAddress
import me.proton.core.key.domain.repository.Source
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetPublicAddressKeys @Inject constructor(
    private val getPublicAddress: GetPublicAddress,
    private val getAddressKeys: GetAddressKeys,
    private val configurationProvider: ConfigurationProvider,
) {
    private val usersCache: MutableMap<UserId, LruCache<String, Key>> = mutableMapOf()
    private val mutex = Mutex()

    suspend operator fun invoke(userId: UserId, email: String): Result<Key> = coRunCatching {
        mutex.withLock {
            userId.cache.getOrPut(email) {
                createKey(userId, email)
            }
        }
    }

    private val UserId.cache: LruCache<String, Key>
        get() = usersCache.getOrPut(this) {
            LruCache<String, Key>(configurationProvider.cacheMaxEntries)
        }

    private suspend fun LruCache<String, Key>.getOrPut(email: String, defaultValue: suspend () -> Key): Key =
        get(email) ?: put(email, defaultValue)

    private suspend fun LruCache<String, Key>.put(email: String, defaultValue: suspend () -> Key) =
        defaultValue().also { key -> put(email, key) }

    private suspend fun createKey(userId: UserId, email: String): Key =
        userId.getPublicAddress(email)
            .getOrNull()
            ?.let { publicAddress ->
                PublicAddressKeys(publicAddress.keyHolder())
            } ?: getAddressKeys(userId, email)

    private suspend fun UserId.getPublicAddress(email: String): Result<PublicAddress> = coRunCatching {
        getPublicAddress(this, email, Source.RemoteNoCache).getOrNull()
            ?: getPublicAddress(this, email, Source.RemoteOrCached).getOrThrow()
    }
}
