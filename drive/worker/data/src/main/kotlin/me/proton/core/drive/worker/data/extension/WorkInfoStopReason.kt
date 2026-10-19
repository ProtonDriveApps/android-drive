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

package me.proton.core.drive.worker.data.extension

import me.proton.core.drive.base.data.entity.WorkInfoStopReason
import me.proton.core.drive.worker.domain.entity.Constraint

fun WorkInfoStopReason.toSystemConstraint(): Constraint.System? = when (this) {
    WorkInfoStopReason.CONSTRAINT_BATTERY_NOT_LOW -> Constraint.System.Type.BATTERY_NOT_LOW
    WorkInfoStopReason.CONSTRAINT_CHARGING -> Constraint.System.Type.CHARGING
    WorkInfoStopReason.CONSTRAINT_CONNECTIVITY -> Constraint.System.Type.CONNECTIVITY
    WorkInfoStopReason.CONSTRAINT_DEVICE_IDLE -> Constraint.System.Type.DEVICE_IDLE
    WorkInfoStopReason.CONSTRAINT_STORAGE_NOT_LOW -> Constraint.System.Type.STORAGE_NOT_LOW
    else -> null
}?.let { type -> Constraint.System(type) }
