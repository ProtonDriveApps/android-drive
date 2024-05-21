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

package me.proton.core.drive.base.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.StorageLocationProvider
import java.io.File
import javax.inject.Inject

class GetUserDataStore @Inject constructor(
    private val storageLocationProvider: StorageLocationProvider,
) {

    private val map = mutableMapOf<UserId, DataStore<Preferences>>()
    suspend operator fun invoke(userId: UserId): DataStore<Preferences> = map.getOrPut(userId) {
        val parent = storageLocationProvider.getPermanentFolder(userId, "datastore")
        val file = File(parent, "user.preferences_pb")
        PreferenceDataStoreFactory.create(
            produceFile = { file }
        )
    }
}
