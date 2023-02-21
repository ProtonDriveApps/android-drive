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

package me.proton.core.drive.files.preview.presentation.component

import org.junit.Test

import org.junit.Assert.*

class TextPreviewTest {

    @Test
    fun `given empty input when readTextLines should returns an empty list`() {
        val content = "".byteInputStream().readTextLines()

        assertEquals(emptyList<String>(), content)
    }

    @Test
    fun `given one line when readTextLines should returns a list`() {
        val content = "first line".byteInputStream().readTextLines()

        assertEquals(listOf("first line"), content)
    }

    @Test
    fun `given two lines when readTextLines should returns a list`() {
        val content = """first line
            |second line
        """.trimMargin().byteInputStream().readTextLines()

        assertEquals(listOf("first line", "second line"), content)
    }

    @Test
    fun `given empty lines when readTextLines should returns a list including empty lines`() {
        val content = """first line
            |
            |second line
        """.trimMargin().byteInputStream().readTextLines()

        assertEquals(listOf("first line", "", "second line"), content)
    }
}