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
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.proton.core.crypto.common.pgp.DecryptedText
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.StorageLocationProvider
import me.proton.core.drive.base.domain.usecase.Sha256
import me.proton.core.drive.base.domain.util.coRunCatchingException
import me.proton.core.drive.crypto.domain.repository.DecryptedTextRepository
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.photo.domain.entity.PhotoListing

@Singleton
internal class PhotoMapLocationCache @Inject constructor(
    private val storage: StorageLocationProvider,
    private val sha256: Sha256,
    private val decryptedTextRepository: DecryptedTextRepository,
) {
    private val diskMutex = Mutex()

    suspend fun get(listing: PhotoListing, revisionId: String?): CachedSdkLocation? {
        val userId = listing.linkId.shareId.userId
        val key = listing.cacheKey
        val memory = decryptedTextRepository.getDecryptedText(userId, key)?.text?.let { decode(it, key) }
        if (memory != null) return memory.takeIf { it.canReuse(revisionId) }

        return withContext(Dispatchers.IO) {
            coRunCatchingException {
                diskMutex.withLock {
                    val digest = sha256(key, Sha256.OutputFormat.HEX).getOrThrow()
                    val file = cacheFile(userId, digest)
                    if (!file.isFile) return@withLock null
                    decode(file.readText(), digest)
                        ?.takeIf { it.canReuse(revisionId) }
                        ?.also { remember(userId, key, it) }
                }
            }.getOrNull()
        }
    }

    suspend fun put(listing: PhotoListing, entry: CachedSdkLocation) {
        val userId = listing.linkId.shareId.userId
        val key = listing.cacheKey

        withContext(Dispatchers.IO) {
            diskMutex.withLock {
                remember(userId, key, entry)
                coRunCatchingException {
                    val digest = sha256(key, Sha256.OutputFormat.HEX).getOrThrow()
                    write(cacheFile(userId, digest), digest, entry)
                }
            }
        }
    }

    private fun write(file: File, digest: String, entry: CachedSdkLocation) {
        val directory = requireNotNull(file.parentFile)
        directory.mkdirs()
        val temporary = Files.createTempFile(directory.toPath(), "location-", ".tmp").toFile()
        try {
            temporary.writeText(Json.encodeToString(StoredLocation(digest, entry.copy(name = null))))
            Files.move(temporary.toPath(), file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }

    private fun remember(userId: UserId, key: String, entry: CachedSdkLocation) =
        decryptedTextRepository.addDecryptedText(
            userId,
            key,
            DecryptedText(Json.encodeToString(StoredLocation(key, entry)), VerificationStatus.Unknown),
        )

    private suspend fun cacheFile(userId: UserId, digest: String): File {
        val directory = storage.getCacheFolder(userId, "photo-map/locations-v3")
        return File(File(directory, digest.take(2)), digest)
    }

    private fun decode(plaintext: String, key: String): CachedSdkLocation? =
        coRunCatchingException { Json.decodeFromString<StoredLocation>(plaintext) }.getOrNull()
            ?.takeIf { it.key == key }?.entry

    private val PhotoListing.cacheKey: String
        get() = "photo-map:sdk-location:v2:${linkId.shareId.userId.id}:${linkId.shareId.id}:${linkId.nodeUid(volumeId).value}"

    @Serializable
    private data class StoredLocation(val key: String, val entry: CachedSdkLocation)
}
