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
package me.proton.core.drive.base.domain.entity

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class Percentage private constructor(val value: Float) {

    companion object {
        operator fun invoke(value: Float) = Percentage(value.coerceIn(0f, 1f))
        operator fun invoke(percentage: Int) = when (percentage) {
            0 -> Percentage(0f)
            100 -> Percentage(1f)
            else -> invoke(percentage / 100f)
        }
    }
}
