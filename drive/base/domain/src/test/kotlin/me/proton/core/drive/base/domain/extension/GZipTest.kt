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
package me.proton.core.drive.base.domain.extension

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException

@RunWith(Parameterized::class)
class GZipTest(
    private val text: String,
) {

    @Test
    fun `test compress and decompress`() {
        assert(
            text == text.gzipCompress().gzipDecompress().getOrThrow()
        ) {
            "GZIP compress and then decompress did not result in original string"
        }
    }

    @Test(expected = IOException::class)
    fun `test decompress of non gzip byte array`() {
        text.toByteArray(Charsets.UTF_8).gzipDecompress().getOrThrow()
    }

    companion object {
        @get:Parameterized.Parameters(name = "Used string: {0}")
        @get:JvmStatic
        val data = listOf(
            "Proton Drive secure cloud storage",
            "",
            "{ \"Group\" : { \"Field\" : 123 } }",
        )
    }
}
