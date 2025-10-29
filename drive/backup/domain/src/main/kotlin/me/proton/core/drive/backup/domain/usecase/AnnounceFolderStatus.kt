/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.backup.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AsyncAnnounceEvent
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BackupState
import me.proton.core.drive.backup.domain.extension.toState
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class AnnounceFolderStatus @Inject constructor(
    val getBackupState: GetBackupState,
    val getFoldersFlow: GetFoldersFlow,
    val asyncAnnounceEvent: AsyncAnnounceEvent,
) {

    operator fun invoke(userId: UserId) = getFoldersFlow(userId).transformLatest { folders ->
        emitAll(folders.map { folder ->
            getBackupState(folder).mapLatest { status ->
                status.toEvent(folder)
            }.filterNotNull()
        }.asIterable().merge())
    }.filterNotNull().onEach { event ->
        asyncAnnounceEvent(userId, event)
    }
}

private fun BackupState.toEvent(folder: BackupFolder): Event? = backupStatus?.let { status ->
    Event.BackupFolder(
        folderId = folder.folderId,
        bucketId = folder.bucketId,
        state = status.toState(),
        total = status.total,
        preparing = status.preparing,
        pending = status.pending,
        failed = status.failed,
    )
}
