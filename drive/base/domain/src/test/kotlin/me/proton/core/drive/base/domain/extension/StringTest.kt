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
package me.proton.core.drive.base.domain.extension

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FileNameTest(
    private val name: String,
    private val generated: String
) {

    @Test
    fun `test updating file name to avoid duplicates`() {
        val fileName = name
            .trimForbiddenChars()
            .avoidDuplicateFileName()
        assert(fileName == generated) { "Expected $generated but got $fileName" }
    }

    companion object {
        @get:Parameterized.Parameters(name = "{0} should generate {1}")
        @get:JvmStatic
        val data = listOf(
            arrayOf("simple.jpg", "simple (1).jpg"),
            arrayOf("\u0000simple\u0001.\u001Ejpg\u001F", "simple (1).jpg"),
            arrayOf("\u0020simple.jpg\u1FFF", "\u0020simple (1).jpg\u1FFF"),
            arrayOf("\u2000simple\u2001.\u200Ejpg\u200F", "simple (1).jpg"),
            arrayOf("\u2010simple.jpg\u202D", "\u2010simple (1).jpg\u202D"),
            arrayOf("\u202Esimple.jpg\u202F", "simple (1).jpg"),
            arrayOf("simple", "simple (1)"),
            arrayOf("simple.", "simple (1)."),
            arrayOf("simple  (1)  .txt", "simple (2).txt"),
            arrayOf("simple (19).jpg", "simple (20).jpg"),
            arrayOf("simple (99).jpg", "simple (100).jpg"),
            arrayOf("simple (103)", "simple (104)"),
            arrayOf("simple (1).jpg", "simple (2).jpg"),
            arrayOf("simple (a).jpg", "simple (a) (1).jpg"),
            arrayOf("simple (1a).jpg", "simple (1a) (1).jpg"),
            arrayOf("file.with.dots.in.name.jpg", "file.with.dots.in.name (1).jpg"),
            arrayOf("file.with.dots.in.name(1).jpg", "file.with.dots.in.name (2).jpg"),
            arrayOf("file without extension", "file without extension (1)"),
            arrayOf("file without extension(1)", "file without extension (2)"),
            arrayOf("file with two suffix (2)(4)", "file with two suffix (2) (5)"),
            arrayOf("file with two suffix with extension (2)(4).txt", "file with two suffix with extension (2) (5).txt"),
            arrayOf("file with two suffix with extension (2)().txt", "file with two suffix with extension (2)() (1).txt"),
        )
    }
}

class EllipsizeConditionTest {

    @Test
    fun `string length is shorter or equal to max length`() {
        assert(LENGTH_9.ellipsizeMiddle(10) == LENGTH_9)
        assert(LENGTH_10.ellipsizeMiddle(10) == LENGTH_10)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ellipsize length must be less than half of max length`() {
        LENGTH_10.ellipsizeMiddle(6, "...")
    }

    companion object {
        private const val LENGTH_9 = "abcdefghi"
        private const val LENGTH_10 = "1234567890"
    }
}

@RunWith(Parameterized::class)
class EllipsizeTest(
    private val original: String,
    private val ellipsized: String,
    private val maxLength: Int,
    private val ellipsize: String
) {

    @Test
    fun `test ellipsize`() {
        val result = original.ellipsizeMiddle(maxLength, ellipsize)
        assert(result.length == maxLength)
        assert(result == ellipsized)
    }

    companion object {
        @get:Parameterized.Parameters(name = "{0} should ellipsis {1}")
        @get:JvmStatic
        val data = listOf(
            arrayOf("1234567890", "12...90", 7, "..."),
            arrayOf("123456789", "12...89", 7, "..."),
            arrayOf("abcdefghij", "ab...hij", 8, "..."),
            arrayOf("abcdefghi", "ab...ghi", 8, "..."),
            arrayOf("Proton Drive cloud service manual.pdf", "Prot…l.pdf", 10, "…"),
        )
    }
}
