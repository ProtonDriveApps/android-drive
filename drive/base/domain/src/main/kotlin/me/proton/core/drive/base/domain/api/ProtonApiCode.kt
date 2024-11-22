/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.base.domain.api

/**
 * https://protonmail.gitlab-pages.protontech.ch/Slim-API/all/#section/API-Body-codes
 */
object ProtonApiCode {
    const val SUCCESS = 1000L
    const val INVALID_REQUIREMENTS = 2000
    const val INVALID_VALUE = 2001
    const val NOT_ALLOWED = 2011
    const val FEATURE_DISABLED = 2032
    const val ALREADY_EXISTS = 2500
    const val NOT_EXISTS = 2501
    const val INCOMPATIBLE_STATE = 2511
    const val INSUFFICIENT_QUOTA = 200001
    const val EXCEEDED_QUOTA = 200002
    const val TOO_MANY_CHILDREN = 200300
    const val ENCRYPTION_VERIFICATION_FAILED = 200501
    const val KEY_GET_ADDRESS_MISSING = 33102
    const val KEY_GET_DOMAIN_EXTERNAL = 33103

    val Long.isSuccessful: Boolean get() = this == SUCCESS
}
