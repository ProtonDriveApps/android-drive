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

package me.proton.android.drive.ui.test

import androidx.annotation.RestrictTo
import androidx.datastore.preferences.core.edit
import me.proton.android.drive.usecase.MarkOnboardingAsShown
import me.proton.android.drive.usecase.MarkRatingBoosterAsShown
import me.proton.android.drive.usecase.MarkWhatsNewAsShown
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.data.datastore.GetUserDataStore.Keys.subscriptionLastUpdate
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.drive.android.settings.data.datastore.AppUiSettingsDataStore
import me.proton.drive.android.settings.domain.entity.WhatsNewKey
import java.util.Date
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.TESTS)
class UiTestHelper @Inject constructor(
    val configurationProvider: ConfigurationProvider,
    private val appUiSettingsDataStore: AppUiSettingsDataStore,
    private val getUserDataStore: GetUserDataStore,
) {
    suspend fun doNotShowOnboardingAfterLogin() {
        appUiSettingsDataStore.onboardingShown = 1L
    }

    suspend fun doNotShowWhatsNewAfterLogin() {
        appUiSettingsDataStore.WhatsNew(WhatsNewKey.ALBUMS.name).shown = 1L
    }

    suspend fun doNotShowRatingBoosterAfterLogin() {
        appUiSettingsDataStore.ratingBooster = 1L
    }

    suspend fun doNotShowDrivePlusPromoAfterLogin(userId: UserId) {
        getUserDataStore(userId).edit { preferences ->
            preferences[subscriptionLastUpdate("drive2022")] = 1L
        }
    }

    suspend fun doNotShowDriveLitePromoAfterLogin(userId: UserId) {
        getUserDataStore(userId).edit { preferences ->
            preferences[subscriptionLastUpdate("drivelite2024")] = 1L
        }
    }
}
