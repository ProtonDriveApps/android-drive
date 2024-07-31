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

package me.proton.core.drive.link.data.api.extension

import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.extension.toDataResult
import me.proton.core.drive.link.data.api.response.LinkResponses
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.network.domain.ApiException

fun LinkResponses.mapResults(links: List<LinkId>): Map<LinkId, DataResult<Unit>> {
    return responses.associateBy(keySelector = { linkResponse ->
        linkResponse.linkId?.let { id ->
            links.first { link -> link.id == id }
        } ?: links[responses.indexOf(linkResponse)]
    }) { linkResponse ->
        with(ProtonApiCode) {
            if (linkResponse.response.code.isSuccessful) {
                DataResult.Success(ResponseSource.Remote, Unit)
            } else {
                DataResult.Error.Remote(
                    message = linkResponse.response.error,
                    cause = null,
                    protonCode = linkResponse.response.code.toInt()
                )
            }
        }
    }
}

inline fun associateResults(
    links: List<LinkId>,
    block: () -> LinkResponses,
): Map<LinkId, DataResult<Unit>> =
    try {
        block().mapResults(links)
    } catch (e: ApiException) {
        val result = e.toDataResult()
        links.associateWith { result }
    }
