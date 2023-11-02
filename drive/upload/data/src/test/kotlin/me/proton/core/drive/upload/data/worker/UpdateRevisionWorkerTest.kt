/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.upload.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.upload.domain.usecase.UpdateRevision
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class UpdateRevisionWorkerTest {
    private val userId: UserId = UserId("user-id")
    private val workManager = mockk<WorkManager>()
    private val broadcastMessages = mockk<BroadcastMessages>()
    private val getUploadFileLink = mockk<GetUploadFileLink>()
    private val updateRevision = mockk<UpdateRevision>()
    private val configurationProvider = mockk<ConfigurationProvider>()
    private val canRun = mockk<CanRun>()
    private val run = mockk<Run>()
    private val done = mockk<Done>()
    private val uploadFileLink = mockk<UploadFileLink>()
    private val operation = mockk<Operation>()

    @Before
    fun before() {
        coEvery { canRun(any(), any()) } returns Result.success(true)
        coEvery { getUploadFileLink(any() as Long) } returns DataResult.Success(ResponseSource.Local, uploadFileLink)
        coEvery { workManager.enqueue(any() as WorkRequest) } returns operation
        coEvery { configurationProvider.useExceptionMessage } returns false
        coEvery { broadcastMessages(userId, any(), any(), any()) } returns Unit
    }

    @Test
    fun `when commit a revision receives error with proton code 200501, upload fails and message is shown`() = runTest {
        // Given
        val uploadFile = "proton_drive.pdf"
        coEvery { uploadFileLink.name } returns uploadFile
        val errorFromServer = "Upload failed: Verification of data failed"
        coEvery { updateRevision(any()) } returns Result.failure(
            ApiException(
                ApiResult.Error.Http(
                    httpCode = 422,
                    message = "Unprocessable Content",
                    proton = ApiResult.Error.ProtonData(
                        code = 200501,
                        error = errorFromServer,
                    )
                )
            )
        )

        // When
        val result = updateRevisionWorker(userId).doLimitedRetryWork()

        // Then
        assertEquals(ListenableWorker.Result.failure(), result)
        verify(exactly = 1) {
            broadcastMessages(
                userId = userId,
                message = "Uploading file $uploadFile failed with reason: $errorFromServer",
                type = BroadcastMessage.Type.ERROR,
                extra = null,
            )
        }
    }

    private fun updateRevisionWorker(
        userId: UserId,
    ): UpdateRevisionWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<UpdateRevisionWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters
                    ) = UpdateRevisionWorker(
                        appContext = appContext,
                        workerParams = workerParameters,
                        workManager = workManager,
                        broadcastMessages = broadcastMessages,
                        getUploadFileLink = getUploadFileLink,
                        updateRevision = updateRevision,
                        configurationProvider = configurationProvider,
                        canRun = canRun,
                        run = run,
                        done = done,
                    )
                }
            )
            .setInputData(
                Data.Builder()
                    .putString(WorkerKeys.KEY_USER_ID, userId.id)
                    .build()
            )
            .build()
    }
}
