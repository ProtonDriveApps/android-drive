/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.base.data.util

import androidx.collection.LruCache

class KeyTrackingLruCache<K : Any, V : Any>(maxSize: Int) {

    private val cache = object : LruCache<K, V>(maxSize) {
        override fun entryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V?) {
            if (evicted) keySet.remove(key)
            super.entryRemoved(evicted, key, oldValue, newValue)
        }
    }

    private val keySet = mutableSetOf<K>()

    fun put(key: K, value: V): V? {
        keySet.add(key)
        return cache.put(key, value)
    }

    fun get(key: K): V? = cache.get(key)

    fun remove(key: K): V? {
        keySet.remove(key)
        return cache.remove(key)
    }

    fun keys(): Set<K> = keySet.toSet()

    fun size(): Int = cache.size()

    fun evictAll(){
        keySet.clear()
        cache.evictAll()
    }
}
