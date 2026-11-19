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
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.runBlocking
import me.proton.android.drive.ui.screen.map.MapPoint
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.pgp.DecryptedText
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.usecase.Sha256Impl
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.provider.StorageLocationProvider
import me.proton.core.drive.crypto.domain.repository.DecryptedTextRepository
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.test.crypto.FakeKeyStoreCrypto
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PhotoMapLocationCacheTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `locations and confirmed absence survive an empty RAM cache`() = runBlocking {
        val crypto = TestKeyStoreCrypto()
        val photo = listing("pictured")
        val unlocated = listing("unlocated")
        val entry = CachedSdkLocation("first", MapPoint(0.2, 0.4), name = "private-photograph")
        val missing = CachedSdkLocation("second")
        cache(crypto).apply {
            put(photo, entry)
            put(unlocated, missing)
        }

        val files = folder.root.walk().filter(File::isFile).toList()
        assertEquals(2, files.size)
        assertFalse(files.any { it.readText().contains("private-photograph") })
        val reopened = cache(crypto)
        assertEquals(entry, reopened.get(photo, revisionId = null))
        assertEquals(missing, reopened.get(unlocated, revisionId = "second"))
        assertNull(reopened.get(photo, revisionId = "replacement"))
        assertNull(reopened.get(listing("pictured", user = UserId("another-user")), revisionId = null))
    }

    @Test
    fun `unavailable or pass-through keystore writes only to RAM`() = runBlocking {
        val entry = CachedSdkLocation("first", MapPoint(0.2, 0.4))
        val photo = listing("pictured")
        val passThrough = object : KeyStoreCrypto by FakeKeyStoreCrypto() {
            override fun isUsingKeyStore() = true
        }

        for (crypto in listOf(FakeKeyStoreCrypto(), passThrough)) {
            val writer = cache(crypto)
            writer.put(photo, entry)
            assertEquals(entry, writer.get(photo, revisionId = null))
            assertNull(cache(crypto).get(photo, revisionId = null))
            assertFalse(folder.root.walk().any(File::isFile))
        }
    }

    @Test
    fun `corrupt encrypted file is a cache miss`() = runBlocking {
        val crypto = TestKeyStoreCrypto()
        val photo = listing("pictured")
        cache(crypto).put(photo, CachedSdkLocation("first"))
        folder.root.walk().single(File::isFile).writeText("corrupt")

        assertNull(cache(crypto).get(photo, revisionId = null))
    }

    private fun cache(crypto: KeyStoreCrypto) =
        PhotoMapLocationCache(TestStorage(folder.root), crypto, Sha256Impl(), Memory())

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
        override suspend fun getPermanentFolder(userId: UserId, path: String): File =
            File(File(root, userId.id), path).apply { mkdirs() }

        override suspend fun getCacheFolder(): File = error("Unexpected directory")
        override suspend fun getCacheFolder(userId: UserId, path: String): File = error("Unexpected directory")
        override suspend fun getCacheTempFolder(userId: UserId): File = error("Unexpected directory")
        override suspend fun getPermanentTempFolder(userId: UserId, path: String): File = error("Unexpected directory")
        override fun getDebugLogFolder(): File = error("Unexpected directory")
        override suspend fun getCameraFolder(): File = error("Unexpected directory")
    }

    private class TestKeyStoreCrypto : KeyStoreCrypto by FakeKeyStoreCrypto() {
        private val secret = KeyGenerator.getInstance("AES").generateKey()

        override fun isUsingKeyStore() = true

        override fun encrypt(value: String): String {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secret)
            return Base64.getEncoder().encodeToString(cipher.iv + cipher.doFinal(value.toByteArray()))
        }

        override fun decrypt(value: String): String {
            val ciphertext = Base64.getDecoder().decode(value)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(128, ciphertext.copyOfRange(0, 12)))
            return cipher.doFinal(ciphertext, 12, ciphertext.size - 12).decodeToString()
        }
    }
}
