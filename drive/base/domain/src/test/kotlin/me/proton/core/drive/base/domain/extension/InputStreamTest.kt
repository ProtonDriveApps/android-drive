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

package me.proton.core.drive.base.domain.extension

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InputStreamTest {

    @Test
    fun sha1() = runTest {
        val hexMessageDigest = "".byteInputStream().getHexMessageDigest("SHA-1")

        assertEquals(sha1_empty, hexMessageDigest)
    }

    @Test
    fun unsupported() = runTest {
        val hexMessageDigest = "".byteInputStream().getHexMessageDigest("unsupported")

        assertNull(hexMessageDigest)
    }

}



private const val sha1_empty = "da39a3ee5e6b4b0d3255bfef95601890afd80709"
