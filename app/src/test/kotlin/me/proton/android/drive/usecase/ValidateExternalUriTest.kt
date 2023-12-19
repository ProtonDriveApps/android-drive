/*
 * Copyright (c) 2023 Proton AG.
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

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ValidateExternalUriTest {
    private val appContext = RuntimeEnvironment.getApplication().applicationContext
    private val validateExternalUri = ValidateExternalUri(appContext)

    @Test
    fun `valid and invalid Uris`() = runTest {
        // Given
        val uris = validUris + invalidUris(appContext)

        // When
        val result = with(validateExternalUri) {
            uris.validate()
        }

        // Then
        assertEquals(validUris.size, result.size) { "List size after `validate()` should contain only valid Uris" }
    }

    companion object {
        fun invalidUris(appContext: Context) = listOf(
            "file:////storage/emulated/0/${appContext.packageName}/databases/db-drive",
            "file:////data/data/${appContext.packageName}/databases/db-drive",
            "file:////data/theory_of_everything.pdf"
        ).map { uriString ->
            Uri.parse(uriString)
        }

        val validUris = listOf(
            "file:////sdcard/DCIM/Camera/IMG_20231207_123456.jpg",
            "file:////sdcard/Download/summary.txt",
            "content://media/external/videos/media/002",
            "content://com.android.providers.downloads.documents/document/001",
            "content://media/external/file/storage/emulated/0/Download/book.pdf",
        ).map { uriString ->
            Uri.parse(uriString)
        }
    }
}
