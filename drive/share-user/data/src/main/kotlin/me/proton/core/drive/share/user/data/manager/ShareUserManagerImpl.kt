/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.share.user.data.manager

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.await
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.user.data.worker.ConvertExternalInvitationWorker
import me.proton.core.drive.share.user.domain.manager.ShareUserManager
import javax.inject.Inject

class ShareUserManagerImpl @Inject constructor(
    private val workManager: WorkManager,
) : ShareUserManager {
    override suspend fun convertExternalInvitation(linkId: LinkId, id: String) {
        coRunCatching {
            workManager.enqueueUniqueWork(
                "convert-external-invitation-$id",
                ExistingWorkPolicy.KEEP,
                ConvertExternalInvitationWorker.getWorkRequest(linkId, id),
            ).await()
        }.onFailure {error ->
            error.log(SHARING, "Cannot enqueue ConvertExternalInvitationWorker for $id")
        }
    }
}
