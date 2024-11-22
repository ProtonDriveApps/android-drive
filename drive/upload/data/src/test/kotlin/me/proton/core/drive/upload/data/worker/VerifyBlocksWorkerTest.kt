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
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.verifier.domain.exception.VerifierException
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.drive.upload.domain.usecase.UploadMetricsNotifier
import me.proton.core.drive.upload.domain.usecase.VerifyBlocks
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.Logger
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class VerifyBlocksWorkerTest {
    private val userId: UserId = UserId("user-id")
    private val workManager = mockk<WorkManager>()
    private val broadcastMessages = mockk<BroadcastMessages>()
    private val getUploadFileLink = mockk<GetUploadFileLink>()
    private val uploadErrorManager = mockk<UploadErrorManager>()
    private val verifyBlocks = mockk<VerifyBlocks>()
    private val configurationProvider = mockk<ConfigurationProvider>()
    private val canRun = mockk<CanRun>()
    private val run = mockk<Run>()
    private val done = mockk<Done>()
    private val uploadFileLink = mockk<UploadFileLink>()
    private val operation = mockk<Operation>()
    private val logger = mockk<Logger>()
    private val uploadMetricsNotifier = mockk<UploadMetricsNotifier>()

    @Before
    fun before() {
        coEvery { canRun(any(), any()) } returns Result.success(true)
        coEvery { getUploadFileLink(any() as Long) } returns DataResult.Success(ResponseSource.Local, uploadFileLink)
        coEvery { uploadErrorManager.post(any()) } returns Unit
        coEvery { uploadFileLink.id } returns 123L
        coEvery { uploadFileLink.name } returns "secret.jpg"
        coEvery { uploadFileLink.uriString } returns "uriString"
        coEvery { uploadFileLink.shouldBroadcastErrorMessage } returns true
        coEvery { workManager.enqueue(any() as WorkRequest) } returns operation
        coEvery { configurationProvider.useExceptionMessage } returns false
        coEvery { broadcastMessages(userId, any(), any(), any()) } returns Unit
        coEvery { logger.d(any(), any()) } returns Unit
        coEvery { logger.e(any(), any(), any()) } returns Unit
        coEvery { uploadMetricsNotifier(any(), any(), any(), any()) } returns Unit
    }

    @Test
    fun `when verify blocks fails with VerifyBlock exception, upload fails and it is logged as error`() = runTest {
        // Given
        val error = VerifierException.VerifyBlock(CryptoException("Invalid key"))
        coEvery { verifyBlocks(uploadFileLink) } returns Result.failure(error)
        val uploadFileLinkId = uploadFileLink.id
        CoreLogger.set(logger)

        // When
        val result = verifyBlocksWorker(userId).doLimitedRetryWork()

        // Then
        Assert.assertEquals(ListenableWorker.Result.failure(), result)
        verify(exactly = 1) {
            logger.e(
                tag = "core.drive.upload.$uploadFileLinkId",
                e = error,
                message = "Verify blocks failed retryable false, max retries reached false",
            )
        }
    }

    private fun verifyBlocksWorker(
        userId: UserId,
    ): VerifyBlocksWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<VerifyBlocksWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters
                    ) = VerifyBlocksWorker(
                        appContext = appContext,
                        workerParams = workerParameters,
                        workManager = workManager,
                        broadcastMessages = broadcastMessages,
                        getUploadFileLink = getUploadFileLink,
                        uploadErrorManager = uploadErrorManager,
                        verifyBlocks = verifyBlocks,
                        uploadMetricsNotifier = uploadMetricsNotifier,
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
