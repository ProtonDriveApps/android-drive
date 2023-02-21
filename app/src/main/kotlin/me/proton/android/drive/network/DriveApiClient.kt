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

package me.proton.android.drive.network

import android.os.Build
import me.proton.android.drive.BuildConfig
import me.proton.android.drive.usecase.OnForceUpdate
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.network.domain.ApiClient
import javax.inject.Inject

class DriveApiClient @Inject constructor(
    private val onForceUpdate: OnForceUpdate,
    private val configurationProvider: ConfigurationProvider,
) : ApiClient {
    override val appVersionHeader: String
        get() = configurationProvider.appVersionHeader
    override val enableDebugLogging: Boolean
        get() = true
    override val shouldUseDoh: Boolean
        get() = false
    override val userAgent: String
        get() = StringBuilder()
            .append("ProtonDrive/${BuildConfig.VERSION_NAME} ")
            .append("(")
            .append("Android ${Build.VERSION.RELEASE}; ")
            .append("${Build.BRAND} ${Build.MODEL})")
            .toString()

    override fun forceUpdate(errorMessage: String) = onForceUpdate(errorMessage)

    override val writeTimeoutSeconds: Long get() = 90L
    override val callTimeoutSeconds: Long get() = 90L
}
