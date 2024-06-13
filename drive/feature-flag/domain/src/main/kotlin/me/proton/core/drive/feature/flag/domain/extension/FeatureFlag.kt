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

package me.proton.core.drive.feature.flag.domain.extension

import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag

suspend fun FeatureFlag.onEnabled(block: suspend (FeatureFlag) -> Unit) = apply {
    if (state == FeatureFlag.State.ENABLED) {
        block(this)
    }
}

suspend fun FeatureFlag.onDisabled(block: suspend (FeatureFlag) -> Unit) = apply {
    if (state == FeatureFlag.State.DISABLED) {
        block(this)
    }
}

suspend fun FeatureFlag.onNotFound(block: suspend (FeatureFlag) -> Unit) = apply {
    if (state == FeatureFlag.State.NOT_FOUND) {
        block(this)
    }
}

suspend fun FeatureFlag.onDisabledOrNotFound(block: suspend (FeatureFlag) -> Unit) = apply {
    if (state == FeatureFlag.State.DISABLED || state == FeatureFlag.State.NOT_FOUND) {
        block(this)
    }
}

val FeatureFlag.on get() = state == FeatureFlag.State.ENABLED

val FeatureFlag.off get() = !on
