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
package me.proton.android.drive.lock.data.usecase

import me.proton.android.drive.lock.data.crypto.PgpSecretKey
import me.proton.android.drive.lock.domain.entity.AppLock
import me.proton.android.drive.lock.domain.entity.AppLockType
import me.proton.android.drive.lock.domain.entity.SecretKey
import me.proton.android.drive.lock.domain.usecase.GetAppLock
import me.proton.core.drive.base.domain.util.coRunCatching
import javax.inject.Inject

class GetAppLockImpl @Inject constructor() : GetAppLock {
    override operator fun invoke(secretKey: SecretKey, appLockType: AppLockType): Result<AppLock> = coRunCatching {
        require(secretKey is PgpSecretKey) { "secret key is no a PgpSecretKey, but ${secretKey.javaClass.simpleName}" }
        AppLock(
            key = secretKey.lockedKey,
            type = appLockType,
        )
    }
}
