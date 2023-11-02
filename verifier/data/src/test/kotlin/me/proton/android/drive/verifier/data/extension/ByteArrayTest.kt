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

import me.proton.core.test.kotlin.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ByteArrayParameterizedTest(
    private val first: ByteArray,
    private val second: ByteArray,
    private val firstXorSecond: ByteArray,
) {

    @Test
    fun `xor of two byte arrays`() {
        assertTrue(
            first.xor(second).contentEquals(firstXorSecond)
        ) { "xor failed" }
    }

    companion object {
        @get:Parameterized.Parameters(name = "{0} xor {1} equals {2}")
        @get:JvmStatic
        val data = listOf(
            // Byte array with size 1
            arrayOf(
                byteArrayOf(0x00),
                byteArrayOf(0xFF.toByte()),
                byteArrayOf(0xFF.toByte()),
            ),
            arrayOf(
                byteArrayOf(0xF0.toByte()),
                byteArrayOf(0x0F.toByte()),
                byteArrayOf(0xFF.toByte()),
            ),
            // Byte array with size 2
            arrayOf(
                byteArrayOf(0x00, 0xFF.toByte()),
                byteArrayOf(0xFF.toByte(), 0x00),
                byteArrayOf(0xFF.toByte(), 0xFF.toByte()),
            ),
            arrayOf(
                byteArrayOf(0x00, 0x55.toByte()),
                byteArrayOf(0xAA.toByte(), 0x00),
                byteArrayOf(0xAA.toByte(), 0x55.toByte()),
            ),
            // Byte array with size 4
            arrayOf(
                byteArrayOf(0x05.toByte(), 0x06.toByte(), 0x07.toByte(), 0x08.toByte()),
                byteArrayOf(0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte(), 0xA6.toByte()),
                byteArrayOf(0xA6.toByte(), 0xA2.toByte(), 0xA2.toByte(), 0xAE.toByte()),
            ),
        )
    }
}

class ByteArrayTest {
    @Test(expected = IllegalArgumentException::class)
    fun `different size byte arrays cause IllegalArgumentException`() {
        byteArrayOf(0x00).xor(byteArrayOf(0x00, 0x00))
    }

    @Test
    fun `xor of two empty byte array result in empty byte array`() {
        assertTrue(
            ByteArray(0).xor(ByteArray(0)).contentEquals(ByteArray(0))
        ) { "xor failed" }
    }
}
