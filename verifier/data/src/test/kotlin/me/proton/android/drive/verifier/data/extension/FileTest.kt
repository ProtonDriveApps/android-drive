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

package me.proton.android.drive.verifier.data.extension

import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.size
import me.proton.core.test.kotlin.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FileTest(
    private val fileSize: Int,
) {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `head on different file size`() {
        // Given
        val fileSizeInBytes = fileSize.bytes
        val file = temporaryFolder.createFile(fileSizeInBytes)
        check(file.exists()) { "File does not exist" }
        check(file.size == fileSizeInBytes) {
            "File size mismatch expected: $fileSizeInBytes, actual: ${file.size}"
        }

        // When
        val head = file.head(TARGET_HEAD_SIZE.bytes)

        // Then
        assertEquals(TARGET_HEAD_SIZE, head.size) {
            "Head size mismatch"
        }
        for (i in 0 until minOf(fileSize, TARGET_HEAD_SIZE)) {
            assertEquals('a'.code, head[i].toInt()) { "Byte mismatch" }
        }
        for (i in fileSize until TARGET_HEAD_SIZE) {
            assertEquals(0, head[i].toInt()) { "Byte mismatch" }
        }
    }

    companion object {
        private const val TARGET_HEAD_SIZE = 32

        @get:Parameterized.Parameters
        @get:JvmStatic
        val data = listOf(
            arrayOf(0),
            arrayOf(1),
            arrayOf(17),
            arrayOf(33),
        )
    }
}
