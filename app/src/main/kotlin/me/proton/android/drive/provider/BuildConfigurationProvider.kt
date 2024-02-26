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

package me.proton.android.drive.provider

import me.proton.android.drive.BuildConfig
import me.proton.core.configuration.EnvironmentConfiguration
import me.proton.core.drive.base.domain.provider.ConfigurationProvider

class BuildConfigurationProvider(envConfig: EnvironmentConfiguration) : ConfigurationProvider {
    override val host: String = envConfig.host
    override val baseUrl: String = envConfig.baseUrl
    override val appVersionHeader: String = when (BuildConfig.FLAVOR) {
        BuildConfig.FLAVOR_DEVELOPMENT -> "${BuildConfig.APP_VERSION_HEADER}-dev+${BuildConfig.GIT_HASH}"
        BuildConfig.FLAVOR_ALPHA -> "${BuildConfig.APP_VERSION_HEADER}-alpha.%02d".format(BuildConfig.VERSION_CODE)
        BuildConfig.FLAVOR_BETA -> "${BuildConfig.APP_VERSION_HEADER}-beta"
        else -> BuildConfig.APP_VERSION_HEADER
    }
}
