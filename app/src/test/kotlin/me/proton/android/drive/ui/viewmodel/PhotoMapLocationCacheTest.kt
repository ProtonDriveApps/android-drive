/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.ui.viewmodel

import java.io.File
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.ui.screen.map.MapPoint
import me.proton.core.crypto.common.pgp.DecryptedText
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.usecase.Sha256Impl
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.provider.StorageLocationProvider
import me.proton.core.drive.crypto.domain.repository.DecryptedTextRepository
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PhotoMapLocationCacheTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `locations and confirmed absence survive an empty RAM cache`() = runBlocking {
        val photo = listing("pictured")
        val unlocated = listing("unlocated")
        val entry = CachedSdkLocation("first", MapPoint(0.2, 0.4), name = "private-photograph")
        val missing = CachedSdkLocation("second")
        cache().apply {
            put(photo, entry)
            put(unlocated, missing)
            assertEquals(entry, get(photo, revisionId = "first"))
        }

        val files = File(folder.root, "cache/user/photo-map/locations-v3").walk().filter(File::isFile).toList()
        assertEquals(2, files.size)
        assertFalse(files.any { it.readText().contains("private-photograph") })
        assertFalse(files.any { it.readText().contains("photo-map:sdk-location") })
        val reopened = cache()
        assertEquals(entry.copy(name = null), reopened.get(photo, revisionId = null))
        assertEquals(missing, reopened.get(unlocated, revisionId = "second"))
        assertNull(reopened.get(photo, revisionId = "replacement"))
        assertNull(reopened.get(listing("pictured", user = UserId("another-user")), revisionId = null))
    }

    @Test
    fun `clearing the private cache removes disk entries`() = runBlocking {
        val photo = listing("pictured")
        val entry = CachedSdkLocation("first", MapPoint(0.2, 0.4))
        cache().put(photo, entry)

        assertEquals(entry, cache().get(photo, revisionId = "first"))
        assertTrue(File(folder.root, "cache").deleteRecursively())
        assertNull(cache().get(photo, revisionId = "first"))
    }

    @Test
    fun `corrupt file is a cache miss`() = runBlocking {
        val photo = listing("pictured")
        cache().put(photo, CachedSdkLocation("first"))
        folder.root.walk().single(File::isFile).writeText("corrupt")

        assertNull(cache().get(photo, revisionId = null))
    }

    private fun cache() =
        PhotoMapLocationCache(TestStorage(folder.root), Sha256Impl(), Memory())

    private fun listing(id: String, user: UserId = UserId("user")) = PhotoListing.Volume(
        volumeId = VolumeId("volume"),
        linkId = FileId(ShareId(user, "share"), id),
        captureTime = TimestampS(0),
        nameHash = null,
        contentHash = null,
    )

    private class Memory : DecryptedTextRepository {
        private val entries = mutableMapOf<Pair<UserId, String>, DecryptedText>()

        override fun addDecryptedText(userId: UserId, keyId: String, decryptedText: DecryptedText) {
            entries[userId to keyId] = decryptedText
        }

        override fun getDecryptedText(userId: UserId, keyId: String) = entries[userId to keyId]

        override fun removeAll(userId: UserId) {
            entries.keys.removeAll { it.first == userId }
        }
    }

    private class TestStorage(private val root: File) : StorageLocationProvider {
        override suspend fun getCacheFolder(): File = error("Unexpected directory")
        override suspend fun getCacheFolder(userId: UserId, path: String): File =
            File(File(root, "cache/${userId.id}"), path).apply { mkdirs() }

        override suspend fun getPermanentFolder(userId: UserId, path: String): File = error("Unexpected directory")
        override suspend fun getCacheTempFolder(userId: UserId): File = error("Unexpected directory")
        override suspend fun getPermanentTempFolder(userId: UserId, path: String): File = error("Unexpected directory")
        override fun getDebugLogFolder(): File = error("Unexpected directory")
        override suspend fun getCameraFolder(): File = error("Unexpected directory")
    }
}
