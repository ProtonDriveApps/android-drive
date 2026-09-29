/*
 * Copyright (c) 2025 Proton AG.
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

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

data class VisibleItems(
    val firstIndex: Int,
    val count: Int,
) {
    val offscreenCount: Int get() = minOf(count, MAX_OFFSCREEN_PAGE_SIZE)

    companion object {
        const val MAX_OFFSCREEN_PAGE_SIZE = 36
    }
}

@Composable
fun LazyListState.rememberVisibleItems(): VisibleItems {
    val visibleItems by remember(this) {
        derivedStateOf {
            VisibleItems(
                firstIndex = firstVisibleItemIndex,
                count = layoutInfo.visibleItemsInfo.size.coerceAtLeast(1),
            )
        }
    }
    return visibleItems
}

@Composable
fun LazyGridState.rememberVisibleItems(): VisibleItems {
    val visibleItems by remember(this) {
        derivedStateOf {
            VisibleItems(
                firstIndex = firstVisibleItemIndex,
                count = layoutInfo.visibleItemsInfo.size.coerceAtLeast(1),
            )
        }
    }
    return visibleItems
}

fun <T> List<T>.aroundVisibleItems(
    visibleItems: VisibleItems,
    leadingItemCount: Int = 0,
): List<T> {
    val firstIndex = (visibleItems.firstIndex - leadingItemCount).coerceAtLeast(0)
    return takeIf { list -> list.isNotEmpty() && list.size > firstIndex }
        ?.let { list ->
            val sizeRange = IntRange(0, list.size - 1)
            val fromIndex = (firstIndex - visibleItems.offscreenCount).coerceIn(sizeRange)
            val toIndex = (firstIndex + visibleItems.count + visibleItems.offscreenCount - 1)
                .coerceIn(sizeRange)
            list.subList(fromIndex, toIndex + 1)
        }
        ?: emptyList()
}

fun <T> List<T>.offscreenAroundVisibleItems(
    visibleItems: VisibleItems,
    leadingItemCount: Int = 0,
): List<T> {
    val firstIndex = (visibleItems.firstIndex - leadingItemCount).coerceAtLeast(0)
    return takeIf { list -> list.isNotEmpty() && list.size > firstIndex }
        ?.let { list ->
            val sizeRange = IntRange(0, list.size - 1)
            val afterFrom = (firstIndex + visibleItems.count).coerceIn(sizeRange)
            val afterTo = (firstIndex + visibleItems.count + visibleItems.offscreenCount - 1)
                .coerceIn(sizeRange)
            val before = if (firstIndex == 0) {
                emptyList()
            } else {
                list.subList(
                    (firstIndex - visibleItems.offscreenCount).coerceIn(sizeRange),
                    firstIndex,
                )
            }
            list.subList(afterFrom, afterTo + 1) + before
        }
        ?: emptyList()
}
