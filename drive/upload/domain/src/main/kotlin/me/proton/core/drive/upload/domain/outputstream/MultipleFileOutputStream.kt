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
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.util.kotlin.CoreLogger
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * Output stream that stores bytes into multiple files.
 * Once first file output stream has reached it's allowed size, second file is created and following bytes are stored
 * there and so on.
 */
class MultipleFileOutputStream(
    private val destinationFolder: File,
    private val blockMaxSize: Bytes,
    private val fileGenerator: FileGenerator = RandomFileGenerator(),
) : OutputStream() {
    private val _files = mutableListOf<FileWithOutputStream>()
    val files: List<File> get() = _files.map { fileWithOutputStream -> fileWithOutputStream.file }
    private lateinit var _current: FileWithOutputStream
    private val current: MaxSizeFileOutputStream
        get() {
            if (needsNewFile) { addNewFile() }
            return _current.outputStream
        }

    override fun write(b: ByteArray, off: Int, len: Int) {
        do {
            val overflow = current.writeBytes(b, off, len)
        } while (overflow > 0)
    }

    override fun write(b: ByteArray) = write(b, 0, b.size)

    override fun write(b: Int) {
        val overflow = current.writeByte(b)
        if (overflow > 0) {
            write(b)
        }
    }

    override fun flush() = _files.forEach { (_, outputStream) ->
        try {
            outputStream.flush()
        } catch (e: IOException) {
            CoreLogger.d(LogTag.UPLOAD, e, e.message ?: "Flush for output stream failed")
        }
    }

    override fun close() = _files.forEach { (_, outputStream) ->
        try {
            outputStream.close()
        } catch (e: IOException) {
            CoreLogger.d(LogTag.UPLOAD, e, e.message ?: "Close for output stream failed")
        }
    }

    private val needsNewFile: Boolean get() = ::_current.isInitialized.not() || _current.outputStream.isFull

    private fun addNewFile() {
        val newFile = fileGenerator.newFile(destinationFolder)
        if (::_current.isInitialized) {
            _current.outputStream.close()
        }
        _current = FileWithOutputStream(newFile, MaxSizeFileOutputStream(newFile,blockMaxSize))
        _files.add(_current)
    }

    private data class FileWithOutputStream(val file: File, val outputStream: MaxSizeFileOutputStream)

    interface FileGenerator {
        fun newFile(destinationFolder: File): File
    }

    private class RandomFileGenerator : FileGenerator {
        override fun newFile(destinationFolder: File): File =
            File(destinationFolder, UUID.randomUUID().toString()).apply {
                parentFile?.mkdirs()
                createNewFile()
            }
    }
}
