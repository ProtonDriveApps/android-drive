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
package me.proton.core.drive.base.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.reflect.KProperty

abstract class BaseDataStore(preferenceName: String) {
    protected val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = preferenceName)

    class Delegate<T>(
        private val dataStore: DataStore<Preferences>,
        private val key: Preferences.Key<T>,
        private val default: T
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = runBlocking {
            key.asFlow(dataStore, default).first()
        }
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = runBlocking {
            dataStore.edit { preferences ->
                preferences[key] = value
            }
        }
    }
}

fun <T> Preferences.Key<T>.asFlow(dataStore: DataStore<Preferences>, default: T) : Flow<T> =
    dataStore.data
        .catch { cause ->
            if (cause is IOException) {
                emit(emptyPreferences())
            } else {
                throw cause
            }
        }
        .map { preferences ->
            preferences[this] ?: default
        }
