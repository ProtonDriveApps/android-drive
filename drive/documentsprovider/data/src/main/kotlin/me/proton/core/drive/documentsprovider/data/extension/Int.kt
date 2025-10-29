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

package me.proton.core.drive.documentsprovider.data.extension

internal inline fun Int.add(block: () -> Int): Int = this or block()

internal inline fun Int.addIfElse(condition: Boolean, matched: () -> Int, unmatched: () -> Int = { 0 }): Int {
    return if (condition) {
        add(matched)
    } else {
        add(unmatched)
    }
}

internal inline fun Int.addIf(condition: Boolean, matched: () -> Int) = addIfElse(condition, matched)

