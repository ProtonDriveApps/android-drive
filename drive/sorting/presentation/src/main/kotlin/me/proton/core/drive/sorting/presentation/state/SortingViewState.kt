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
package me.proton.core.drive.sorting.presentation.state

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.util.kotlin.exhaustive
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Immutable
data class SortingViewState(
    @StringRes val title: Int,
    @DrawableRes val icon: Int
)

fun Sorting.toSortingViewState() =
    SortingViewState(
        title = by.toTitleResId(),
        icon = direction.toIconResId()
    )

fun By.toTitleResId() =
    when (this) {
        By.NAME -> I18N.string.common_name
        By.LAST_MODIFIED -> I18N.string.title_last_modified
        By.SIZE -> I18N.string.title_size
        By.TYPE -> I18N.string.title_file_type
    }.exhaustive

private fun Direction.toIconResId() =
    when (this) {
        Direction.ASCENDING -> CorePresentation.drawable.ic_proton_arrow_up
        Direction.DESCENDING -> CorePresentation.drawable.ic_proton_arrow_down
    }.exhaustive
