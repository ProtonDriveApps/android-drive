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
package me.proton.core.drive.base.data.formatter

import android.os.Build
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.base.domain.util.coRunCatching
import java.text.SimpleDateFormat
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import java.time.format.DateTimeFormatter as JavaDateTimeFormatter

class DateTimeFormatterImpl @Inject constructor() : DateTimeFormatter {
    private val _sdf = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
    } else {
        null
    }
    private val sdf: SimpleDateFormat get() = requireNotNull(_sdf)

    override fun formatToIso8601String(date: Date): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ZonedDateTime.ofInstant(
                date.toInstant(),
                ZoneOffset.UTC
            ).format(JavaDateTimeFormatter.ISO_INSTANT)
        } else {
            sdf.format(date)
        }

    override fun parseFromIso8601String(iso8601: String): Result<TimestampS> = coRunCatching {
        TimestampS(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ZonedDateTime.parse(iso8601).toEpochSecond()
            } else {
                TimeUnit.MILLISECONDS.toSeconds(sdf.parse(iso8601)!!.time)
            }
        )
    }
}
