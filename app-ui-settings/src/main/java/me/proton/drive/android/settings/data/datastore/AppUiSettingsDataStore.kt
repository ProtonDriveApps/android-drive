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

package me.proton.drive.android.settings.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.drive.base.data.datastore.BaseDataStore
import javax.inject.Inject

class AppUiSettingsDataStore @Inject constructor(
    @ApplicationContext appContext: Context,
) : BaseDataStore(APP_SETTINGS_PREFERENCES) {
    private val prefsKeyWelcomeFlowShown = booleanPreferencesKey(WELCOME_FLOW_SHOWN)
    var welcomeFlowShown by Delegate(appContext.dataStore, prefsKeyWelcomeFlowShown, default = false)

    companion object {
        const val APP_SETTINGS_PREFERENCES = "app_settings_prefs"
        const val WELCOME_FLOW_SHOWN = "welcome_flow_shown"
    }
}
