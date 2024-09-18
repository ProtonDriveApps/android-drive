/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.drivelink.shared.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.standardShare
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.drivelink.shared.domain.usecase.MigrateKeyPacket
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.getShare
import me.proton.core.drive.test.api.getShareBootstrap
import me.proton.core.drive.test.api.getUnmigratedShares
import me.proton.core.drive.test.api.response
import me.proton.core.drive.test.api.retryableErrorResponse
import me.proton.core.drive.test.entity.NullableShareDto
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MigrateKeyPacketWorkerTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var migrateKeyPacket: MigrateKeyPacket

    private val standardShareId = standardShareId()

    @Before
    fun setUp() = runTest {
        driveRule.db.photo {}
    }

    @Test
    fun success() = runTest {
        driveRule.server.run {
            getShare(listOf(NullableShareDto(standardShareId.id)))
            getUnmigratedShares { response(404) }
        }

        val result = makeWorker(userId).doWork()

        assertEquals(Result.success(), result)
    }

    @Test
    fun error() = runTest {
        driveRule.server.run {
            getShare(listOf(NullableShareDto(standardShareId.id)))
            getUnmigratedShares { errorResponse() }
        }

        val result = makeWorker(userId).doWork()

        assertEquals(Result.failure(), result)
    }

    @Test
    fun retry() = runTest {
        driveRule.server.run {
            getUnmigratedShares { retryableErrorResponse() }
        }

        val result = makeWorker(userId).doWork()

        assertEquals(Result.retry(), result)
    }


    private fun makeWorker(userId: UserId): MigrateKeyPacketWorker =
        TestListenableWorkerBuilder<MigrateKeyPacketWorker>(
            ApplicationProvider.getApplicationContext()
        )
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker =
                    MigrateKeyPacketWorker(appContext, workerParameters, migrateKeyPacket)
            })
            .setInputData(MigrateKeyPacketWorker.workDataOf(userId))
            .build()


}
