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

import android.media.MediaMetadataRetriever
import me.proton.core.drive.base.domain.entity.TimestampS
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

val MediaMetadataRetriever.date: TimestampS?
    get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)?.let { dateTime ->
        SimpleDateFormat("yyyyMMdd'T'hhmmss", Locale.US)
            .apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            .parse(dateTime)?.let { date ->
                TimestampS(TimeUnit.MILLISECONDS.toSeconds(date.time))
            }?.takeIf { date -> date.value >= 0 } // avoiding negative value and mostly -2082844800
    }
