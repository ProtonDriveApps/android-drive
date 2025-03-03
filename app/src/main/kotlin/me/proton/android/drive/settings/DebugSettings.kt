/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.provider.BuildConfigurationProvider
import me.proton.core.drive.base.data.datastore.BaseDataStore
import me.proton.core.drive.base.data.datastore.asFlow
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class DebugSettings(
    @ApplicationContext private val context: Context,
    private val buildConfig: BuildConfigurationProvider,
) : BaseDataStore(DEBUG_SETTINGS_PREFERENCES), ConfigurationProvider {
    private val prefsKeyHost = stringPreferencesKey(HOST)
    private val prefsKeyBaseUrl = stringPreferencesKey(BASE_URL)
    private val prefsKeyAppVersionHeader = stringPreferencesKey(APP_VERSION_HEADER)
    private val prefsUseExceptionMessage = booleanPreferencesKey(USE_EXCEPTION_MESSAGE)
    private val prefsLogToFileEnabled = booleanPreferencesKey(LOG_TO_FILE_ENABLED)
    private val prefsAllowBackupDeletedFilesEnabled = booleanPreferencesKey(ALLOW_BACKUP_DELETED_FILES_ENABLED)
    private val prefsFeatureFlagFreshDuration = longPreferencesKey(FEATURE_FLAG_FRESH_DURATION)
    private val prefsUseVerifier = booleanPreferencesKey(USE_VERIFIER)
    private val prefsPhotosUpsellPhotoCount = intPreferencesKey(PHOTOS_UPSELL_PHOTO_COUNT)
    val baseUrlFlow: Flow<String> = prefsKeyBaseUrl.asFlow(
        dataStore = context.dataStore,
        default = buildConfig.baseUrl
    )
    val hostFlow: Flow<String> = prefsKeyHost.asFlow(
        dataStore = context.dataStore,
        default = buildConfig.host
    )
    val appVersionHeaderFlow: Flow<String> = prefsKeyAppVersionHeader.asFlow(
        dataStore = context.dataStore,
        default = buildConfig.appVersionHeader
    )
    val useExceptionMessageFlow: Flow<Boolean> = prefsUseExceptionMessage.asFlow(
        dataStore = context.dataStore,
        default = buildConfig.useExceptionMessage,
    )
    val logToFileEnabledFlow: Flow<Boolean> = prefsLogToFileEnabled.asFlow(
        dataStore = context.dataStore,
        default = buildConfig.logToFileInDebugEnabled,
    )
    val allowBackupDeletedFilesEnabledFlow: Flow<Boolean> = prefsAllowBackupDeletedFilesEnabled.asFlow(
        dataStore = context.dataStore,
        default = buildConfig.allowBackupDeletedFilesEnabled,
    )
    val featureFlagFreshDurationFlow: Flow<Long> = prefsFeatureFlagFreshDuration.asFlow(
        dataStore = context.dataStore,
        default = buildConfig.featureFlagFreshDuration.inWholeMinutes,
    )
    val useVerifierFlow: Flow<Boolean> = prefsUseVerifier.asFlow(
        dataStore = context.dataStore,
        default = buildConfig.useVerifier,
    )

    override var baseUrl by Delegate(
        dataStore = context.dataStore,
        key = prefsKeyBaseUrl,
        default = buildConfig.baseUrl
    )
    override var host by Delegate(
        dataStore = context.dataStore,
        key = prefsKeyHost,
        default = buildConfig.host
    )
    override var appVersionHeader by Delegate(
        dataStore = context.dataStore,
        key = prefsKeyAppVersionHeader,
        default = buildConfig.appVersionHeader
    )
    override var useExceptionMessage by Delegate(
        dataStore = context.dataStore,
        key = prefsUseExceptionMessage,
        default = buildConfig.useExceptionMessage,
    )
    override var logToFileInDebugEnabled by Delegate(
        dataStore = context.dataStore,
        key = prefsLogToFileEnabled,
        default = buildConfig.logToFileInDebugEnabled,
    )
    override var allowBackupDeletedFilesEnabled by Delegate(
        dataStore = context.dataStore,
        key = prefsAllowBackupDeletedFilesEnabled,
        default = buildConfig.allowBackupDeletedFilesEnabled,
    )
    override var featureFlagFreshDuration: Duration
        get() = runBlocking {
            prefsFeatureFlagFreshDuration.asFlow(
                context.dataStore,
                buildConfig.featureFlagFreshDuration.inWholeMinutes,
            ).first().minutes
        }
        set(value) = runBlocking {
            context.dataStore.edit { preferences ->
                preferences[prefsFeatureFlagFreshDuration] = value.inWholeMinutes
            }
        }
    override var photosSavedCounter: Boolean = true
    override var photoExportData: Boolean = true
    override val disableFeatureFlagInDevelopment: Boolean = false
    override var useVerifier by Delegate(
        dataStore = context.dataStore,
        key = prefsUseVerifier,
        default = buildConfig.useVerifier,
    )
    override var photosUpsellPhotoCount by Delegate(
        dataStore = context.dataStore,
        key = prefsPhotosUpsellPhotoCount,
        default = buildConfig.photosUpsellPhotoCount,
    )
    override var albumsFeatureFlag: Boolean = true

    fun reset(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }

    companion object {
        const val DEBUG_SETTINGS_PREFERENCES = "debug_settings_prefs"
        const val BASE_URL = "base_url"
        const val HOST = "host"
        const val APP_VERSION_HEADER = "app_version_header"
        const val USE_EXCEPTION_MESSAGE = "use_exception_message"
        const val LOG_TO_FILE_ENABLED = "log_to_file_enabled"
        const val ALLOW_BACKUP_DELETED_FILES_ENABLED = "allow_backup_deleted_files_enabled"
        const val FEATURE_FLAG_FRESH_DURATION = "feature_flag_fresh_duration"
        const val USE_VERIFIER = "use_verifier"
        const val PHOTOS_UPSELL_PHOTO_COUNT = "photos_upsell_photo_count"
    }
}
