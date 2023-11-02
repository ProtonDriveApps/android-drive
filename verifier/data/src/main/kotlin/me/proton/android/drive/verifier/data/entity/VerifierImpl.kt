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

package me.proton.android.drive.verifier.data.entity

import me.proton.android.drive.verifier.domain.exception.VerifierException
import me.proton.android.drive.verifier.data.extension.head
import me.proton.android.drive.verifier.data.extension.xor
import me.proton.android.drive.verifier.domain.entity.Verifier
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.crypto.domain.usecase.file.DecryptFiles
import me.proton.core.drive.key.domain.entity.ContentKey
import java.io.File
import java.util.UUID

internal class VerifierImpl constructor(
    private val decryptFiles: DecryptFiles,
    private val contentKey: ContentKey,
    private val verificationCode: ByteArray,
    private val tempFolder: File
) : Verifier {

    init {
        require(verificationCode.size == VERIFICATION_CODE_SIZE) {
            "Invalid verification code size"
        }
    }

    override suspend fun verifyBlocks(blocks: List<File>): Result<Map<File, ByteArray>> =
        try {
            require(blocks.isNotEmpty()) { "Input blocks list is empty" }
            require(blocks.all { block -> block.exists() }) { "Input block does not exist" }
            verifyByDecryptingBlocks(blocks)
            Result.success(
                blocks.associateBy(
                    keySelector = { file -> file }
                ) { file -> verificationCode.xor(file.head(len = verificationCode.size.bytes))}
            )
        } catch (t: Throwable) {
            Result.failure(VerifierException.VerifyBlock(t))
        }

    private suspend fun verifyByDecryptingBlocks(blocks: List<File>) {
        val output = blocks.map { block ->
            File(block.destinationFolder(), UUID.randomUUID().toString()).apply {
                parentFile?.mkdirs()
                createNewFile()
            }
        }
        val result = decryptFiles(
            contentKey = contentKey,
            input = blocks,
            output = output,
        )
        output.forEach { file -> file.delete() }
        result.getOrThrow()
    }

    private fun File.destinationFolder() = parent ?: tempFolder.path

    companion object {
        private const val VERIFICATION_CODE_SIZE = 32
    }
}
