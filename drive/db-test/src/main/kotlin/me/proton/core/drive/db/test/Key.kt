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
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.entity.key.KeyId
import me.proton.core.user.data.entity.AddressEntity
import me.proton.core.user.data.entity.AddressKeyEntity
import me.proton.core.user.data.entity.UserKeyEntity
import me.proton.core.user.domain.entity.AddressId

suspend fun UserContext.withKey(
    userKey: UserKeyEntity = NullableUserKeyEntity(),
    address: AddressEntity = NullableAddressEntity(),
    addressKey: AddressKeyEntity = NullableAddressKeyEntity(),
) {
    db.userKeyDao().insertOrUpdate(userKey)
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
    addressId: AddressId = AddressId("address-id"),
) = NullableAddressEntity(
    userId = user.userId,
    addressId = addressId,
    email = requireNotNull(account.email),
    displayName = account.username,
)

fun NullableAddressEntity(
    userId: UserId,
    addressId: AddressId = AddressId("address-id"),
    email: String,
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

fun NullableAddressKeyEntity(
    addressId: AddressId = AddressId("address-id"),
    keyId: KeyId = KeyId("key-id"),
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
