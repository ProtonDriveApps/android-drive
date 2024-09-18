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
package me.proton.core.drive.file.base.domain.extension

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.CoroutineContext

suspend fun File.verifyOrDelete(
    hashSha256: String?,
    coroutineContext: CoroutineContext = Job() + Dispatchers.IO
): Boolean = withContext(coroutineContext) {
    if (hashSha256 == null || sha256(coroutineContext) == hashSha256) {
        return@withContext true
    }
    delete()
    false
}

suspend fun File.sha256(coroutineContext: CoroutineContext = Dispatchers.IO): String =
    withContext(coroutineContext) {
        Base64.encodeToString(sha256, Base64.NO_WRAP)
    }

val File.sha256: ByteArray
    get() {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { fileInputStream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytes = fileInputStream.read(buffer)
            while (bytes > 0) {
                digest.update(buffer, 0, bytes)
                bytes = fileInputStream.read(buffer)
            }
        }
        return digest.digest()
    }

fun File.moveTo(target: File) {
    if (!target.exists()) {
        target.mkdirs()
    }
    copyRecursively(target, true)
    deleteRecursively()
}
