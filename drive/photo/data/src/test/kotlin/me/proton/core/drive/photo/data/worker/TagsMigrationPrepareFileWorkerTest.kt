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
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.usecase.InsertTagsMigrationFiles
import me.proton.core.drive.photo.domain.usecase.TagsMigrationPrepareFile
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.UnknownHostException
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TagsMigrationPrepareFileWorkerTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var insertTagsMigrationFiles: InsertTagsMigrationFiles

    private lateinit var fileId: FileId

    @Before
    fun setUp() = runTest {
        driveRule.db.photo {
            fileId = file("photo-id")
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
    }

    @Test
    fun success() = runTest {
        val worker = tagsMigrationPrepareFileWorker(photoVolumeId, fileId)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun failure() = runTest {
        val worker = tagsMigrationPrepareFileWorker(
            photoVolumeId,
            fileId,
            object : TagsMigrationPrepareFile {
                override suspend fun invoke(
                    volumeId: VolumeId,
                    fileId: FileId,
                    startTagging: Boolean
                ) = Result.failure<String?>(RuntimeException())
            }
        )
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun retry() = runTest {
        val worker = tagsMigrationPrepareFileWorker(
            photoVolumeId,
            fileId,
            object : TagsMigrationPrepareFile {
                override suspend fun invoke(
                    volumeId: VolumeId,
                    fileId: FileId,
                    startTagging: Boolean
                ) = Result.failure<String?>(
                    ApiException(
                        ApiResult.Error.Connection(
                            cause = UnknownHostException()
                        )
                    )
                )
            }
        )
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    private fun tagsMigrationPrepareFileWorker(
        volumeId: VolumeId,
        fileId: FileId,
        prepareFile: TagsMigrationPrepareFile = object : TagsMigrationPrepareFile {
            override suspend fun invoke(
                volumeId: VolumeId,
                fileId: FileId,
                startTagging: Boolean
            ) = Result.success(null)
        }
    ): TagsMigrationPrepareFileWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<TagsMigrationPrepareFileWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = TagsMigrationPrepareFileWorker(
                    appContext = appContext,
                    workerParams = workerParameters,
                    prepareFile = prepareFile,
                )
            })
            .setInputData(
                TagsMigrationPrepareFileWorker.workDataOf(volumeId, fileId)
            )
            .build()
    }
}
