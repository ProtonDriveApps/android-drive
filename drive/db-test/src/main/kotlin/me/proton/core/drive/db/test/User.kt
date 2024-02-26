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

package me.proton.core.drive.db.test

import me.proton.android.drive.db.DriveDatabase
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.domain.entity.UserId
import me.proton.core.user.data.entity.UserEntity

data class UserContext(
    val db: DriveDatabase,
    val user: UserEntity,
    val account: AccountEntity,
) : BaseContext()

suspend fun DriveDatabase.user(
    user: UserEntity = NullableUserEntity(),
    block: suspend UserContext.() -> Unit,
) {
    val account = AccountEntity(
        userId = user.userId,
        username = user.userId.id,
        email = "${user.userId.id}@proton.test",
        state = AccountState.Ready,
        sessionId = null,
        sessionState = null
    )
    accountDao().insertOrUpdate(
        account
    )
    userDao().insertOrUpdate(user)
    UserContext(this, user, account).block()
}

val userId = UserId("user-id")

@Suppress("FunctionName")
fun NullableUserEntity(
    userId: UserId = me.proton.core.drive.db.test.userId,
    maxSpace: Long = 0,
) = UserEntity(
    userId = userId,
    email = null,
    name = null,
    displayName = null,
    currency = "EUR",
    credit = 0,
    createdAtUtc = 0,
    usedSpace = 0,
    maxSpace = maxSpace,
    maxUpload = 0,
    role = null,
    isPrivate = false,
    subscribed = 0,
    services = 0,
    delinquent = null,
    passphrase = EncryptedByteArray("user-passphrase".toByteArray()),
    recovery = null,
    maxBaseSpace = null,
    maxDriveSpace = null,
    usedBaseSpace = null,
    usedDriveSpace = null,
)
