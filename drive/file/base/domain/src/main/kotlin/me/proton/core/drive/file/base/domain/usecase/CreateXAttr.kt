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
package me.proton.core.drive.file.base.domain.usecase

import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.CameraExifTags
import me.proton.core.drive.base.domain.entity.Location
import me.proton.core.drive.base.domain.entity.MediaResolution
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.file.base.domain.entity.XAttr
import me.proton.core.drive.file.base.domain.extension.captureTime
import me.proton.core.drive.file.base.domain.extension.subjectCoordinates
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration

class CreateXAttr @Inject constructor(
    private val dateTimeFormatter: DateTimeFormatter
) {
    operator fun invoke(modificationTime: Date = Date()) =
        XAttr(
            common = XAttr.Common(
                modificationTime = dateTimeFormatter.formatToIso8601String(modificationTime)
            )
        )

    operator fun invoke(
        modificationTime: Date,
        size: Bytes,
        blockSizes: List<Bytes>,
        mediaResolution: MediaResolution? = null,
        digests: Map<String, String>? = null,
        mediaDuration: Duration? = null,
        creationDateTime: TimestampS? = null,
        location: Location? = null,
        cameraExifTags: CameraExifTags? = null,
    ) =
        XAttr(
            common = XAttr.Common(
                modificationTime = dateTimeFormatter.formatToIso8601String(modificationTime),
                size = size.value,
                blockSizes = blockSizes.map { blockSize -> blockSize.value },
                digests = digests,
            ),
            media = mediaResolution?.let {
                XAttr.Media(
                    width = mediaResolution.width,
                    height = mediaResolution.height,
                    duration = mediaDuration?.inWholeSeconds?.toDouble(),
                )
            },
            location = location?.let {
                XAttr.Location(
                    latitude = location.latitude,
                    longitude = location.longitude,
                )
            },
            camera = cameraExifTags?.let {
                creationDateTime?.captureTime(dateTimeFormatter)?.let { captureTime ->
                    XAttr.Camera(
                        captureTime = captureTime,
                        device = cameraExifTags.model,
                        orientation = cameraExifTags.orientation,
                        subjectCoordinates = cameraExifTags.subjectCoordinates,
                    )
                }
            },
        )
}
