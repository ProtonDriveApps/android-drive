/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.observability.domain.metrics.sdk.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The schema values are not valid Kotlin identifiers, so unlike the other label enums
 * the entry names cannot carry them and [SerialName] has to.
 */
@Serializable
enum class CausedBy {
    @SerialName("1p")
    FIRST_PARTY,

    @SerialName("3p-sdk")
    THIRD_PARTY_WITH_SDK,

    @SerialName("3p")
    THIRD_PARTY,

    @SerialName("unknown")
    UNKNOWN,
}
