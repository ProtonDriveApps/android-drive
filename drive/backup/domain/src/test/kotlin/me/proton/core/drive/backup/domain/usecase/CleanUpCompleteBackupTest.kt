/*
 * Copyright (c) 2023-2024 Proton AG.
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

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.handler.MemoryEventHandler
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject


@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CleanUpCompleteBackupTest {
    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId
    private lateinit var backupFolder: BackupFolder

    @Inject
    lateinit var cleanUpCompleteBackup: CleanUpCompleteBackup

    @Inject
    lateinit var setFiles: SetFiles

    @Inject
    lateinit var markAsCompleted: MarkAsCompleted

    @Inject
    lateinit var getBackupStatus: GetBackupStatus

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var handler: MemoryEventHandler

    private val bucketId = 0

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles {}
        backupFolder = BackupFolder(bucketId, folderId)
        addFolder(backupFolder).getOrThrow()
    }

    @Test
    fun empty() = runTest {
        cleanUpCompleteBackup(backupFolder).getOrThrow()

        assertEquals(
            BackupStatus.Complete(totalBackupPhotos = 0),
            getBackupStatus(folderId).first(),
        )
        assertEquals(
            mapOf(userId to listOf(Event.BackupCompleted(folderId))),
            handler.events,
        )
    }

    @Test
    fun complete() = runTest {
        setFiles(
            listOf(
                backupFile(bucketId, "uri1"),
                backupFile(bucketId, "uri2"),
                backupFile(bucketId, "uri3"),
            )
        ).getOrThrow()

        markAsCompleted(folderId, "uri1").getOrThrow()
        markAsCompleted(folderId, "uri2").getOrThrow()
        markAsCompleted(folderId, "uri3").getOrThrow()

        cleanUpCompleteBackup(backupFolder).getOrThrow()

        assertEquals(
            BackupStatus.Complete(totalBackupPhotos = 0),
            getBackupStatus(folderId).first(),
        )
        assertEquals(
            mapOf(userId to listOf(Event.BackupCompleted(folderId))),
            handler.events,
        )
    }

    @Test
    fun incomplete() = runTest {
        setFiles(
            listOf(
                backupFile(bucketId, "uri1"),
                backupFile(bucketId, "uri2"),
                backupFile(bucketId, "uri3"),
            )
        ).getOrThrow()

        markAsCompleted(folderId, "uri1").getOrThrow()

        cleanUpCompleteBackup(backupFolder).getOrThrow()

        assertEquals(
            BackupStatus.InProgress(totalBackupPhotos = 3, pendingBackupPhotos = 2),
            getBackupStatus(folderId).first(),
        )
        assertEquals(
            emptyMap<UserId, List<Event>>(),
            handler.events,
        )
    }


    private fun backupFile(
        bucketId: Int,
        uriString: String,
    ) = BackupFile(
        bucketId = bucketId,
        folderId = folderId,
        uriString = uriString,
        mimeType = "",
        name = "",
        hash = "",
        size = 0.bytes,
        state = BackupFileState.READY,
        date = TimestampS(0),
    )
}
