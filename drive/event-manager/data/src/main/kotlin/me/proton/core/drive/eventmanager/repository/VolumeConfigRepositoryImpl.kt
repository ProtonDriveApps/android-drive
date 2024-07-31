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

package me.proton.core.drive.eventmanager.repository

import android.util.LruCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.eventmanager.entity.VolumeConfig
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class VolumeConfigRepositoryImpl @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
) : VolumeConfigRepository {
    private val usersCache: MutableMap<UserId, LruCache<String, VolumeConfig>> = mutableMapOf()
    private val mutex = Mutex()

    override suspend fun add(userId: UserId, volumeId: VolumeId, config: VolumeConfig) {
        mutex.withLock {
            userId.cache.put(volumeId.id, config)
        }
    }

    override suspend fun get(userId: UserId, volumeId: VolumeId): VolumeConfig? =
        mutex.withLock {
            userId.cache.get(volumeId.id)
        }

    override suspend fun removeAll(userId: UserId) {
        mutex.withLock {
            userId.cache.evictAll()
        }
    }

    private val UserId.cache: LruCache<String, VolumeConfig>
        get() = usersCache.getOrPut(this) {
            LruCache<String, VolumeConfig>(configurationProvider.cacheMaxEntries)
        }
}
