/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.recently.accessed.data.reporter

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatchingException
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.provider.ProtonSdkClientProvider
import me.proton.core.drive.recently.accessed.domain.entity.RecentlyAccessed
import me.proton.core.drive.recently.accessed.domain.reporter.RecentlyAccessedReporter
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.drive.sdk.entity.RecentlyAccessedReportItem
import java.time.Instant
import javax.inject.Inject

class SdkRecentlyAccessedReporter @Inject constructor(
    private val sdkClientProvider: ProtonSdkClientProvider,
) : RecentlyAccessedReporter {

    override suspend fun report(
        userId: UserId,
        volumeId: VolumeId,
        items: List<RecentlyAccessed>,
    ): Result<Unit> = coRunCatchingException {
        val linkId = items.firstOrNull()?.linkId ?: return Result.success(Unit)
        val client = sdkClientProvider.getOrCreate(linkId).getOrThrow()
        client.reportRecentlyAccessed(
            items.map { item ->
                RecentlyAccessedReportItem(
                    nodeUid = item.linkId.nodeUid(volumeId),
                    accessTime = Instant.ofEpochMilli(item.accessTime.value),
                )
            }
        )
    }
}
