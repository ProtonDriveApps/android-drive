/*
 * Copyright (c) 2023-2024 Proton AG.
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
@file:Suppress("MatchingDeclarationName")
package me.proton.core.drive.db.test

import me.proton.android.drive.db.DriveDatabase
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.GiB
import me.proton.core.key.domain.entity.key.KeyId
import me.proton.core.user.data.entity.UserEntity
import me.proton.core.user.domain.entity.AddressId


data class UserContext(
    val db: DriveDatabase,
    val user: UserEntity,
    val account: AccountEntity,
) : BaseContext()

suspend fun <T> DriveDatabase.user(
    user: UserEntity = NullableUserEntity(),
    block: suspend UserContext.() -> T,
): T {
    val account = AccountEntity(
        userId = user.userId,
        username = user.userId.id,
        email = user.email,
        state = AccountState.Ready,
        sessionId = null,
        sessionState = null
    )
    accountDao().insertOrUpdate(
        account
    )
    userDao().insertOrUpdate(user)
    return UserContext(this, user, account).block()
}

val userId = UserId("user-id")

@Suppress("FunctionName, LongParameterList")
fun NullableUserEntity(
    userId: UserId = me.proton.core.drive.db.test.userId,
    maxSpace: Long = 5.GiB.value,
    usedSpace: Long = 0,
    maxDriveSpace: Long? = null,
    usedDriveSpace: Long? = null,
    subscribed: Int = 0,
    email: String = "${userId.id}@proton.test"
) = UserEntity(
    userId = userId,
    email = email,
    name = null,
    displayName = null,
    currency = "EUR",
    type = 0,
    credit = 0,
    createdAtUtc = 0,
    usedSpace = usedSpace,
    maxSpace = maxSpace,
    maxUpload = 0,
    role = null,
    isPrivate = false,
    subscribed = subscribed,
    services = 0,
    delinquent = null,
    passphrase = EncryptedByteArray("user-passphrase".toByteArray()),
    recovery = null,
    maxBaseSpace = null,
    maxDriveSpace = maxDriveSpace,
    usedBaseSpace = null,
    usedDriveSpace = usedDriveSpace,
    flags = null
)

suspend fun UserContext.addPrimaryAddress(email: String) {
    addAddress(email)
    db.userDao().update(this.user.copy(email = email))
}

suspend fun UserContext.addAddress(email: String) {
    val addressId = AddressId("address-id-$email")
    db.addressWithKeysDao().insertOrUpdate(
        NullableAddressEntity(
            addressId = addressId,
            email = email,
        )
    )
    db.addressKeyDao().insertOrUpdate(
        NullableAddressKeyEntity(
            addressId = addressId,
            keyId = KeyId("key-id-${addressId.id}"),
        )
    )
}
