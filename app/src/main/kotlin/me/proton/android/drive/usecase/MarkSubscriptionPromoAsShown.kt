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

package me.proton.android.drive.usecase

import androidx.datastore.preferences.core.edit
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.data.datastore.GetUserDataStore.Keys.subscriptionLastUpdate
import me.proton.core.drive.base.domain.util.coRunCatching
import java.util.Date
import javax.inject.Inject

class MarkSubscriptionPromoAsShown @Inject constructor(
    private val getUserDataStore: GetUserDataStore,
) {
    suspend operator fun invoke(userId: UserId, key: String) = coRunCatching {
        getUserDataStore(userId).edit { preferences ->
            preferences[subscriptionLastUpdate(key)] = Date().time
        }
    }
}
