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

package me.proton.core.drive.drivelink.device.presentation.options

import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.files.presentation.entry.OptionEntry
import me.proton.core.presentation.R as CorePresentation
import me.proton.core.drive.i18n.R as I18N

interface DeviceOptionEntry : OptionEntry<Device>

class RenameDeviceOption(
    override val onClick: (Device) -> Unit
) : DeviceOptionEntry {

    override val icon: Int get() = CorePresentation.drawable.ic_proton_pen

    override val label: Int get() = I18N.string.common_rename_action
}
