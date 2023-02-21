/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.base.data.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.StorageLocationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class StorageLocationProviderImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
) : StorageLocationProvider {

    override suspend fun getCacheFolder(userId: UserId, path: String): File = coRunCatching(coroutineContext) {
        with (userId.getFolder(appContext.cacheDir)) {
            if (path.isBlank()) {
                this
            } else {
                File(this, path).apply { mkdirs() }
            }
        }
    }.getOrThrow()

    override suspend fun getPermanentFolder(userId: UserId, path: String): File = coRunCatching(coroutineContext) {
        with (userId.getFolder(appContext.filesDir)) {
            if (path.isBlank()) {
                this
            } else {
                File(this, path).apply { mkdirs() }
            }
        }
    }.getOrThrow()

    override suspend fun getCacheTempFolder(userId: UserId): File = coRunCatching(coroutineContext) {
        userId.getTempFolder(appContext.cacheDir)
    }.getOrThrow()

    private fun UserId.getFolder(parent: File) = File(parent, id).apply { mkdirs() }
    private fun UserId.getTempFolder(parent: File) = File(parent, "$TEMP_FOLDER/$id/").apply { mkdirs() }

    companion object {
        const val TEMP_FOLDER = "tmp"
    }
}
