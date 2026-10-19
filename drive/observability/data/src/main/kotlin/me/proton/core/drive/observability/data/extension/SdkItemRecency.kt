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

package me.proton.core.drive.observability.data.extension

import me.proton.core.drive.observability.domain.metrics.sdk.common.Recency
import me.proton.drive.sdk.telemetry.ItemRecency as SdkItemRecency

fun SdkItemRecency.toRecency(): Recency = when (this) {
    SdkItemRecency.UNRECOGNIZED -> Recency.unknown
    SdkItemRecency.UNSPECIFIED -> Recency.unknown
    SdkItemRecency.PAST_MONTH -> Recency.past_month
    SdkItemRecency.PAST_YEAR -> Recency.past_year
    SdkItemRecency.SINCE_2024 -> Recency.since_2024
    SdkItemRecency.BEFORE_2024 -> Recency.before_2024
}
