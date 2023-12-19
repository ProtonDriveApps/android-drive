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
package me.proton.core.drive.base.presentation.extension

import android.content.Context
import io.mockk.mockkClass
import me.proton.core.drive.base.domain.entity.Bytes
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

@RunWith(Parameterized::class)
class Base2IECTest(
    private val bytes: Long,
    private val humanReadable: String,
) {
    private val context: Context = mockkClass(Context::class)

    @Test
    fun asHumanReadableString() {
        // region When
        val humanReadableString = Bytes(bytes).asHumanReadableString(context, SizeUnits.BASE2_IEC, Locale.US)
        // endregion
        // region Then
        assert(humanReadableString == humanReadable) { "Expected $humanReadable but actual $humanReadableString" }
        // endregion
    }

    companion object {
        @get:Parameterized.Parameters(name = "{0} should be mapped to {1}")
        @get:JvmStatic
        val data = listOf(
            arrayOf(                        0L, "0 B"),
            arrayOf(                     1023L, "1023 B"),
            arrayOf(                     1024L, "1.00 KiB"),
            arrayOf(                1_000_000L, "976.56 KiB"),
            arrayOf(                1_024_000L, "1000.00 KiB"),
            arrayOf(                1_048_576L, "1.00 MiB"),
            arrayOf(              500_000_000L, "476.84 MiB"),
            arrayOf(              524_288_000L, "500.00 MiB"),
            arrayOf(            1_000_000_000L, "953.67 MiB"),
            arrayOf(            1_048_576_000L, "1000.00 MiB"),
            arrayOf(            1_073_741_824L, "1.00 GiB"),
            arrayOf(        1_000_000_000_000L, "931.32 GiB"),
            arrayOf(        1_073_741_824_000L, "1000.00 GiB"),
            arrayOf(        1_099_511_627_776L, "1.00 TiB"),
            arrayOf(    1_000_000_000_000_000L, "909.49 TiB"),
            arrayOf(    1_099_511_627_776_000L, "1000.00 TiB"),
            arrayOf(    1_125_899_906_842_624L, "1.00 PiB"),
            arrayOf(1_000_000_000_000_000_000L, "888.18 PiB"),
            arrayOf(1_125_899_906_842_624_000L, "1000.00 PiB"),
        )
    }
}

@RunWith(Parameterized::class)
class Base2LegacyTest(
    private val bytes: Long,
    private val humanReadable: String,
) {
    private val context: Context = mockkClass(Context::class)

    @Test
    fun asHumanReadableString() {
        // region When
        val humanReadableString = Bytes(bytes).asHumanReadableString(context, SizeUnits.BASE2_LEGACY, Locale.US)
        // endregion
        // region Then
        assert(humanReadableString == humanReadable) { "Expected $humanReadable but actual $humanReadableString" }
        // endregion
    }

    companion object {
        @get:Parameterized.Parameters(name = "{0} should be mapped to {1}")
        @get:JvmStatic
        val data = listOf(
            arrayOf(                        0L, "0 B"),
            arrayOf(                     1023L, "1023 B"),
            arrayOf(                     1024L, "1.00 KB"),
            arrayOf(                1_000_000L, "976.56 KB"),
            arrayOf(                1_024_000L, "1000.00 KB"),
            arrayOf(                1_048_576L, "1.00 MB"),
            arrayOf(              500_000_000L, "476.84 MB"),
            arrayOf(              524_288_000L, "500.00 MB"),
            arrayOf(            1_000_000_000L, "953.67 MB"),
            arrayOf(            1_048_576_000L, "1000.00 MB"),
            arrayOf(            1_073_741_824L, "1.00 GB"),
            arrayOf(        1_000_000_000_000L, "931.32 GB"),
            arrayOf(        1_073_741_824_000L, "1000.00 GB"),
            arrayOf(        1_099_511_627_776L, "1.00 TB"),
            arrayOf(    1_000_000_000_000_000L, "909.49 TB"),
            arrayOf(    1_099_511_627_776_000L, "1000.00 TB"),
            arrayOf(    1_125_899_906_842_624L, "1.00 PB"),
            arrayOf(1_000_000_000_000_000_000L, "888.18 PB"),
            arrayOf(1_125_899_906_842_624_000L, "1000.00 PB"),
        )
    }
}
