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

package me.proton.android.drive.ui.extension

import androidx.test.espresso.contrib.PickerActions
import me.proton.core.drive.drivelink.shared.presentation.component.asDayOfMonth
import me.proton.core.drive.drivelink.shared.presentation.component.asMonth
import me.proton.core.drive.drivelink.shared.presentation.component.asYear
import me.proton.core.util.kotlin.toInt
import me.proton.test.fusion.ui.espresso.wrappers.EspressoActions
import java.util.Date

fun EspressoActions.setDate(date: Date, normalizeMonthOfYear: Boolean = true) =
    perform(
        PickerActions.setDate(
            date.asYear,
            date.asMonth + (!normalizeMonthOfYear).toInt(),
            date.asDayOfMonth
        )
    )