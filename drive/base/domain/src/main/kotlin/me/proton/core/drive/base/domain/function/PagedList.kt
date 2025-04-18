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

package me.proton.core.drive.base.domain.function

suspend fun <T> pagedList(
    pageSize: Int,
    page: suspend (fromIndex: Int, count: Int) -> List<T>
): MutableList<T> {
    require(pageSize > 0) {
        "pageSize should be strictly positive"
    }
    val items = mutableListOf<T>()
    var loaded: Int
    var fromIndex = 0
    do {
        val pageItems = page(fromIndex, pageSize)
        fromIndex += pageSize
        loaded = pageItems.size
        items.addAll(pageItems)
    } while (loaded == pageSize)
    return items
}

suspend fun <T> processPagedList(
    pageSize: Int,
    page: suspend (fromIndex: Int, count: Int) -> List<T>,
    block: suspend (List<T>) -> Unit,
) {
    require(pageSize > 0) {
        "pageSize should be strictly positive"
    }
    var loaded: Int
    var fromIndex = 0
    do {
        val pageItems = page(fromIndex, pageSize)
        fromIndex += pageSize
        loaded = pageItems.size
        block(pageItems)
    } while (loaded == pageSize)
}
