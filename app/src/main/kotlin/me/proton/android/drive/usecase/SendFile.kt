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

import android.content.Context
import android.content.Intent
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.upload.domain.resolver.UriResolver
import java.io.File
import javax.inject.Inject

class SendFile @Inject constructor(
    private val getUriForFile: GetUriForFile,
    private val uriResolver: UriResolver,
) {
    suspend operator fun invoke(context: Context, file: File): Result<Unit> = coRunCatching {
        val uri = getUriForFile(file).getOrThrow()
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND)
                    .setDataAndType(uri, uriResolver.getMimeType(uri.toString()))
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                file.name,
            )
        )
    }
}
