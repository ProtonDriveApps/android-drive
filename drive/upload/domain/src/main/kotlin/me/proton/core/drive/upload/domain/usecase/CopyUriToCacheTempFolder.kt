/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.upload.domain.usecase

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.GetCacheTempFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.upload.domain.resolver.UriResolver
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class CopyUriToCacheTempFolder @Inject constructor(
    private val uriResolver: UriResolver,
    private val getCacheTempFolder: GetCacheTempFolder,
) {

    suspend operator fun invoke(
        userId: UserId,
        uriString: String,
        fileName: String?,
        coroutineContext: CoroutineContext = Dispatchers.IO,
    ): Result<Uri> = coRunCatching(coroutineContext) {
        val name = fileName ?: uriResolver.getName(uriString) ?: UUID.randomUUID().toString()
        val lastModified = uriResolver.getLastModified(uriString)
        val destination = File(getCacheTempFolder(userId, coroutineContext), name)
            .apply {
                if (!exists()) {
                    createNewFile()
                } else {
                    throw FileAlreadyExistsException(this)
                }
            }
        uriResolver.useInputStream(uriString) { inputStream ->
            destination.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        lastModified?.let { timestamp ->
            destination.setLastModified(timestamp.value)
        }
        Uri.fromFile(destination)
    }
}
