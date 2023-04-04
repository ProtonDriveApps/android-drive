/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.base.domain.extension

import me.proton.core.domain.arch.DataResult
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.util.kotlin.exhaustive

fun ApiException.toDataResult() : DataResult.Error = when (val e = error) {
    is ApiResult.Error.Http -> {
        DataResult.Error.Remote(
            message = e.proton?.error ?: message,
            cause = this,
            protonCode = e.proton?.code ?: 0,
            httpCode = e.httpCode
        )
    }
    is ApiResult.Error.Parse -> DataResult.Error.Remote(cause?.message, this)
    is ApiResult.Error.Connection -> DataResult.Error.Remote(cause?.message, this)
}.exhaustive
