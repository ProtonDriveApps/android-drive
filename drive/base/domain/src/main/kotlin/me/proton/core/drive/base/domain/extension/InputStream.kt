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

package me.proton.core.drive.base.domain.extension

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.drive.base.domain.log.LogTag.UPLOAD
import me.proton.core.util.kotlin.CoreLogger
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Objects


suspend fun InputStream.getHexMessageDigest(
    algorithm: String,
): String? = withContext(Dispatchers.IO) {
    val messageDigest = try {
        MessageDigest.getInstance(algorithm)
    } catch (e: NoSuchAlgorithmException) {
        CoreLogger.i(UPLOAD, e, "Algorithm not supported")
        return@withContext null
    }
    DigestInputStream(this@getHexMessageDigest, messageDigest).use { stream ->
        stream.copyTo(NullOutputStream())
    }
    messageDigest.digest().toHex()
}

// Copy of OutputStream.nullOutputStream()
private class NullOutputStream : OutputStream() {

    @Volatile
    private var closed = false
    private fun ensureOpen() {
        if (closed) {
            throw IOException("Stream closed")
        }
    }

    override fun write(b: Int) {
        ensureOpen()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        Objects.checkFromIndexSize(off, len, b.size)
        ensureOpen()
    }

    override fun close() {
        closed = true
    }
}
