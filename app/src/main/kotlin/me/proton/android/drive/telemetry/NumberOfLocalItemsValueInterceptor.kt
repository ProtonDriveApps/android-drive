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

package me.proton.android.drive.telemetry

import me.proton.android.drive.extension.log
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.usecase.CountLibraryItems
import me.proton.core.drive.base.data.entity.LoggerLevel.WARNING
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.telemetry.domain.entity.DriveTelemetryEvent
import me.proton.core.drive.telemetry.domain.manager.DriveTelemetryInterceptor
import javax.inject.Inject

class NumberOfLocalItemsValueInterceptor @Inject constructor(
    private val getPhotoShare: GetPhotoShare,
    private val countLibraryItems: CountLibraryItems,
) : DriveTelemetryInterceptor {
    override suspend fun invoke(
        userId: UserId,
        event: DriveTelemetryEvent,
    ): DriveTelemetryEvent = coRunCatching {
        val share = getPhotoShare(userId).toResult().getOrThrow()
        countLibraryItems(share.rootFolderId).getOrThrow()
    }.fold(
        onSuccess = { count ->
            val values = "number_of_local_items" to count.toFloat()
            event.copy(values = event.values + values)
        },
        onFailure = { error ->
            error.log(
                LogTag.TELEMETRY,
                "Cannot count for number_of_local_items dimension",
                WARNING,
            )
            event
        }
    )
}
