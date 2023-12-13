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

package me.proton.core.drive.base.domain.entity

import kotlin.time.Duration

sealed interface FileAttributes {
    sealed interface Camera : FileAttributes {
        val cameraExifTags: CameraExifTags?
    }

    sealed interface CreationDateTime : FileAttributes {
        val creationDateTime: TimestampS?
    }

    sealed interface Duration : FileAttributes {
        val duration: kotlin.time.Duration?
    }

    sealed interface Location : FileAttributes {
        val location: me.proton.core.drive.base.domain.entity.Location?
    }

    sealed interface Resolution : FileAttributes {
        val resolution: MediaResolution?
    }
}

data class ImageAttributes(
    override val cameraExifTags: CameraExifTags?,
    override val creationDateTime: TimestampS?,
    override val resolution: MediaResolution?,
    override val location: Location?,
) : FileAttributes.Camera, FileAttributes.CreationDateTime, FileAttributes.Location, FileAttributes.Resolution

data class VideoAttributes(
    override val cameraExifTags: CameraExifTags?,
    override val creationDateTime: TimestampS?,
    override val duration: Duration?,
    override val location: Location?,
    override val resolution: MediaResolution?,
) : FileAttributes.Camera, FileAttributes.CreationDateTime, FileAttributes.Duration,
    FileAttributes.Location, FileAttributes.Resolution
