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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.StorageLocationProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetUserDataStore @Inject constructor(
    private val storageLocationProvider: StorageLocationProvider,
) {
    private val mutex = Mutex()
    private val userDataStore = mutableMapOf<UserId, DataStore<Preferences>>()

    suspend operator fun invoke(userId: UserId): DataStore<Preferences> = mutex.withLock {
        userDataStore.getOrPut(userId) {
            val parent = storageLocationProvider.getPermanentFolder(userId, DATASTORE_ROOT)
            val file = File(parent, DATASTORE_USER_PREFERENCES_FILE)
            PreferenceDataStoreFactory.create(
                produceFile = { file }
            )
        }
    }

    companion object {
        private const val DATASTORE_ROOT = "datastore"
        private const val DATASTORE_USER_PREFERENCES_FILE = "user.preferences_pb"
        private const val KEY_MIGRATION_KEY_PACKET_LAST_UPDATE = "migration_key_packet_last_update"
        private const val KEY_CREATE_DOCUMENT_ACITON_INVOKED = "create_document_action_invoked"
        private const val KEY_NOTIFICATION_PERMISSION_RATIONALE_REJECTED = "notification_permission_rationale_rejected"
        private const val KEY_NEW_ALBUM_NAME = "new_album_name"
    }

    data object Keys {
        val migrationKeyPacketLastUpdate get() = longPreferencesKey(KEY_MIGRATION_KEY_PACKET_LAST_UPDATE)
        val createDocumentActionInvoked get() = booleanPreferencesKey(KEY_CREATE_DOCUMENT_ACITON_INVOKED)
        val notificationPermissionRationaleRejected get() = booleanPreferencesKey(
            KEY_NOTIFICATION_PERMISSION_RATIONALE_REJECTED
        )
        val newAlbumName get() = stringPreferencesKey(KEY_NEW_ALBUM_NAME)
    }
}
