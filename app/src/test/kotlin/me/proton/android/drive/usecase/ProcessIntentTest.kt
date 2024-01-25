/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.usecase

import android.content.Intent
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.android.drive.ui.viewmodel.AccountViewModel
import me.proton.core.account.domain.entity.Account
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.drivelink.upload.domain.usecase.ValidateUploadLimit
import me.proton.core.drive.i18n.R
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.notification.domain.usecase.RemoveNotification
import me.proton.core.drive.upload.data.resolver.AggregatedUriResolver
import me.proton.core.drive.upload.domain.usecase.GetUploadFileName
import me.proton.core.test.kotlin.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProcessIntentTest {
    private val removeNotification = mockk<RemoveNotification>()
    private val broadcastMessages = mockk<BroadcastMessages>()
    private val getUploadFileName = mockk<GetUploadFileName>()
    private val validateUploadLimit = mockk<ValidateUploadLimit>()
    private val accountViewModel = mockk<AccountViewModel>()
    private val aggregatedUriResolver = mockk<AggregatedUriResolver>()
    private val appContext = RuntimeEnvironment.getApplication().applicationContext
    private val userId = UserId("user-id")
    private val account = mockk<Account>()
    private lateinit var processIntent: ProcessIntent
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun before() {
        Dispatchers.setMain(testDispatcher)
        coEvery { validateUploadLimit(any(), any()) } returns Result.success(Unit)
        coEvery { accountViewModel.state } returns MutableStateFlow(AccountViewModel.State.AccountReady)
        coEvery { accountViewModel.primaryAccount } returns MutableStateFlow(account)
        coEvery { account.userId } returns userId
        coEvery { broadcastMessages(userId, any(), any(), any()) } returns Unit
        coEvery { getUploadFileName(any<String>()) } returns "db-drive"
        coEvery { aggregatedUriResolver.schemes } returns setOf("file", "content")
        processIntent = ProcessIntent(
            appContext = appContext,
            removeNotification = removeNotification,
            broadcastMessages = broadcastMessages,
            getUploadFileName = getUploadFileName,
            validateUploadLimit = validateUploadLimit,
            validateExternalUri = ValidateExternalUri(appContext, aggregatedUriResolver),
        )
    }

    @Test
    fun `ProcessIntent broadcast message when invalid uri is provided`() = runTest {
        // Given
        val intent = Intent()
            .setAction(Intent.ACTION_SEND)
            .putExtra(
                Intent.EXTRA_STREAM,
                ValidateExternalUriTest.invalidUris(appContext)[1],
            )

        // When
        processIntent(
            intent = intent,
            deepLinkIntent = MutableSharedFlow(),
            accountViewModel = accountViewModel,
        ).join()

        // Then
        verify(exactly = 1) {
            broadcastMessages(
                userId = userId,
                message = appContext.getString(
                    R.string.in_app_notification_upload_files_only,
                    appContext.getString(R.string.app_name)
                ),
                type = BroadcastMessage.Type.INFO,
                extra = null,
            )
        }
    }

    @Test
    fun `ProcessIntent broadcast message when all provided uris are invalid`() = runTest {
        // Given
        val intent = Intent()
            .setAction(Intent.ACTION_SEND_MULTIPLE)
            .putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList(ValidateExternalUriTest.invalidUris(appContext)),
            )

        // When
        processIntent(
            intent = intent,
            deepLinkIntent = MutableSharedFlow(),
            accountViewModel = accountViewModel,
        ).join()

        // Then
        verify(exactly = 1) {
            broadcastMessages(
                userId = userId,
                message = appContext.getString(
                    R.string.in_app_notification_upload_files_only,
                    appContext.getString(R.string.app_name)
                ),
                type = BroadcastMessage.Type.INFO,
                extra = null,
            )
        }
    }

    @Test
    fun `ProcessIntent emits Intent when valid uri is provided`() = runTest {
        // Given
        val deepLinkIntent = MutableSharedFlow<Intent>(replay = 1)
        val intent = Intent()
            .setAction(Intent.ACTION_SEND)
            .putExtra(
                Intent.EXTRA_STREAM,
                ValidateExternalUriTest.validUris[0],
            )

        // When
        val countDownLatch = CountDownLatch(1)
        val job = deepLinkIntent
            .onEach {
                countDownLatch.countDown()
            }
            .launchIn(this)
        processIntent(
            intent = intent,
            deepLinkIntent = deepLinkIntent,
            accountViewModel = accountViewModel,
        ).join()
        countDownLatch.await(5, TimeUnit.SECONDS)
        job.cancel()

        // Then
        assertEquals(1, deepLinkIntent.replayCache.size) { "Process intent emit deep link intent has wrong size" }
    }

    @Test
    fun `ProcessIntent emits Intent when at least one valid uri is provided`() = runTest {
        // Given
        val deepLinkIntent = MutableSharedFlow<Intent>(replay = 1)
        val intent = Intent()
            .setAction(Intent.ACTION_SEND_MULTIPLE)
            .putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList(ValidateExternalUriTest.invalidUris(appContext) + ValidateExternalUriTest.validUris[0]),
            )

        // When
        val countDownLatch = CountDownLatch(1)
        val job = deepLinkIntent
            .onEach {
                countDownLatch.countDown()
            }
            .launchIn(this)
        processIntent(
            intent = intent,
            deepLinkIntent = deepLinkIntent,
            accountViewModel = accountViewModel,
        ).join()
        countDownLatch.await(5, TimeUnit.SECONDS)
        job.cancel()

        // Then
        assertEquals(1, deepLinkIntent.replayCache.size) { "Process intent emit deep link intent has wrong size" }
    }
}
