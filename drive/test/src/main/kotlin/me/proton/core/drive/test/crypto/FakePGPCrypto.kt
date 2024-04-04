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

package me.proton.core.drive.test.crypto

import android.util.Base64
import me.proton.core.crypto.common.pgp.Armored
import me.proton.core.crypto.common.pgp.DataPacket
import me.proton.core.crypto.common.pgp.DecryptedData
import me.proton.core.crypto.common.pgp.DecryptedFile
import me.proton.core.crypto.common.pgp.DecryptedMimeBody
import me.proton.core.crypto.common.pgp.DecryptedMimeMessage
import me.proton.core.crypto.common.pgp.DecryptedText
import me.proton.core.crypto.common.pgp.EncryptedFile
import me.proton.core.crypto.common.pgp.EncryptedMessage
import me.proton.core.crypto.common.pgp.EncryptedPacket
import me.proton.core.crypto.common.pgp.EncryptedSignature
import me.proton.core.crypto.common.pgp.HashKey
import me.proton.core.crypto.common.pgp.KeyPacket
import me.proton.core.crypto.common.pgp.PGPCrypto
import me.proton.core.crypto.common.pgp.PGPHeader
import me.proton.core.crypto.common.pgp.PacketType
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.crypto.common.pgp.Signature
import me.proton.core.crypto.common.pgp.SignatureContext
import me.proton.core.crypto.common.pgp.Unarmored
import me.proton.core.crypto.common.pgp.UnlockedKey
import me.proton.core.crypto.common.pgp.VerificationContext
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.crypto.common.pgp.VerificationTime
import java.io.File

class FakePGPCrypto : PGPCrypto {
    override fun decryptAndVerifyData(
        data: DataPacket,
        sessionKey: SessionKey,
        publicKeys: List<Armored>,
        time: VerificationTime,
        verificationContext: VerificationContext?,
    ): DecryptedData {
        return DecryptedData(data, VerificationStatus.Success)
    }

    override fun decryptAndVerifyData(
        message: EncryptedMessage,
        publicKeys: List<Armored>,
        unlockedKeys: List<Unarmored>,
        time: VerificationTime,
        verificationContext: VerificationContext?,
    ): DecryptedData {
        return DecryptedData(message.toByteArray(), VerificationStatus.Success)
    }

    override fun decryptAndVerifyFile(
        source: EncryptedFile,
        destination: File,
        sessionKey: SessionKey,
        publicKeys: List<Armored>,
        time: VerificationTime,
        verificationContext: VerificationContext?,
    ): DecryptedFile {
        return DecryptedFile(source, VerificationStatus.Success, source.name, 0L)
    }

    override fun decryptAndVerifyMimeMessage(
        message: EncryptedMessage,
        publicKeys: List<Armored>,
        unlockedKeys: List<Unarmored>,
        time: VerificationTime,
    ): DecryptedMimeMessage {
        return DecryptedMimeMessage(
            headers = emptyList(),
            DecryptedMimeBody("", message),
            attachments = emptyList(),
            verificationStatus = VerificationStatus.Success
        )
    }

    override fun decryptAndVerifyText(
        message: EncryptedMessage,
        publicKeys: List<Armored>,
        unlockedKeys: List<Unarmored>,
        time: VerificationTime,
        verificationContext: VerificationContext?,
    ): DecryptedText {
        return DecryptedText(
            text = message,
            status = VerificationStatus.Success,
        )
    }

    override fun decryptData(data: DataPacket, sessionKey: SessionKey): ByteArray {
        return data
    }

    override fun decryptData(message: EncryptedMessage, unlockedKey: Unarmored): ByteArray {
        return message.toByteArray()
    }

    override fun decryptFile(
        source: EncryptedFile,
        destination: File,
        sessionKey: SessionKey,
    ): DecryptedFile {
        source.copyTo(destination)
        return DecryptedFile(
            file = destination,
            status = VerificationStatus.Success,
            filename = source.name,
            lastModifiedEpochSeconds = 0L
        )
    }

    override fun decryptMimeMessage(
        message: EncryptedMessage,
        unlockedKeys: List<Unarmored>,
    ): DecryptedMimeMessage {
        return DecryptedMimeMessage(
            headers = emptyList(),
            body = DecryptedMimeBody(
                "",
                message,
            ),
            attachments = emptyList(),
            verificationStatus = VerificationStatus.Success
        )
    }

    override fun decryptSessionKey(keyPacket: KeyPacket, unlockedKey: Unarmored): SessionKey {
        return SessionKey(keyPacket)
    }

    override fun decryptSessionKeyWithPassword(
        keyPacket: KeyPacket,
        password: ByteArray,
    ): SessionKey {
        return SessionKey(keyPacket)
    }

    override fun decryptText(message: EncryptedMessage, unlockedKey: Unarmored): String {
        return message
    }

    override fun decryptTextWithPassword(message: EncryptedMessage, password: ByteArray): String {
        return message
    }

    override fun encryptAndSignData(
        data: ByteArray,
        publicKey: Armored,
        unlockedKey: Unarmored,
        signatureContext: SignatureContext?,
    ): EncryptedMessage {
        return String(data)
    }

    override fun encryptAndSignData(
        data: ByteArray,
        sessionKey: SessionKey,
        unlockedKey: Unarmored,
        signatureContext: SignatureContext?,
    ): DataPacket {
        return data
    }

    override fun encryptAndSignDataWithCompression(
        data: ByteArray,
        publicKey: Armored,
        unlockedKey: Unarmored,
        signatureContext: SignatureContext?,
    ): EncryptedMessage {
        return String(data)
    }

    override fun encryptAndSignFile(
        source: File,
        destination: File,
        sessionKey: SessionKey,
        unlockedKey: Unarmored,
        signatureContext: SignatureContext?,
    ): EncryptedFile {
        source.copyTo(destination)
        return destination
    }

    override fun encryptAndSignText(
        plainText: String,
        publicKey: Armored,
        unlockedKey: Unarmored,
        signatureContext: SignatureContext?,
    ): EncryptedMessage {
        return plainText
    }

    override fun encryptAndSignTextWithCompression(
        plainText: String,
        publicKey: Armored,
        unlockedKey: Unarmored,
        signatureContext: SignatureContext?,
    ): EncryptedMessage {
        return plainText
    }

    override fun encryptData(data: ByteArray, publicKey: Armored): EncryptedMessage {
        return String(data)
    }

    override fun encryptData(data: ByteArray, sessionKey: SessionKey): DataPacket {
        return data
    }

    override fun encryptFile(
        source: File,
        destination: File,
        sessionKey: SessionKey,
    ): EncryptedFile {
        source.copyTo(destination)
        return destination
    }

    override fun encryptSessionKey(sessionKey: SessionKey, publicKey: Armored): KeyPacket {
        return sessionKey.key
    }

    override fun encryptSessionKeyWithPassword(
        sessionKey: SessionKey,
        password: ByteArray,
    ): KeyPacket {
        return sessionKey.key
    }

    override fun encryptMessageToAdditionalKey(
        message: EncryptedMessage,
        unlockedKey: Unarmored,
        publicKey: Armored
    ): EncryptedMessage {
        return message
    }

    override fun encryptText(plainText: String, publicKey: Armored): EncryptedMessage {
        return plainText
    }

    override fun encryptTextWithPassword(text: String, password: ByteArray): EncryptedMessage {
        return text
    }

    override fun generateNewHashKey(): HashKey {
        return HashKey(key = "hash-key".toByteArray(), VerificationStatus.Success)
    }

    override fun generateNewKeySalt(): String {
        return "key-salt"
    }

    override fun generateNewPrivateKey(
        username: String,
        domain: String,
        passphrase: ByteArray,
    ): Armored {
        return String(passphrase)
    }

    override fun generateNewSessionKey(): SessionKey {
        return SessionKey("session-key".toByteArray())
    }

    override fun generateNewToken(size: Long): ByteArray {
        return ByteArray(size.toInt())
    }

    override fun generateRandomBytes(size: Long): ByteArray {
        return ByteArray(size.toInt())
    }

    override fun getArmored(data: Unarmored, header: PGPHeader): Armored {
        return data.toString()
    }

    override fun getBase64Decoded(string: String): ByteArray {
        return Base64.decode(string, Base64.NO_WRAP)
    }

    override fun getBase64Encoded(array: ByteArray): String {
        return Base64.encode(array, Base64.NO_WRAP).toString()
    }

    override suspend fun getCurrentTime(): Long {
        return System.currentTimeMillis()
    }

    override fun getEncryptedPackets(message: EncryptedMessage): List<EncryptedPacket> {
        return listOf(EncryptedPacket(message.toByteArray(), PacketType.Data))
    }

    override fun getFingerprint(key: Armored): String {
        return "fingerprint:$key"
    }

    override fun getJsonSHA256Fingerprints(key: Armored): String {
        return "json_sha256_fingerprint:$key"
    }

    override fun getPassphrase(password: ByteArray, encodedSalt: String): ByteArray {
        return password
    }

    override fun getPublicKey(privateKey: Armored): Armored {
        return privateKey
    }

    override fun getUnarmored(data: Armored): Unarmored {
        return data.toByteArray()
    }

    override fun getVerifiedTimestampOfData(
        data: ByteArray,
        signature: Armored,
        publicKey: Armored,
        time: VerificationTime,
        verificationContext: VerificationContext?,
    ): Long? {
        return when(time) {
            VerificationTime.Ignore -> null
            VerificationTime.Now -> System.currentTimeMillis() / 1000
            is VerificationTime.Utc -> time.seconds
        }
    }

    override fun getVerifiedTimestampOfText(
        plainText: String,
        signature: Armored,
        publicKey: Armored,
        time: VerificationTime,
        trimTrailingSpaces: Boolean,
        verificationContext: VerificationContext?,
    ): Long? {
        return when(time) {
            VerificationTime.Ignore -> null
            VerificationTime.Now -> System.currentTimeMillis() / 1000
            is VerificationTime.Utc -> time.seconds
        }
    }

    override fun isKeyExpired(key: Armored): Boolean {
        return false
    }

    override fun isKeyRevoked(key: Armored): Boolean {
        return false
    }

    override fun isPrivateKey(key: Armored): Boolean {
        return "private" in key
    }

    override fun isPublicKey(key: Armored): Boolean {
        return "public" in key
    }

    override fun isValidKey(key: Armored): Boolean {
        return "valid" in key
    }

    override fun lock(unlockedKey: Unarmored, passphrase: ByteArray): Armored {
        return unlockedKey.toString()
    }

    override fun signData(
        data: ByteArray,
        unlockedKey: Unarmored,
        signatureContext: SignatureContext?,
    ): Signature {
        return "signature:$data"
    }

    override fun signDataEncrypted(
        data: ByteArray,
        unlockedKey: Unarmored,
        encryptionKeys: List<Armored>,
        signatureContext: SignatureContext?,
    ): EncryptedSignature {
        return "signature:$data"
    }

    override fun signFile(
        file: File,
        unlockedKey: Unarmored,
        signatureContext: SignatureContext?,
    ): Signature {
        return "signature:${file.name}"
    }

    override fun signFileEncrypted(
        file: File,
        unlockedKey: Unarmored,
        encryptionKeys: List<Armored>,
        signatureContext: SignatureContext?,
    ): EncryptedSignature {
        return "signature:${file.name}"
    }

    override fun signText(
        plainText: String,
        unlockedKey: Unarmored,
        trimTrailingSpaces: Boolean,
        signatureContext: SignatureContext?,
    ): Signature {
        return "signature:$plainText"
    }

    override fun signTextEncrypted(
        plainText: String,
        unlockedKey: Unarmored,
        encryptionKeys: List<Armored>,
        trimTrailingSpaces: Boolean,
        signatureContext: SignatureContext?,
    ): EncryptedSignature {
        return "signature:$plainText"
    }

    override fun unlock(privateKey: Armored, passphrase: ByteArray): UnlockedKey {
        return object : UnlockedKey{
            override val value: Unarmored
                get() = privateKey.toByteArray()

            override fun close() {
                // do nothing
            }

        }
    }

    override fun updatePrivateKeyPassphrase(
        privateKey: Armored,
        passphrase: ByteArray,
        newPassphrase: ByteArray,
    ): Armored {
       return privateKey
    }

    override fun updateTime(epochSeconds: Long) {
        // nothing
    }

    override fun verifyData(
        data: ByteArray,
        signature: Armored,
        publicKey: Armored,
        time: VerificationTime,
        verificationContext: VerificationContext?,
    ): Boolean {
        return true
    }

    override fun verifyDataEncrypted(
        data: ByteArray,
        encryptedSignature: EncryptedSignature,
        privateKey: Unarmored,
        publicKeys: List<Armored>,
        time: VerificationTime,
        verificationContext: VerificationContext?,
    ): Boolean {
        return true
    }

    override fun verifyFile(
        file: DecryptedFile,
        signature: Armored,
        publicKey: Armored,
        time: VerificationTime,
        verificationContext: VerificationContext?,
    ): Boolean {
        return true
    }

    override fun verifyFileEncrypted(
        file: File,
        encryptedSignature: EncryptedSignature,
        privateKey: Unarmored,
        publicKeys: List<Armored>,
        time: VerificationTime,
        verificationContext: VerificationContext?,
    ): Boolean {
        return true
    }

    override fun verifyText(
        plainText: String,
        signature: Armored,
        publicKey: Armored,
        time: VerificationTime,
        trimTrailingSpaces: Boolean,
        verificationContext: VerificationContext?,
    ): Boolean {
        return true
    }

    override fun verifyTextEncrypted(
        plainText: String,
        encryptedSignature: EncryptedSignature,
        privateKey: Unarmored,
        publicKeys: List<Armored>,
        time: VerificationTime,
        trimTrailingSpaces: Boolean,
        verificationContext: VerificationContext?,
    ): Boolean {
        return true
    }
}
