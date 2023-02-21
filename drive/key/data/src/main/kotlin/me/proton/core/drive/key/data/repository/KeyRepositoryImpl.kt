/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.key.data.repository

import android.util.LruCache
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.repository.KeyRepository
import javax.inject.Inject

class KeyRepositoryImpl @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
) : KeyRepository {
    private val usersCache: MutableMap<UserId, LruCache<String, Key>> = mutableMapOf()

    override fun addKey(userId: UserId, keyId: String, key: Key) {
        userId.cache[keyId] = key
    }

    override fun getKey(userId: UserId, keyId: String): Key? =
        userId.cache[keyId]

    override fun removeAll(userId: UserId) {
        usersCache[userId]?.evictAll()
    }

    private val UserId.cache: LruCache<String, Key>
        get() = usersCache.getOrPut(this) {
            LruCache<String, Key>(configurationProvider.cacheMaxEntries)
        }

    private operator fun LruCache<String, Key>.set(keyId: String, key: Key) = put(keyId, key)
}
