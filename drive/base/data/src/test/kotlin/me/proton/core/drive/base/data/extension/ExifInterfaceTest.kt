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
import androidx.exifinterface.media.ExifInterface.TAG_DATETIME
import androidx.exifinterface.media.ExifInterface.TAG_OFFSET_TIME
import io.mockk.every
import io.mockk.mockk
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.test.kotlin.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@RunWith(ParameterizedRobolectricTestRunner::class)
class ExifInterfaceTest(
    private val tagDateTime: String,
    private val tagOffsetTime: String?,
    private val expectedUtcTime: String?,
) {
    private val exifInterface = mockk<ExifInterface>()

    @Test
    fun `creationDateTime is in UTC`() {
        // Given
        every { exifInterface.getAttribute(TAG_DATETIME) } returns tagDateTime
        every { exifInterface.getAttribute(TAG_OFFSET_TIME) } returns tagOffsetTime

        // When
        val creationDateTime = exifInterface.creationDateTime

        // Then
        assertEquals(expectedUtcTime?.toTimestampS(), creationDateTime) {
            "TAG_DATETIME $tagDateTime TAG_OFFSET_TIME $tagOffsetTime EXPECTED UTC TIME $expectedUtcTime"
        }
    }

    private fun String.toTimestampS(): TimestampS? =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            .parse(this)?.let { date ->
                TimestampS(TimeUnit.MILLISECONDS.toSeconds(date.time))
            }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "TAG_DATE_TIME {0}, TAG_OFFSET_TIME {1}, EXPECTED UTC TIME {2}"
        )
        fun data() = listOf(
            arrayOf("2008:06:01 02:11:-612368384", null, null),
            arrayOf("2008:06:01 02:12:1879048192", null, null),
            arrayOf("2020:03:22 11:56:22", "+01:00", "2020-03-22 10:56:22"),
            arrayOf("2020:03:22 11:56:22", null, "2020-03-22 11:56:22"),
            arrayOf("2020:03:22 11:56:22", "-02:30", "2020-03-22 14:26:22"),
            arrayOf("2020:03:22 00:56:22", "+01:00", "2020-03-21 23:56:22"),
            arrayOf("2020:03:01 00:56:22", "+02:00", "2020-02-29 22:56:22"),
            arrayOf("2019:03:01 00:56:22", "+03:00", "2019-02-28 21:56:22"),
            arrayOf("2020:01:01 00:56:22", "+04:00", "2019-12-31 20:56:22"),
            arrayOf("2022:04:07 23:56:22", "-01:00", "2022-04-08 00:56:22"),
            arrayOf("2022:04:30 23:56:22", "-02:00", "2022-05-01 01:56:22"),
            arrayOf("2022:12:31 23:56:22", "-03:00", "2023-01-01 02:56:22"),
        )
    }
}
