/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.base.data.api

/**
 * https://protonmail.gitlab-pages.protontech.ch/Slim-API/all/#section/API-Body-codes
 */
object ProtonApiCode {
    const val SUCCESS = 1000
    const val INVALID_REQUIREMENTS = 2000
    const val ALREADY_EXISTS = 2500
    const val NOT_EXISTS = 2501
    const val INSUFFICIENT_QUOTA = 200001
    const val ENCRYPTION_VERIFICATION_FAILED = 200501

    val Long.isSuccessful: Boolean get() = this == SUCCESS.toLong()
}
