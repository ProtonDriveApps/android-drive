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

@file:Suppress("FunctionName")

package me.proton.core.drive.db.test

import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.pgp.Armored
import me.proton.core.domain.entity.UserId
import me.proton.core.key.data.entity.PublicAddressInfoEntity
import me.proton.core.key.data.entity.PublicAddressKeyDataEntity
import me.proton.core.key.domain.entity.key.KeyId
import me.proton.core.user.data.entity.AddressEntity
import me.proton.core.user.data.entity.AddressKeyEntity
import me.proton.core.user.data.entity.UserKeyEntity
import me.proton.core.user.domain.entity.AddressId

suspend fun UserContext.withKey(
    userKey: UserKeyEntity = NullableUserKeyEntity(),
    address: AddressEntity = NullableAddressEntity(),
    addressKey: AddressKeyEntity = NullableAddressKeyEntity(),
    publicAddressInfoEntity: PublicAddressInfoEntity = NullablePublicAddressInfoEntity(),
    publicAddressKeyDataEntity: PublicAddressKeyDataEntity = NullablePublicAddressKeyDataEntity(),
) {
    db.userKeyDao().insertOrUpdate(userKey)
    db.addressWithKeysDao().insertOrUpdate(address)
    db.addressKeyDao().insertOrUpdate(addressKey)
    db.publicAddressInfoDao().insertOrUpdate(publicAddressInfoEntity)
    db.publicAddressKeyDataDao().insertOrUpdate(publicAddressKeyDataEntity)
}

suspend fun ShareContext.withKey(
    address: AddressEntity = NullableAddressEntity(),
    addressKey: AddressKeyEntity = NullableAddressKeyEntity(),
) {
    db.addressWithKeysDao().insertOrUpdate(address)
    db.addressKeyDao().insertOrUpdate(addressKey)
}

fun UserContext.NullableUserKeyEntity(
    keyId: KeyId = KeyId("user-key-id"),
) = NullableUserKeyEntity(user.userId, keyId)

fun NullableUserKeyEntity(
    userId: UserId,
    keyId: KeyId,
) = UserKeyEntity(
    userId = userId,
    keyId = keyId,
    version = 1,
    privateKey = "valid-user-private-key",
    isPrimary = true,
    isUnlockable = true,
    fingerprint = null,
    activation = null,
    active = true,
)

fun UserContext.NullableAddressEntity(
    addressId: AddressId = AddressId("address-id-${account.email}"),
    email: String = requireNotNull(account.email),
) = NullableAddressEntity(
    userId = user.userId,
    addressId = addressId,
    email = email,
    displayName = account.username,
)

fun ShareContext.NullableAddressEntity(
    addressId: AddressId = requireNotNull(share.addressId),
): AddressEntity = NullableAddressEntity(
    userId = user.userId,
    addressId = addressId,
    email = requireNotNull(account.email),
    displayName = account.username,
)

fun NullableAddressEntity(
    userId: UserId,
    email: String,
    addressId: AddressId = AddressId("address-id-$email"),
    displayName: String? = null,
) = AddressEntity(
    userId = userId,
    addressId = addressId,
    email = email,
    displayName = displayName,
    signature = null,
    domainId = null,
    canSend = true,
    canReceive = true,
    enabled = true,
    type = 0,
    order = 0,
    signedKeyList = null
)

fun ShareContext.NullableAddressKeyEntity(
    active: Boolean = true,
): AddressKeyEntity = NullableAddressKeyEntity(
    addressId = requireNotNull(share.addressId),
    keyId = KeyId("key-id-${share.addressId}"),
    active = active
)

fun UserContext.NullableAddressKeyEntity(
    active: Boolean = true,
): AddressKeyEntity = NullableAddressKeyEntity(
    addressId = AddressId("address-id-${account.email}"),
    keyId = KeyId("key-id-${account.email}"),
    active = active,
)

fun NullableAddressKeyEntity(
    addressId: AddressId,
    keyId: KeyId,
    active: Boolean = true,
): AddressKeyEntity {
    val token = (0..7).joinToString("") { "abcdef01" }
    return AddressKeyEntity(
        addressId = addressId,
        keyId = keyId,
        version = 1,
        privateKey = "valid-private-key",
        isPrimary = active,
        isUnlockable = active,
        flags = 3,
        passphrase = EncryptedByteArray(token.toByteArray()),
        active = active,
        token = token,
        signature = "signature",
        fingerprint = null,
        fingerprints = null,
        activation = null
    )
}

fun UserContext.NullablePublicAddressInfoEntity(
    email: String = account.email ?: "null-email",
) = PublicAddressInfoEntity(
    email = email,
    warnings = emptyList(),
    protonMx = false,
    isProton = 1,
    addressSignedKeyList = null,
    catchAllSignedKeyList = null,
)

fun UserContext.NullablePublicAddressKeyDataEntity(
    email: String = account.email ?: "null-email",
    publicKey: Armored = "public-key-$email",
    isPrimary: Boolean = true,
) = PublicAddressKeyDataEntity(
    email = email,
    emailAddressType = 0,
    flags = 3,
    publicKey = publicKey,
    isPrimary = isPrimary,
    source = null,
)
