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

import android.net.Uri
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.DriveUrlBuilder
import javax.inject.Inject

class DriveUrlBuilderImpl @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
) : DriveUrlBuilder {

    override fun invoke(builder: Uri.Builder.() -> Unit): String {
        return Uri.Builder()
            .scheme("https")
            .authority("drive.${configurationProvider.host}")
            .apply { builder() }
            .build()
            .toString()
    }
}
