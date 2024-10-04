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

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.verifier.data.extension.createFile
import me.proton.android.drive.verifier.domain.exception.VerifierException
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.toHex
import me.proton.core.drive.crypto.domain.usecase.file.DecryptFiles
import me.proton.core.drive.key.domain.entity.ContentKey
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.usecase.BuildContentKey
import me.proton.core.test.kotlin.assertEquals
import me.proton.core.test.kotlin.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VerifierTest {
    private val buildContentKey = mockk<BuildContentKey>()
    private val decryptFiles = mockk<DecryptFiles>()
    private val fileKey = mockk<Key.Node>()
    private val contentKey = mockk<ContentKey>()
    private val verificationCode = ByteArray(VERIFICATION_CODE_SIZE) { i -> i.toByte() }
    private lateinit var verifier: VerifierImpl
    private lateinit var file64B: File
    private lateinit var file17B: File

    @get: Rule
    val temporaryFolder = TemporaryFolder()

    @Before
    fun before() {
        coEvery {
            buildContentKey(
                userId = any(),
                shareId = any(),
                contentKeyPacket = any(),
                contentKeyPacketSignature = any(),
                fileKey = fileKey,
            )
        } returns Result.success(contentKey)

        coEvery {
            decryptFiles(
                contentKey = contentKey,
                input = any(),
                output = any(),
                processDecryptedFile = any(),
            )
        } returns Result.success(emptyList())

        verifier = VerifierImpl(decryptFiles, contentKey, verificationCode, temporaryFolder.newFolder())
        file64B = temporaryFolder.createFile(64.bytes)
        file17B = temporaryFolder.createFile(17.bytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `VerifierImpl throws IllegalArgumentException if verificationCode with invalid size is given`() = runTest {
        // Given
        val verificationCode = ByteArray(31) { i -> i.toByte() }

        // Then
        VerifierImpl(decryptFiles, contentKey, verificationCode, temporaryFolder.newFolder())
    }

    @Test
    fun `verifyBlock when given empty block list throws VerifyBlock with cause IllegalArgumentException`() = runTest {
        // Given
        val blocks = emptyList<File>()

        // When
        val exception = verifier.verifyBlocks(blocks).exceptionOrNull()

        // Then
        assertNotNull(exception)
        assertTrue(exception is VerifierException.VerifyBlock) {
            "actual: $exception, expected: VerifyBlock"
        }
        assertTrue(exception?.cause is IllegalArgumentException) {
            "actual: ${exception?.cause}, expected: IllegalArgumentException"
        }
    }

    @Test
    fun `verifyBlock when given non-existing block file(s) throws VerifyBlock with cause IllegalArgumentException`() = runTest {
        // Given
        val nonExistingFile = File("", "test.txt")
        val blocks = listOf(nonExistingFile)

        // When
        val exception = verifier.verifyBlocks(blocks).exceptionOrNull()

        // Then
        assertNotNull(exception)
        assertTrue(exception is VerifierException.VerifyBlock) {
            "actual: $exception, expected: VerifyBlock"
        }
        assertTrue(exception?.cause is IllegalArgumentException) {
            "actual: ${exception?.cause}, expected: IllegalArgumentException"
        }
    }

    @Test
    fun `verifyBlock throws VerifyBlock when decrypt blocks fails`() = runTest {
        // Given
        coEvery {
            decryptFiles(
                contentKey = contentKey,
                input = any(),
                output = any(),
                processDecryptedFile = any(),
            )
        } returns Result.failure(CryptoException())

        // When
        val exception = verifier.verifyBlocks(listOf(file64B)).exceptionOrNull()

        // Then
        assertNotNull(exception)
        assertTrue(exception is VerifierException.VerifyBlock) {
            "actual: $exception, expected: VerifyBlock"
        }
        assertTrue(exception?.cause is CryptoException) {
            "actual: ${exception?.cause}, expected: CryptoException"
        }
    }

    @Test
    fun `successful dual block verification`() = runTest {
        // Given
        val blocks = listOf(file64B, file17B)

        // When
        val verifierTokens = verifier.verifyBlocks(blocks).getOrThrow()

        // Then
        assertEquals(blocks.size, verifierTokens.size) { "Verify blocks result size mismatch" }
        assertTrue(verifierTokens.containsKey(file64B)) { "File mismatch" }
        assertTrue(verifierTokens.containsKey(file17B)) { "File mismatch" }
        val verificationCodeXorHeaderOfFile64B = byteArrayOf(
            0x61, 0x60, 0x63, 0x62, 0x65, 0x64, 0x67, 0x66,
            0x69, 0x68, 0x6B, 0x6A, 0x6D, 0x6C, 0x6F, 0x6E,
            0x71, 0x70, 0x73, 0x72, 0x75, 0x74, 0x77, 0x76,
            0x79, 0x78, 0x7B, 0x7A, 0x7D, 0x7C, 0x7F, 0x7E
        )
        assertTrue(verifierTokens[file64B].contentEquals(verificationCodeXorHeaderOfFile64B)) {
            "Verifier token mismatch actual: ${verifierTokens[file64B]?.toHex()}, expected: ${verificationCodeXorHeaderOfFile64B.toHex()}"
        }
        val verificationCodeXorHeaderOfFile17B = byteArrayOf(
            0x61, 0x60, 0x63, 0x62, 0x65, 0x64, 0x67, 0x66,
            0x69, 0x68, 0x6B, 0x6A, 0x6D, 0x6C, 0x6F, 0x6E,
            0x71, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
            0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
        )
        assertTrue(verifierTokens[file17B].contentEquals(verificationCodeXorHeaderOfFile17B)) {
            "Verifier token mismatch actual: ${verifierTokens[file17B]?.toHex()}, expected: ${verificationCodeXorHeaderOfFile17B.toHex()}"
        }
    }

    companion object {
        private const val VERIFICATION_CODE_SIZE = 32 // bytes
    }
}
