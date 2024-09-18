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
package me.proton.core.drive.upload.domain.extension

import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.upload.domain.outputstream.MultipleFileOutputStream
import me.proton.core.util.kotlin.CoreLogger
import java.io.File
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

fun InputStream.saveToBlocks(
    destinationFolder: File,
    blockMaxSize: Bytes,
    listener: MultipleFileOutputStream.Listener? = null,
): List<File> =
    MultipleFileOutputStream(destinationFolder, blockMaxSize, listener)
        .apply { use { outputStream -> copyTo(outputStream) } }
        .files

internal fun InputStream.injectMessageDigests(algorithms: List<String>): Pair<InputStream, List<MessageDigest>> {
    val messageDigests = algorithms.mapNotNull { algorithm ->
        try {
            MessageDigest.getInstance(algorithm)
        } catch (e: NoSuchAlgorithmException) {
            CoreLogger.i(LogTag.UPLOAD, e, "Algorithm not supported")
            null
        }
    }
    val digestsInputStream = messageDigests.fold(this) { acc, messageDigest ->
        DigestInputStream(acc, messageDigest)
    }
    return digestsInputStream to messageDigests
}
