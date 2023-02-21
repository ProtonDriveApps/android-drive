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
package me.proton.core.drive.upload.data.provider

import android.graphics.BitmapFactory
import junit.framework.TestCase.assertEquals
import me.proton.core.drive.thumbnail.data.provider.ImageThumbnailProvider.Companion.calculateInSampleSize
import org.junit.Test

class ImageThumbnailProviderTest {

    @Test
    fun `bigger width image inSampleSize without rotation`() {
        // region Arrange
        val options = BitmapFactory.Options()
        options.outWidth = 780
        options.outHeight = 768
        // endregion
        // region Act
        val inSampleSize = options.calculateInSampleSize(256, 256, 0)
        // endregion
        // region Assert
        assertEquals(4, inSampleSize)
        // endregion
    }

    @Test
    fun `bigger height image inSampleSize without rotation`() {
        // region Arrange
        val options = BitmapFactory.Options()
        options.outWidth = 768
        options.outHeight = 780
        // endregion
        // region Act
        val inSampleSize = options.calculateInSampleSize(256, 256, 0)
        // endregion
        // region Assert
        assertEquals(4, inSampleSize)
        // endregion
    }

    @Test
    fun `same size image inSampleSize without rotation`() {
        // region Arrange
        val options = BitmapFactory.Options()
        options.outWidth = 256
        options.outHeight = 256
        // endregion
        // region Act
        val inSampleSize = options.calculateInSampleSize(256, 256, 0)
        // endregion
        // region Assert
        assertEquals(1, inSampleSize)
        // endregion
    }

    @Test
    fun `lower size image inSampleSize without rotation`() {
        // region Arrange
        val options = BitmapFactory.Options()
        options.outWidth = 124
        options.outHeight = 124
        // endregion
        // region Act
        val inSampleSize = options.calculateInSampleSize(256, 256, 0)
        // endregion
        // region Assert
        assertEquals(1, inSampleSize)
        // endregion
    }

    @Test
    fun `image inSampleSize with rotation`() {
        // region Arrange
        val options = BitmapFactory.Options()
        options.outWidth = 5616
        options.outHeight = 3744
        // endregion
        listOf(
            0 to 47,
            90 to 32,
            180 to 47,
            270 to 32,
        ).forEach { (rotation, expectedSampleSize) ->
            // region Act
            val inSampleSize = options.calculateInSampleSize(120, 256, rotation)
            // endregion
            // region Assert
            assertEquals(expectedSampleSize, inSampleSize)
            // endregion
        }
    }
}
