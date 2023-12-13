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

import android.content.ClipData
import android.content.ClipData.Item
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import me.proton.core.drive.base.domain.provider.StorageLocationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.upload.domain.resolver.UriResolver
import javax.inject.Inject

class SendDebugLog @Inject constructor(
    private val storageLocationProvider: StorageLocationProvider,
    private val getUriForFile: GetUriForFile,
    private val uriResolver: UriResolver,
) {
    suspend operator fun invoke(context: Context): Result<Unit> = coRunCatching {
        val logFolder = storageLocationProvider.getDebugLogFolder()
        val debugLogUris = logFolder.listFiles().orEmpty()
            .filter { file -> file.name.endsWith(".log") }
            .map { debugLog -> getUriForFile(debugLog).getOrThrow() }
        require(debugLogUris.isNotEmpty()) { "Debug log file does not exist" }
        context.startActivity(
            Intent.createChooser(
                Intent().apply {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(debugLogUris))
                    type = uriResolver.getMimeType(debugLogUris.first().toString())
                    clipData = ClipData(
                        ClipDescription(
                            "logs",
                            debugLogUris.map { debugLogUri ->
                                uriResolver.getMimeType(debugLogUri.toString())
                            }.toTypedArray(),
                        ),
                        Item(debugLogUris.first()),
                    ).apply {
                        (debugLogUris - debugLogUris.first()).forEach { uri ->
                            addItem(Item(uri))
                        }
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Send log ${debugLogUris.size} files",
            )
        )
    }
}
