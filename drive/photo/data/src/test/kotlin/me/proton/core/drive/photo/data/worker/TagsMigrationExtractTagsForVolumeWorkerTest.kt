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

package me.proton.core.drive.photo.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.NullableLinkEntity
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.PREPARED
import me.proton.core.drive.photo.domain.usecase.InsertTagsMigrationFiles
import me.proton.core.drive.photo.domain.usecase.TagsMigrationExtractTagsForVolume
import me.proton.core.drive.photo.domain.usecase.UpdateTagsMigrationFileState
import me.proton.core.drive.photo.domain.usecase.UpdateTagsMigrationFileUri
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TagsMigrationExtractTagsForVolumeWorkerTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var insertTagsMigrationFiles: InsertTagsMigrationFiles
    @Inject
    lateinit var updateTagsMigrationFileUri: UpdateTagsMigrationFileUri
    @Inject
    lateinit var updateTagsMigrationFileState: UpdateTagsMigrationFileState

    @Inject
    lateinit var extractTagsForVolume: TagsMigrationExtractTagsForVolume

    private lateinit var fileId: FileId

    @Before
    fun setUp() = runTest {
        driveRule.db.photo {
            fileId = file(
                link = NullableLinkEntity(
                    id = "photo-id",
                    parentId = this.link.id,
                    type = 2L,
                    name = "Screenshot_xyz.png",
                )
            )
        }
        insertTagsMigrationFiles(
            listOf(
                TagsMigrationFile(
                    volumeId = photoVolumeId,
                    fileId = fileId,
                    captureTime = TimestampS(),
                )
            )
        ).getOrThrow()
        updateTagsMigrationFileUri(photoVolumeId, fileId, "uri").getOrThrow()
        updateTagsMigrationFileState(photoVolumeId, fileId, PREPARED).getOrThrow()
    }

    @Test
    fun `happy path`() = runTest {
        val worker = tagsMigrationExtractTagsForVolumeWorker(userId, photoVolumeId)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    private fun tagsMigrationExtractTagsForVolumeWorker(
        userId: UserId,
        volumeId: VolumeId,
    ): TagsMigrationExtractTagsForVolumeWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<TagsMigrationExtractTagsForVolumeWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = TagsMigrationExtractTagsForVolumeWorker(
                    appContext = appContext,
                    workerParams = workerParameters,
                    extractTagsForVolume = extractTagsForVolume,
                )
            })
            .setInputData(
                TagsMigrationExtractTagsForVolumeWorker.workDataOf(userId, volumeId)
            )
            .build()
    }

}
