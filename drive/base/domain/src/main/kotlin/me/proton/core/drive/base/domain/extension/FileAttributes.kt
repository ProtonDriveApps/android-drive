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
import me.proton.core.drive.base.domain.entity.FileAttributes
import me.proton.core.drive.base.domain.entity.Location
import me.proton.core.drive.base.domain.entity.MediaResolution
import me.proton.core.drive.base.domain.entity.TimestampS
import kotlin.time.Duration

val FileAttributes?.cameraExifTags: CameraExifTags? get() = (this as? FileAttributes.Camera)?.cameraExifTags

val FileAttributes?.duration: Duration? get() = (this as? FileAttributes.Duration)?.duration

val FileAttributes?.creationDateTime: TimestampS? get() = (this as? FileAttributes.CreationDateTime)?.creationDateTime

val FileAttributes?.location: Location? get() = (this as? FileAttributes.Location)?.location

val FileAttributes?.resolution: MediaResolution? get() = (this as? FileAttributes.Resolution)?.resolution
