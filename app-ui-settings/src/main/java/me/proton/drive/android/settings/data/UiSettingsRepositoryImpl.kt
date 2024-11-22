/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.drive.android.settings.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.drive.android.settings.data.datastore.AppUiSettingsDataStore
import me.proton.drive.android.settings.data.db.AppUiSettingsDatabase
import me.proton.drive.android.settings.data.extension.toDomain
import me.proton.drive.android.settings.data.extension.toEntity
import me.proton.drive.android.settings.domain.UiSettingsRepository
import me.proton.drive.android.settings.domain.entity.HomeTab
import me.proton.drive.android.settings.domain.entity.LayoutType
import me.proton.drive.android.settings.domain.entity.ThemeStyle
import me.proton.drive.android.settings.domain.entity.UiSettings
import javax.inject.Inject

class UiSettingsRepositoryImpl @Inject constructor(
    private val database: AppUiSettingsDatabase,
    private val dataStore: AppUiSettingsDataStore,
) : UiSettingsRepository {

    private val dao = database.uiSettingsDao

    override fun getUiSettingsFlow(userId: UserId): Flow<UiSettings> =
        dao.getFlow(userId)
            .distinctUntilChanged()
            .map { entity ->
                entity?.toDomain() ?: UiSettings()
            }

    override suspend fun updateLayoutType(userId: UserId, layoutType: LayoutType) {
        updateOrInsert(userId, { dao.updateLayoutType(userId, layoutType) }) {
            UiSettings(layoutType = layoutType)
        }
    }

    override suspend fun updateThemeStyle(userId: UserId, themeStyle: ThemeStyle) {
        updateOrInsert(userId, { dao.updateThemeStyle(userId, themeStyle) }) {
            UiSettings(themeStyle = themeStyle)
        }
    }

    override suspend fun updateHomeTab(userId: UserId, homeTab: HomeTab) {
        updateOrInsert(userId, { dao.updateHomeTab(userId, homeTab) }) {
            UiSettings(homeTab = homeTab)
        }
    }

    override suspend fun hasShownOnboarding(): Boolean =
        dataStore.onboardingShown > 0L

    override suspend fun updateOnboardingShown(timestamp: TimestampS) {
        dataStore.onboardingShown = timestamp.value
    }


    private suspend inline fun updateOrInsert(
        userId: UserId,
        crossinline update: suspend () -> Int,
        crossinline default: () -> UiSettings,
    ) {
        database.inTransaction {
            if (update() == 0) {
                dao.insertOrUpdate(default().toEntity(userId))
            }
        }
    }
}
