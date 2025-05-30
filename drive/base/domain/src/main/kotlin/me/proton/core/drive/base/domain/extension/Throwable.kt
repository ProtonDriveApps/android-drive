/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.base.domain.extension

import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult


fun ApiException.hasHttpCode(code: Int): Boolean =
    (error as? ApiResult.Error.Http)?.httpCode == code

fun Throwable.hasHttpCode(code: Int): Boolean =
    (this as? ApiException)?.hasHttpCode(code) ?: false

inline fun <T> Throwable.onProtonHttpException(block: (protonData: ApiResult.Error.ProtonData) -> T): T? =
    ((this as? ApiException)?.error as? ApiResult.Error.Http)?.proton?.let { protonData ->
        block(protonData)
    }
