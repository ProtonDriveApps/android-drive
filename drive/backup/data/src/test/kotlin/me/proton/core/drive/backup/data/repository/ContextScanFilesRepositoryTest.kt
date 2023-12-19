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

package me.proton.core.drive.backup.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.provider.MediaStore.MediaColumns
import androidx.core.net.toUri
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.repository.ContextScanFilesRepository.ScanResult.Data
import me.proton.core.drive.backup.data.repository.ContextScanFilesRepository.ScanResult.NotFound
import me.proton.core.drive.base.domain.entity.TimestampS
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsInstanceOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContextScanFilesRepositoryTest {
    private val resolver = mockk<ContentResolver>()
    private val context = mockk<Context> {
        every { contentResolver } returns resolver
    }
    private val contextScanFilesRepository = ContextScanFilesRepository(context)

    @Test
    fun empty() = runTest {
        val result = contextScanFilesRepository(emptyList())
        assertEquals(Result.success(emptyList<ContextScanFilesRepository.ScanResult>()), result)
    }

    @Test
    fun find() = runTest {
        val uri = "content://uri/0".toUri()

        every {
            resolver.query(uri, null, null, null, null)
        } returns MatrixCursor(
            arrayOf(
                MediaColumns._ID,
                MediaColumns.DATE_ADDED,
                MediaColumns.BUCKET_ID,
            )
        ).apply {
            addRow(arrayOf(0, 12345, 1))
        }
        val result = contextScanFilesRepository(listOf(uri))
        assertEquals(Result.success(listOf(Data(uri, TimestampS(12345), 1))), result)
    }

    @Test
    fun `not found`() = runTest {
        val uri = "content://uri/0".toUri()

        every {
            resolver.query(uri, null, null, null, null)
        } returns MatrixCursor(arrayOf(MediaColumns._ID, MediaColumns.DATE_ADDED))
        val result = contextScanFilesRepository(listOf(uri))
        assertEquals(Result.success(listOf(NotFound(uri))), result)
    }

    @Test
    fun error() = runTest {
        val uri = "content://media/external".toUri()

        every {
            resolver.query(uri, null, null, null, null)
        } throws IllegalStateException("Unknown URL: $uri is hidden API")
        val result = contextScanFilesRepository(listOf(uri))
        val scanResult = result.getOrThrow().first()
        assertThat(scanResult, IsInstanceOf(NotFound::class.java))
        assertEquals(uri, scanResult.uri)
        assertThat((scanResult as NotFound).error, IsInstanceOf(IllegalStateException::class.java))
        assertEquals(
            scanResult.error?.message,
            "Unknown URL: content://media/external is hidden API"
        )
    }
}
