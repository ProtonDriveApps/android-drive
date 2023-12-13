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

package me.proton.core.drive.base.presentation.extension

import android.content.Context
import android.text.format.DateUtils
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toTimestampMs
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

fun TimestampS.toReadableDate(format: Int = DateFormat.LONG): String = toTimestampMs().toReadableDate(format)

fun TimestampS.asHumanReadableString(
    format: Int = DateFormat.MEDIUM
): String = toTimestampMs().asHumanReadableString(format)

fun TimestampS.asHumanReadableStringRelative(context: Context, now: Long = System.currentTimeMillis()): CharSequence =
    toTimestampMs().asHumanReadableStringRelative(context, now)

fun TimestampMs.toReadableDate(format: Int = DateFormat.LONG): String =
    DateFormat.getDateInstance(format).format(Date(value))

fun TimestampMs.asHumanReadableString(format: Int = DateFormat.MEDIUM): String =
    DateFormat.getDateInstance(format).format(Date(value))

fun TimestampMs.asHumanReadableStringRelative(context: Context, now: Long = System.currentTimeMillis()): CharSequence {
    val lastModifiedCalendar = asCalendar()
    val nowCalendar = TimestampMs(now).asCalendar()
    return DateUtils.formatDateTime(
        context,
        lastModifiedCalendar.timeInMillis,
        when {
            lastModifiedCalendar.isSameDay(nowCalendar) -> DateUtils.FORMAT_SHOW_TIME
            lastModifiedCalendar.isSameYear(nowCalendar) ->
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_ALL
            else -> DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL
        }
    )
}

fun TimestampS.asHumanReadableString(
    context: Context,
    flags: Int = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME,
) = toTimestampMs().asHumanReadableString(context, flags)

fun TimestampMs.asHumanReadableString(
    context: Context,
    flags: Int = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME,
): CharSequence = DateUtils.formatDateTime(
        context,
        asCalendar().timeInMillis,
        flags,
    )

fun TimestampMs.asCalendar(): Calendar =
    Calendar.getInstance().apply { timeInMillis = value }

fun Calendar.isSameYear(other: Calendar) =
    get(Calendar.YEAR) == other.get(Calendar.YEAR)

fun Calendar.isSameDay(other: Calendar): Boolean {
    if (get(Calendar.YEAR) != other.get(Calendar.YEAR)) {
        return false
    }
    if (get(Calendar.MONTH) != other.get(Calendar.MONTH)) {
        return false
    }
    if (get(Calendar.DAY_OF_MONTH) != other.get(Calendar.DAY_OF_MONTH)) {
        return false
    }
    return true
}
