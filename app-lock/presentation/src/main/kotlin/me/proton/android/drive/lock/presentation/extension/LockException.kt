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

package me.proton.android.drive.lock.presentation.extension

import android.content.Context
import me.proton.android.drive.lock.domain.exception.LockException
import me.proton.core.drive.base.presentation.R as BasePresentation

fun LockException.getDefaultMessage(context: Context): String = when (this) {
    is LockException.BiometricAuthenticationFailed -> context.getString(
        BasePresentation.string.app_lock_system_biometrics_authentication_failed
    )
    is LockException.BiometricAuthenticationError -> errorMessage
    else -> error("Default message for exception is missing")
}