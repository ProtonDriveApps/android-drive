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
package me.proton.core.drive.sorting.presentation.entity

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.sorting.domain.extension.toggleDirection
import me.proton.core.drive.sorting.presentation.state.toTitleResId
import me.proton.core.util.kotlin.exhaustive
import me.proton.core.presentation.R as CorePresentation

@Immutable
data class SortingOption(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    val isApplied: Boolean,
    val toggleDirection: Sorting,
)

fun defaultSortingOption(sortingBy: By) =
    SortingOption(
        icon = CorePresentation.drawable.ic_proton_arrow_up,
        title = sortingBy.toTitleResId(),
        isApplied = false,
        toggleDirection = Sorting(sortingBy, Sorting.DEFAULT_DIRECTION)
    )

fun Sorting.toSortingOption() =
    SortingOption(
        icon = direction.toIconResId(),
        title = by.toTitleResId(),
        isApplied = true,
        toggleDirection = toggleDirection()
    )

fun Sorting.toSortingOptions() = enumValues<By>().map { sortingBy ->
    if (by == sortingBy) {
        toSortingOption()
    } else {
        defaultSortingOption(sortingBy)
    }
}

private fun Direction.toIconResId() =
    when (this) {
        Direction.ASCENDING -> CorePresentation.drawable.ic_proton_arrow_up
        Direction.DESCENDING -> CorePresentation.drawable.ic_proton_arrow_down
    }.exhaustive

