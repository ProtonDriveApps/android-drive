/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.test.network

import android.os.Build
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.network.domain.ApiClient
import javax.inject.Inject

class TestDriveApiClient @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
) : ApiClient {
    override val appVersionHeader: String
        get() = configurationProvider.appVersionHeader
    override val enableDebugLogging: Boolean
        get() = true
    override suspend fun shouldUseDoh(): Boolean = false
    override val userAgent: String
        get() = StringBuilder()
            .append("ProtonDrive/test ")
            .append("(")
            .append("Android ${Build.VERSION.RELEASE}; ")
            .append("${Build.BRAND} ${Build.MODEL})")
            .toString()

    override fun forceUpdate(errorMessage: String) {}

    override val writeTimeoutSeconds: Long get() = 90L
    override val callTimeoutSeconds: Long get() = 90L
}
