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

import me.proton.core.drive.base.domain.entity.CameraExifTags
import me.proton.core.drive.base.domain.entity.Rectangle
import org.junit.Assert
import org.junit.Test

class CameraExifTagsTest {
    private val cameraExifTags = CameraExifTags(
        model = "Test camera",
        orientation = 1,
        subjectArea = "",
    )

    @Test
    fun `conversion of exif SubjectArea tag where subject is given as coordinates`() {
        val cameraExifTags = cameraExifTags.copy(subjectArea = "820,600")
        Assert.assertNull(cameraExifTags.subjectAreaRectangle)
    }

    @Test
    fun `conversion of exif SubjectArea tag where subject is given as a circle`() {
        val cameraExifTags = cameraExifTags.copy(subjectArea = "820,600,480")
        Assert.assertEquals(
            Rectangle(
                top = 360,
                left = 580,
                bottom = 840,
                right = 1060,
            ),
            cameraExifTags.subjectAreaRectangle,
        )
    }

    @Test
    fun `conversion of exif SubjectArea tag where subject is given as a rectangle`() {
        val cameraExifTags = cameraExifTags.copy(subjectArea = "820,600,480,230")
        Assert.assertEquals(
            Rectangle(
                top = 485,
                left = 580,
                bottom = 715,
                right = 1060,
            ),
            cameraExifTags.subjectAreaRectangle,
        )
    }
}
