/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.base.presentation.extension

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ColorKtTest(
    private val fgColor: Int,
    private val alpha: Float,
    private val bgColor: Int,
    private val expected: Int,
) {
    private fun assertColorEquals(expected: Color, actual: Color, delta: Float = 0.01f) {
        assertEquals(expected.red, actual.red, delta)
        assertEquals(expected.green, actual.green, delta)
        assertEquals(expected.blue, actual.blue, delta)
        assertEquals(expected.alpha, actual.alpha, delta)
    }

    @Test
    fun testBlendToSolid() {
        val actual = Color(fgColor).blendToSolid(
            alpha = alpha,
            bg = Color(bgColor),
        )
        assertColorEquals(Color(expected), actual)
    }

    companion object {
        @get:Parameterized.Parameters(name = "argb({0}).blendToSolid(alpha={1}, bgColor=argb({2})) = argb({3})")
        @get:JvmStatic
        val data = listOf(
            arrayOf(Color(0xFF007B58).toArgb(), 0.3f, Color.White.toArgb(), Color(0xFFB2D7CD).toArgb()),
            arrayOf(Color(0xFFFF5733).toArgb(), 0.5f, Color.Black.toArgb(), Color(0xFF7F2B19).toArgb()),
            arrayOf(Color(0xFF123456).toArgb(), 0.7f, Color.White.toArgb(), Color(0xFF597189).toArgb()),
            arrayOf(Color(0xFFABCDEF).toArgb(), 0.2f, Color.Black.toArgb(), Color(0xFF222930).toArgb()),
            arrayOf(Color(0xFF800080).toArgb(), 0.4f, Color(0xFF00FF00).toArgb(), Color(0xFF339933).toArgb()),
        )
    }
}
