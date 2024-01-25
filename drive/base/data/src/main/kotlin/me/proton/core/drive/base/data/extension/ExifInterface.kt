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

package me.proton.core.drive.base.data.extension

import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90
import androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED
import androidx.exifinterface.media.ExifInterface.TAG_DATETIME
import androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED
import androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL
import androidx.exifinterface.media.ExifInterface.TAG_IMAGE_LENGTH
import androidx.exifinterface.media.ExifInterface.TAG_IMAGE_WIDTH
import androidx.exifinterface.media.ExifInterface.TAG_MODEL
import androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME
import androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_DIGITIZED
import androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME_ORIGINAL
import androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION
import androidx.exifinterface.media.ExifInterface.TAG_SUBJECT_AREA
import me.proton.core.drive.base.domain.entity.CameraExifTags
import me.proton.core.drive.base.domain.entity.Location
import me.proton.core.drive.base.domain.entity.MediaResolution
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.util.coRunCatching
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

val ExifInterface.mediaResolution: MediaResolution? get() {
    val orientation = getAttributeInt(
        TAG_ORIENTATION,
        ORIENTATION_UNDEFINED,
    )
    val (width, height) = when (orientation) {
        ORIENTATION_ROTATE_90,
        ORIENTATION_ROTATE_270 -> {
            getAttributeInt(TAG_IMAGE_LENGTH, 0) to
                    getAttributeInt(TAG_IMAGE_WIDTH, 0)
        }
        else -> getAttributeInt(TAG_IMAGE_WIDTH, 0) to
                getAttributeInt(TAG_IMAGE_LENGTH, 0)
    }
    return takeIf { width > 0 && height > 0 }?.let {
        MediaResolution(width.toLong(), height.toLong())
    }
}

val ExifInterface.location: Location? get() = latLong?.let { array ->
    array.takeIf { array.size == 2 }?.let {
        Location(
            latitude = array[0],
            longitude = array[1],
        )
    }
}

val ExifInterface.model: String? get() = getAttribute(TAG_MODEL)

val ExifInterface.orientation: Int get() = getAttributeInt(TAG_ORIENTATION, ORIENTATION_UNDEFINED)

val ExifInterface.creationDateTime: TimestampS?
    get() = getDate(TAG_DATETIME, TAG_OFFSET_TIME)
val ExifInterface.creationDateTimeOriginal: TimestampS?
    get() = getDate(TAG_DATETIME_ORIGINAL, TAG_OFFSET_TIME_ORIGINAL)
val ExifInterface.creationDateTimeDigitized: TimestampS?
    get() = getDate(TAG_DATETIME_DIGITIZED, TAG_OFFSET_TIME_DIGITIZED)

private val DATETIME_PRIMARY_FORMAT_PATTERN = """^(\d{4}):(\d{2}):(\d{2})\s(\d{2}):(\d{2}):(\d{2})$""".toRegex()
private fun ExifInterface.getDate(tagDateTime: String, tagOffsetTime: String) =
    getAttribute(tagDateTime)
        ?.takeIf { dateTime -> DATETIME_PRIMARY_FORMAT_PATTERN.matches(dateTime) }
        ?.let { dateTime ->
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                .apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                .parse(dateTime)?.let { date ->
                    utcOffsetTime(date, tagOffsetTime)
                }
        }

fun ExifInterface.utcOffsetTime(date: Date, key: String): TimestampS {
    val (hour, minute) = getAttribute(key)?.let { utcOffset ->
        require(utcOffset.length == 6) { "Unexpected format \"+/-HH:MM\", $utcOffset" }
        val invSign = utcOffset[0].invSign
        utcOffset
            .drop(1) // drop the sign character
            .split(":") // HH:MM
            .map { time -> time.toInt() * invSign }
            .toPair()
    } ?: (0 to 0)
    val calendar = Calendar.getInstance()
    calendar.time = date
    calendar.add(Calendar.MINUTE, minute)
    calendar.add(Calendar.HOUR, hour)
    return TimestampS(TimeUnit.MILLISECONDS.toSeconds(calendar.timeInMillis))
}

private fun<T> List<T>.toPair(): Pair<T, T> {
    require(size == 2) { "Unexpected list size: $size" }
    return this[0] to this[1]
}

private val Char.invSign: Int get() = when (this) {
    '+' -> -1
    '-' -> 1
    else -> error("Unexpected character: $this")
}

val ExifInterface.subjectArea: String? get() = getAttribute(TAG_SUBJECT_AREA)

val ExifInterface.cameraExifTags: CameraExifTags? get() = coRunCatching {
    CameraExifTags(
        model = requireNotNull(model),
        orientation = orientation,
        subjectArea = subjectArea,
    )
}.getOrNull()
