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

package me.proton.android.drive.usecase

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.drive.upload.data.resolver.AggregatedUriResolver
import javax.inject.Inject

class ValidateExternalUri @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val aggregatedUriResolver: AggregatedUriResolver,
) {
    operator fun invoke(uri: Uri): Boolean = uri.isValid

    fun List<Uri>.validate(): List<Uri> = filter { uri -> uri.isValid }

    private val Uri.isValid: Boolean get() =
        when (scheme) {
            SCHEME_FILE -> {
                val file = toFile()
                val canonicalPath = file.canonicalPath
                canonicalPath.equals(file.path) &&
                        !canonicalPath.startsWith("/data") &&
                        !canonicalPath.contains(appContext.packageName) &&
                        aggregatedUriResolver.schemes.contains(scheme)
            }
            else -> aggregatedUriResolver.schemes.contains(scheme)
        }
}
