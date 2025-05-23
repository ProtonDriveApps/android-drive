/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.entitlement.data.extension

import me.proton.core.drive.entitlement.domain.entity.Entitlement
import me.proton.core.drive.entitlement.domain.entity.Entitlement.Key
import me.proton.core.drive.entitlement.domain.entity.Entitlement.MaxRevisionCount
import me.proton.core.drive.entitlement.domain.entity.Entitlement.PublicCollaboration

fun <T> Key<T>.fromString(value: String): Entitlement<T> = when (this) {
    Key.PublicCollaboration -> PublicCollaboration(value.toBoolean())
    Key.MaxRevisionCount -> MaxRevisionCount(value.toLong())
} as Entitlement<T>
