/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.drivelink.crypto.domain.usecase

import me.proton.core.drive.crypto.domain.usecase.DecryptLinkName
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.usecase.Sha256
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.DecryptLinkXAttr
import me.proton.core.drive.cryptobase.domain.entity.UnlockedKey
import me.proton.core.drive.cryptobase.domain.usecase.UnlockKey
import me.proton.core.drive.drivelink.crypto.domain.repository.DecryptedTextRepository
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.updateLastModified
import me.proton.core.drive.drivelink.domain.usecase.GetLastModified
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetLinkParentKey
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.userId
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class DecryptDriveLink @Inject constructor(
    private val unlockKey: UnlockKey,
    private val getLinkParentKey: GetLinkParentKey,
    private val decryptLinkName: DecryptLinkName,
    private val decryptLinkXAttr: DecryptLinkXAttr,
    private val getLastModified: GetLastModified,
    private val decryptedTextRepository: DecryptedTextRepository,
    private val sha256: Sha256,
) {

    suspend operator fun invoke(
        folder: DriveLink.Folder,
        failOnError: Boolean = true,
    ): Result<DriveLink.Folder> =
        decrypt(folder, failOnError)

    suspend operator fun invoke(
        file: DriveLink.File,
        failOnError: Boolean = true,
    ): Result<DriveLink.File> =
        decrypt(file, failOnError)

    suspend operator fun invoke(driveLink: DriveLink, failOnError: Boolean = true): Result<DriveLink> =
        decrypt(driveLink, failOnError)

    private suspend fun <T : DriveLink> decrypt(driveLink: T, failOnError: Boolean): Result<T> =
        getLinkParentKey(driveLink).mapCatching { parentKey ->
            unlockKey(parentKey.keyHolder) { unlockedKey ->
                invoke(unlockedKey, driveLink, failOnError).getOrThrow()
            }.getOrThrow()
        }

    suspend operator fun <T : DriveLink> invoke(
        unlockedParentKey: UnlockedKey,
        driveLink: T,
        failOnError: Boolean = true,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<T> = coRunCatching(coroutineContext) {
        driveLink
            .decryptXAttrAndUpdateLastModified()
            .decryptName(unlockedParentKey, failOnError)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : DriveLink> T.decryptName(unlockedParentKey: UnlockedKey, failOnError: Boolean): T {
        if (cryptoName is CryptoProperty.Decrypted) return this
        val decryptedName = decryptedTextRepository.getDecryptedText(userId, nameKey) ?:
            decryptLinkName(unlockedParentKey, this.id)
                .onFailure { exception ->
                    if (failOnError) throw exception
                }
                .onSuccess { decryptedName ->
                    decryptedTextRepository.addDecryptedText(userId, nameKey, decryptedName)
                }
                .getOrNull()
        return decryptedName?.let {
            when (this) {
                is DriveLink.File -> copy(
                    cryptoName = CryptoProperty.Decrypted(decryptedName.text, decryptedName.status)
                ) as T
                is DriveLink.Folder -> copy(
                    cryptoName = CryptoProperty.Decrypted(decryptedName.text, decryptedName.status)
                ) as T
                else -> throw IllegalStateException("This should not happen")
            }
        } ?: this
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : DriveLink> T.decryptXAttrAndUpdateLastModified(): T {
        if (cryptoXAttr is CryptoProperty.Decrypted) return this
        if (xAttr == null) return this
        val decryptedXAttr = decryptedTextRepository.getDecryptedText(userId, xAttrKey) ?:
            decryptLinkXAttr(this)
                .onSuccess { decryptedXAttr ->
                    decryptedTextRepository.addDecryptedText(userId, xAttrKey, decryptedXAttr)
                }
                .getOrNull(LogTag.ENCRYPTION, "Cannot decrypt xAttr")
        return decryptedXAttr?.let {
            when (this) {
                is DriveLink.File -> {
                    val file = copy(
                        cryptoXAttr = CryptoProperty.Decrypted(decryptedXAttr.text, decryptedXAttr.status)
                    )
                    file.copy(link = updateLastModified(getLastModified(file)) as Link.File) as T
                }
                is DriveLink.Folder -> {
                    val folder = copy(
                        cryptoXAttr = CryptoProperty.Decrypted(decryptedXAttr.text, decryptedXAttr.status)
                    )
                    folder.copy(link = updateLastModified(getLastModified(folder)) as Link.Folder) as T
                }
                else -> throw IllegalStateException("This should not happen")
            }
        } ?: this
    }

    private val DriveLink.nameKey: String
        get() = "name.${parentId?.id}.$nameHash"

    private val DriveLink.xAttrKey: String
        get() = "xattr.${parentId?.id}.${sha256(requireNotNull(xAttr), Sha256.OutputFormat.BASE_64).getOrNull()}"
}
