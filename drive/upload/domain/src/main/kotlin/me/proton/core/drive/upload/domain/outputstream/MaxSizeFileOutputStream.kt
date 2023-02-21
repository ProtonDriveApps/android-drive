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
package me.proton.core.drive.upload.domain.outputstream

import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.minOf
import me.proton.core.drive.base.domain.extension.bytes
import java.io.File
import java.io.FileOutputStream
import java.lang.IllegalStateException

@Suppress("unused")
class MaxSizeFileOutputStream(file: File, private val maxSize: Bytes) : FileOutputStream(file) {
    private var bytesCopied: Bytes = 0.bytes

    /**
     * Writes bytes into file output stream
     * @return 0 if all bytes were written into file output stream, otherwise the number of bytes that overflows max allowed size
     */
    fun writeBytes(b: ByteArray, off: Int, len: Int): Int {
        val available = minOf(len, maxSize - bytesCopied).toInt()
        write(b, off, len)
        return len - available
    }

    /**
     * See [writeBytes]
     */
    fun writeBytes(b: ByteArray): Int = writeBytes(b, 0, b.size)

    /**
     * Writes byte into file output stream
     * @return 0 if byte was written into file output stream, 1 if file output stream reached max allowed size
     */
    fun writeByte(b: Int): Int {
        return if (bytesCopied < maxSize) {
            write(b)
            0
        } else {
            1
        }
    }

    val isFull: Boolean get() = bytesCopied >= maxSize

    override fun write(b: ByteArray, off: Int, len: Int) {
        val available = minOf(len, maxSize - bytesCopied)
        super.write(b, off, available.toInt())
        bytesCopied += available.bytes
    }

    override fun write(b: ByteArray) = write(b, 0, b.size)

    override fun write(b: Int) {
        if (bytesCopied < maxSize) {
            super.write(b)
            bytesCopied++
        } else {
            throw IllegalStateException("File output stream reached max size and cannot write additional byte")
        }
    }
}
