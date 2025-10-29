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

package me.proton.core.drive.log.domain.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.log.toBase36
import me.proton.core.drive.log.domain.entity.Log

internal fun Event.Backup.toLog(userId: UserId): Log = Log(
    userId = userId,
    message = "$state"
            + " total=$total,"
            + " preparing=$preparing,"
            + " pending=$pending,"
            + " failed=$failed"
    ,
    creationTime = occurredAt,
    origin = Log.Origin.EVENT_BACKUP,
)

internal fun Event.BackupFolder.toLog(userId: UserId): Log = Log(
    userId = userId,
    message = "$state(${bucketId.toBase36()})"
            + " total=$total,"
            + " preparing=$preparing,"
            + " pending=$pending,"
            + " failed=$failed",
    creationTime = occurredAt,
    origin = Log.Origin.EVENT_BACKUP,
)

internal fun Event.BackupEnabled.toLog(userId: UserId): Log = Log(
    userId = userId,
    message = "Backup[${folderId.id.logId()}] enabled",
    creationTime = occurredAt,
    origin = Log.Origin.EVENT_BACKUP,
)

internal fun Event.BackupDisabled.toLog(userId: UserId): Log = Log(
    userId = userId,
    message = "Backup[${folderId.id.logId()}] disabled",
    creationTime = occurredAt,
    origin = Log.Origin.EVENT_BACKUP,
)

internal fun Event.BackupStarted.toLog(userId: UserId): Log = Log(
    userId = userId,
    message = "Backup[${folderId.id.logId()}] started",
    creationTime = occurredAt,
    origin = Log.Origin.EVENT_BACKUP,
)

internal fun Event.BackupStopped.toLog(userId: UserId): Log = Log(
    userId = userId,
    message = "Backup[${folderId.id.logId()}] stopped: $state",
    creationTime = occurredAt,
    origin = Log.Origin.EVENT_BACKUP,
)

internal fun Event.BackupCompleted.toLog(userId: UserId): Log = Log(
    userId = userId,
    message = "Backup[${folderId.id.logId()}] completed",
    creationTime = occurredAt,
    origin = Log.Origin.EVENT_BACKUP,
)


internal fun Event.BackupSync.toLog(userId: UserId): Log = Log(
    userId = userId,
    message = "Backup[${folderId.id.logId()}] sync ${bucketId.toBase36()}",
    creationTime = occurredAt,
    origin = Log.Origin.EVENT_BACKUP,
)
