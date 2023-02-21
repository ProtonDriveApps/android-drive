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
package me.proton.core.drive.cryptobase.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ValidatePassphraseFormatTest(
    private val passphrase: ByteArray,
    private val expected: Any,
) {
    private val validatePassphraseFormat = ValidatePassphraseFormat()

    @Test
    fun `test valid and invalid passphrase format`() {
        assertEquals(expected, validatePassphraseFormat(passphrase))
    }

    companion object {

        @get:Parameterized.Parameters
        @get:JvmStatic
        @Suppress("unused")
        val data = listOf(
            // region invalid passphrase format
            //64 bytes where first is not hex digit -> "g70ca6f14a0d784d30080ef9726362abdc6542d5c94fa87a4c524cf0d4e9be30"
            arrayOf(
                byteArrayOf(103, 54, 54, 99, 98, 49, 50, 56, 99, 49, 99, 51, 52, 56, 98, 101, 54, 97, 50, 101, 50, 56, 55, 99, 97, 49, 101, 102, 51, 51, 56, 51, 56, 57, 54, 48, 102, 99, 57, 99, 56, 98, 102, 52, 98, 101, 97, 48, 99, 54, 99, 51, 48, 48, 48, 55, 57, 52, 102, 57, 101, 102, 49, 57),
                false
            ),
            //44 bytes where first is not Base64 char -> "-KZV+29md1Hd7pUkyWPy29GhYkbE8hjpyXLGESDEuW4="
            arrayOf(
                byteArrayOf(45, 75, 90, 86, 43, 50, 57, 109, 100, 49, 72, 100, 55, 112, 85, 107, 121, 87, 80, 121, 50, 57, 71, 104, 89, 107, 98, 69, 56, 104, 106, 112, 121, 88, 76, 71, 69, 83, 68, 69, 117, 87, 52, 61),
                false
            ),
            //33 bytes
            arrayOf(
                byteArrayOf(0, 127, 1, 90, 86, 43, 50, 57, 109, 100, 49, 72, 100, 55, 112, 85, 107, 121, 87, 80, 121, 50, 57, 71, 104, 89, 107, 98, 69, 56, 104, 106, 112),
                false
            ),
            //31 bytes
            arrayOf(
                byteArrayOf(90, 86, 43, 50, 57, 109, 100, 49, 72, 100, 55, 112, 85, 107, 121, 87, 80, 121, 50, 57, 71, 104, 89, 107, 98, 69, 56, 104, 106, 112),
                false
            ),
            // endregion
            // region valid passphrase format
            //64 bytes where all are hex digits -> "a70ca6f14a0d784d30080ef9726362abdc6542d5c94fa87a4c524cf0d4e9be30"
            arrayOf(
                byteArrayOf(54, 54, 54, 99, 98, 49, 50, 56, 99, 49, 99, 51, 52, 56, 98, 101, 54, 97, 50, 101, 50, 56, 55, 99, 97, 49, 101, 102, 51, 51, 56, 51, 56, 57, 54, 48, 102, 99, 57, 99, 56, 98, 102, 52, 98, 101, 97, 48, 99, 54, 99, 51, 48, 48, 48, 55, 57, 52, 102, 57, 101, 102, 49, 57),
                true
            ),
            //44 bytes where all are Base64 chars -> "mKZV+29md1Hd7pUkyWPy29GhYkbE8hjpyXLGESDEuW4="
            arrayOf(
                byteArrayOf(109, 75, 90, 86, 43, 50, 57, 109, 100, 49, 72, 100, 55, 112, 85, 107, 121, 87, 80, 121, 50, 57, 71, 104, 89, 107, 98, 69, 56, 104, 106, 112, 121, 88, 76, 71, 69, 83, 68, 69, 117, 87, 52, 61),
                true
            ),
            //32 bytes
            arrayOf(
                byteArrayOf(0, 127, 90, 86, 43, 50, 57, 109, 100, 49, 72, 100, 55, 112, 85, 107, 121, 87, 80, 121, 50, 57, 71, 104, 89, 107, 98, 69, 56, 104, 106, 112),
                true
            )
            // endregion
        )
    }
}
