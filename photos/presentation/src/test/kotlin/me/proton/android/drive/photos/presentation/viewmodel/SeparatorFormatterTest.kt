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

package me.proton.android.drive.photos.presentation.viewmodel

import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import me.proton.core.drive.base.domain.entity.TimestampS
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.concurrent.TimeUnit
import me.proton.core.drive.i18n.R as I18N

class SeparatorFormatterTest {
    private val clock = { 1680533650000L }

    private lateinit var presenter: SeparatorFormatter

    @Before
    fun setUp() {
        val resources = mockk<Resources>()
        every { resources.getString(I18N.string.photos_separator_current_month) } returns "This month"
        presenter = SeparatorFormatter(
            resources = resources, 
            clock = clock, 
            locale = Locale.US,
        )
    }

    @Test
    fun `this month`() {
        assertEquals(
            "This month",
            presenter.toSeparator(
                TimestampS(
                    TimeUnit.MILLISECONDS.toSeconds(clock())
                            - TimeUnit.DAYS.toSeconds(1)
                )
            )
        )
    }

    @Test
    fun `month this year`() {
        assertEquals(
            "March",
            presenter.toSeparator(
                TimestampS(
                    TimeUnit.MILLISECONDS.toSeconds(clock())
                            - TimeUnit.DAYS.toSeconds(30)
                )
            )
        )
    }

    @Test
    fun `month in past year`() {
        assertEquals(
            "April 2022",
            presenter.toSeparator(
                TimestampS(
                    TimeUnit.MILLISECONDS.toSeconds(clock())
                            - TimeUnit.DAYS.toSeconds(365)
                )
            )
        )
    }
}
