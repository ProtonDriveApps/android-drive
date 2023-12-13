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

package me.proton.core.drive.upload.domain.extension

import me.proton.core.drive.base.domain.entity.Bytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile

class SaveToBlocksTest {
    private val blockSize = Bytes(16 * 1024)

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun empty() {
        val blocks = split(0L)
        assertEquals(0, blocks.size)
        blocks.assertNoneEmpty()
    }

    @Test
    fun one() {
        val blocks = split(1L)
        assertEquals(1, blocks.size)
        blocks.assertNoneEmpty()
    }

    @Test
    fun block_size() {
        val blocks = split(blockSize.value)
        assertEquals(1, blocks.size)
        blocks.assertNoneEmpty()
    }

    @Test
    fun block_size_plus_one() {
        val blocks = split(blockSize.value + 1)
        assertEquals(2, blocks.size)
        blocks.assertNoneEmpty()
    }

    @Test
    fun double_block_size() {
        val blocks = split(blockSize.value * 2)
        assertEquals(2, blocks.size)
        blocks.assertNoneEmpty()
    }

    @Test
    fun ten_thousand_block_size() {
        // test Too many open files
        val blocks = split(blockSize.value * 10000)
        assertEquals(10000, blocks.size)
        blocks.assertNoneEmpty()
    }

    private fun List<File>.assertNoneEmpty() = forEach { block ->
        assertNotEquals("${block.name} is empty", 0, block.length())
    }

    private fun split(size: Long): List<File> {
        val source = folder.newFolder()
        val output = folder.newFolder()

        val blocks = createFile(source, "file_$size", size).inputStream().use {
            it.saveToBlocks(File(output, "split_$size"), blockSize)
        }
        return blocks
    }

    private fun createFile(folder: File, name: String, size: Long) = File(folder, name).apply {
        createNewFile()
        if (size > 0L) {
            RandomAccessFile(this, "rw").use { it.setLength(size) }
        }
    }

}
