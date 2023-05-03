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

package me.proton.core.drive.link.presentation.extension

import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import io.mockk.mockk
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.entity.toTimestampS
import me.proton.core.drive.link.domain.entity.BaseLink
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class BaseLinkTest {

    private val now = 1_635_164_040_000L // October 25 2021, 14:14
    private var savedLocale = Locale.getDefault()
    private var savedTimeZone = TimeZone.getDefault()
    private val context get() = InstrumentationRegistry.getInstrumentation().context

    @Before
    fun setup() {
        savedLocale = Locale.getDefault()
        savedTimeZone = TimeZone.getDefault()
        Locale.setDefault(Locale.ENGLISH)
    }

    @After
    fun tearDown() {
        Locale.setDefault(savedLocale)
        TimeZone.setDefault(savedTimeZone)
    }

    @Test
    fun lastModifiedRelativeToSameDay() {
        // region Arrange
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+2"))
        val link = link(1_635_133_380_000L) // Mon Oct 25 2021 05:43:00
        // endregion
        // region Act
        val lastModified = link.lastModifiedRelative(context, now)
        // endregion
        // region Assert
        lastModified.assertEqual("5:43 AM")
        // endregion
    }

    @Test
    fun lastModifiedRelativeToNextDay() {
        // region Arrange
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+2"))
        val link = link(1_635_089_580_000L) // Sun Oct 24 2021 17:33:00
        // endregion
        // region Act
        val lastModified = link.lastModifiedRelative(context, now)
        // endregion
        // region Assert
        lastModified.assertEqual("Oct 24")
        // endregion
    }

    @Test
    fun lastModifiedRelativeToSameYear() {
        // region Arrange
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+1"))
        val link = link(1_609_487_760_000L) // Fri Jan 01 2021 08:56:00
        // endregion
        // region Act
        val lastModified = link.lastModifiedRelative(context, now)
        // endregion
        // region Assert
        lastModified.assertEqual("Jan 1")
        // endregion
    }

    @Test
    fun lastModifiedRelativeToNextYear() {
        // region Arrange
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+1"))
        val link = link(1_609_455_599_000L) // Thu Dec 31 2020 23:59:59
        // endregion
        // region Act
        val lastModified = link.lastModifiedRelative(context, now)
        // endregion
        // region Assert
        lastModified.assertEqual("Dec 31, 2020")
        // endregion
    }

    private fun CharSequence.assertEqual(other: String) =
        assert(this == other) { "'$this' should be equal to '$other'" }

    private fun link(timestampMs: Long) = mockk<BaseLink>().apply {
        every { lastModified } returns TimestampMs(timestampMs).toTimestampS()
    }
}
