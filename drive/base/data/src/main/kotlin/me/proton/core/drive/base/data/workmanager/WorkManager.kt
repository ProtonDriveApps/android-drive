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

package me.proton.core.drive.base.data.workmanager

import androidx.work.WorkInfo
import androidx.work.WorkRequest
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult

fun <B : WorkRequest.Builder<B, *>, W : WorkRequest> WorkRequest.Builder<B, W>.addTags(
    tags: Collection<String>,
) = apply {
    tags.forEach { tag ->
        this.addTag(tag)
    }
}

fun WorkInfo.getLong(key: String, defaultValue: Long = 0) =
    progress.getLong(key, Long.MIN_VALUE).let {
        if (it > Long.MIN_VALUE) {
            it
        } else {
            outputData.getLong(key, defaultValue)
        }
    }

//TODO: remove this as we have it in domain
inline fun <T> Throwable.onProtonHttpException(block: (protonData: ApiResult.Error.ProtonData) -> T): T? =
    ((this as? ApiException)?.error as? ApiResult.Error.Http)?.proton?.let { protonData ->
        block(protonData)
    }
