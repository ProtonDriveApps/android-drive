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
import me.proton.core.drive.base.domain.entity.TimestampS
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class SeparatorFormatter constructor(
    private val resources: Resources,
    private val clock: () -> Long,
    private val locale: Locale,
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
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestampS.value * 1000L
        }
        val now = Calendar.getInstance().apply {
            timeInMillis = clock()
        }
        return if (now.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
            if (now.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)) {
                resources.getString(I18N.string.photos_separator_current_month)
            } else {
                sameYear.format(calendar.time)
            }
        } else {
            differentYear.format(calendar.time)
        }
    }
}
