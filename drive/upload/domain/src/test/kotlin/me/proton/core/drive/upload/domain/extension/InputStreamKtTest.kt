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

import me.proton.core.drive.base.domain.extension.toHex
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.MessageDigest


@RunWith(RobolectricTestRunner::class)
class InputStreamKtTest {

    @Test
    fun injectDigests_sha1() {
        val (inputStream, digests) = "".byteInputStream().injectMessageDigests(listOf("SHA1"))

        inputStream.readAllBytes()

        assertEquals(sha1_empty, digests.first().digest().toHex())
    }

    @Test
    fun injectDigests_unsupported() {
        val (_, digests) = "".byteInputStream().injectMessageDigests(listOf("unsupported"))

        assertEquals(emptyList<MessageDigest>(), digests)
    }
}

private const val sha1_empty = "da39a3ee5e6b4b0d3255bfef95601890afd80709"
