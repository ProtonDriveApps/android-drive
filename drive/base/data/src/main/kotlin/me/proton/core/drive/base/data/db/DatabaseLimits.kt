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

package me.proton.core.drive.base.data.db

object DatabaseLimits {
    // To prevent excessive memory allocations,
    // the maximum value of a host parameter number is SQLITE_MAX_VARIABLE_NUMBER,
    // which defaults to 999 for SQLite versions prior to 3.32.0 (2020-05-22)
    // or 32766 for SQLite versions after 3.32.0.
    // https://www.sqlite.org/limits.html
    const val MAX_VARIABLE_NUMBER = 500
}
