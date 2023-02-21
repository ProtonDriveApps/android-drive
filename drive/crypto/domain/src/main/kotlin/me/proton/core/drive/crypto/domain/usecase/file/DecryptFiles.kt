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
package me.proton.core.drive.crypto.domain.usecase.file

import me.proton.core.crypto.common.pgp.DecryptedFile
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.base.UseSessionKey
import me.proton.core.drive.cryptobase.domain.usecase.DecryptFile
import me.proton.core.drive.key.domain.entity.ContentKey
import java.io.File
import javax.inject.Inject

class DecryptFiles @Inject constructor(
    private val useSessionKey: UseSessionKey,
    private val decryptFile: DecryptFile
) {
    suspend operator fun invoke(
        contentKey: ContentKey,
        input: List<File>,
        output: List<File>,
    ): Result<List<DecryptedFile>> = coRunCatching {
        useSessionKey(contentKey = contentKey) { sessionKey ->
            input.mapIndexed { index, file ->
                DecryptedFile(
                    file = decryptFile(sessionKey, file, output[index]).getOrThrow(),
                    status = VerificationStatus.Unknown,
                    filename = "",
                    lastModifiedEpochSeconds = -1,
                )
            }
        }.getOrThrow()
    }
}
