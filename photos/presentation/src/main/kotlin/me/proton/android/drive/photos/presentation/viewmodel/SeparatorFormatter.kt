/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.presentation.viewmodel

import android.content.Context
import android.content.res.Resources
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.log.LogTag
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N


class SeparatorFormatter constructor(
    private val resources: Resources,
    private val clock: () -> Long,
    private val locale: Locale,
    private val minTimestampS: TimestampS = TimestampS(
        LocalDateTime.of(1900, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC)
    ),
    private val maxTimestampS: TimestampS = TimestampS(
        LocalDateTime.of(3000, 1, 1, 0, 0).toEpochSecond(ZoneOffset.UTC)
    )
) {
    @Inject
    constructor(
        @ApplicationContext context: Context
    ) : this(context.resources, System::currentTimeMillis, Locale.getDefault())

    private val sameYear: SimpleDateFormat by lazy {
        SimpleDateFormat("MMMM", locale)
    }
    private val differentYear: SimpleDateFormat by lazy {
        SimpleDateFormat("MMMM yyyy", locale)
    }

    fun toSeparator(timestampS: TimestampS): String {
        val timestampInSeconds = timestampS.value.coerceIn(minTimestampS.value, maxTimestampS.value)
        val calendar = Calendar.getInstance(locale).apply {
            timeInMillis = timestampInSeconds * 1000L
        }
        val now = Calendar.getInstance(locale).apply {
            timeInMillis = clock()
        }
        return if (now.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
            if (now.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)) {
                resources.getString(I18N.string.photos_separator_current_month)
            } else {
                sameYear.tryFormat(calendar, timestampS, timestampInSeconds)
            }
        } else {
            differentYear.tryFormat(calendar, timestampS, timestampInSeconds)
        }
    }

    private fun SimpleDateFormat.tryFormat(
        calendar: Calendar,
        timestampS: TimestampS,
        coercedTimestampS: Long,
    ) = runCatching {
        format(calendar.time)
    }.onFailure { error ->
        error.log(
            tag = LogTag.PHOTO,
            message = buildString {
                append("Formatting failed")
                append(", calendar time=${calendar.time.time}")
                append(", timestampS=${timestampS.value}")
                append(", minTimestampS=${minTimestampS.value}")
                append(", maxTimestampS=${maxTimestampS.value}")
                append(", coercedTimestampS=$coercedTimestampS")
                append(", locale=$locale")
            },
        )
    }.getOrThrow()
}
