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

import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.drive.file.base.domain.exception.CancelledException
import java.io.File
import java.io.InputStream

@Suppress("BlockingMethodInNonBlockingContext")
@Throws(CancelledException::class)
internal fun InputStream.saveToFile(
    file: File,
    mutableStateFlow: MutableStateFlow<Long>,
    isCancelled: () -> Boolean,
) = use {
    require(!file.isDirectory) { "Directories can not be used to save stream" }
    file.apply { parentFile?.mkdirs() }.outputStream().use { outputStream ->
        val buffer = ByteArray(16_384) // 16KiB
        var totalBytes = 0L
        var bytes = read(buffer)
        while (bytes >= 0) {
            outputStream.write(buffer, 0, bytes)
            totalBytes += bytes
            mutableStateFlow.value = totalBytes
            bytes = read(buffer)
            if (isCancelled()) {
                throw CancelledException()
            }
        }
        outputStream.flush()
    }
}
