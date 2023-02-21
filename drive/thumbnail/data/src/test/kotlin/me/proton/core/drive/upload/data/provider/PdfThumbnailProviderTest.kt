/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.presentation.thumbnail

import android.graphics.pdf.PdfRenderer
import io.mockk.every
import io.mockk.mockk
import me.proton.core.drive.thumbnail.data.provider.PdfThumbnailProvider.Companion.computeRatio
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfThumbnailProviderTest {
    @Test
    fun `bigger width pdf ratio`() {
        // region Arrange
        val page = mockk<PdfRenderer.Page>()
        every { page.width } returns 780
        every { page.height } returns 768
        // endregion
        // region Act
        val ratio = page.computeRatio(256, 256)
        // endregion
        // region Assert
        assertEquals(256f, 780 / ratio)
        // endregion
    }

    @Test
    fun `bigger height pdf ratio`() {
        // region Arrange
        val page = mockk<PdfRenderer.Page>()
        every { page.width } returns 768
        every { page.height } returns 780
        // endregion
        // region Act
        val ratio = page.computeRatio(256, 256)
        // endregion
        // region Assert
        assertEquals(256f, 780 / ratio)
        // endregion
    }

    @Test
    fun `same size pdf ratio`() {
        // region Arrange
        val page = mockk<PdfRenderer.Page>()
        every { page.width } returns 256
        every { page.height } returns 256
        // endregion
        // region Act
        val ratio = page.computeRatio(256, 256)
        // endregion
        // region Assert
        assertEquals(256f, 256f / ratio)
        // endregion
    }

    @Test
    fun `lower size pdf ratio`() {
        // region Arrange
        val page = mockk<PdfRenderer.Page>()
        every { page.width } returns 124
        every { page.height } returns 124
        // endregion
        // region Act
        val ratio = page.computeRatio(256, 256)
        // endregion
        // region Assert
        assertEquals(256f, 124 / ratio)
        // endregion
    }
}
