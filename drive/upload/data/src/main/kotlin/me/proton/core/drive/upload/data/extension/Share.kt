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

package me.proton.core.drive.upload.data.extension

import me.proton.core.drive.observability.domain.metrics.common.ShareType
import me.proton.core.drive.share.domain.entity.Share

fun Share.Type.toShareType(): ShareType = when (this) {
    Share.Type.MAIN -> ShareType.main
    Share.Type.PHOTO -> ShareType.photo
    Share.Type.DEVICE -> ShareType.device
    Share.Type.STANDARD -> ShareType.shared
    else -> error("Unknown share type $this")
}
