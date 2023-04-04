/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.android.drive.lock.data.extension

import android.util.Base64
import me.proton.android.drive.lock.data.db.entity.LockEntity
import me.proton.android.drive.lock.domain.entity.LockKey

fun LockEntity.toLock(): LockKey = LockKey(
    appKeyPassphrase = Base64.decode(this.appKeyPassphrase, Base64.NO_WRAP),
    appKey = this.appKey,
    type = this.type,
)
