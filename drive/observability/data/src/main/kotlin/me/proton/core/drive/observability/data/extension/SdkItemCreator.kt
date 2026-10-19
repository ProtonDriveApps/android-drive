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

import me.proton.core.drive.observability.domain.metrics.sdk.common.CausedBy
import me.proton.drive.sdk.telemetry.ItemCreator as SdkItemCreator

fun SdkItemCreator.toCausedBy(): CausedBy = when (this) {
    SdkItemCreator.UNRECOGNIZED -> CausedBy.UNKNOWN
    SdkItemCreator.UNSPECIFIED -> CausedBy.UNKNOWN
    SdkItemCreator.FIRST_PARTY -> CausedBy.FIRST_PARTY
    SdkItemCreator.THIRD_PARTY_WITH_SDK -> CausedBy.THIRD_PARTY_WITH_SDK
    SdkItemCreator.THIRD_PARTY_WITHOUT_SDK -> CausedBy.THIRD_PARTY
}
