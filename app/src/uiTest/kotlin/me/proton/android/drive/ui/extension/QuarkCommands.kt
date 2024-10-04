/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.ui.extension

import me.proton.core.test.quark.Quark.GenKeys
import me.proton.core.test.quark.data.User
import me.proton.core.test.quark.response.CreateUserAddressQuarkResponse
import me.proton.core.test.quark.v2.QuarkCommand
import me.proton.core.test.quark.v2.command.USERS_CREATE_ADDRESS
import me.proton.core.test.quark.v2.toEncodedArgs
import okhttp3.Response

fun QuarkCommand.populate(
    user: User,
    isDevice: Boolean = false,
    isPhotos: Boolean = false,
    sharingUser: User? = null
): Response =
    route("quark/drive:populate")
        .args(
            listOfNotNull(
                "-u" to user.name,
                "-p" to user.password,
                "-S" to user.dataSetScenario,
                isDevice.optionalArg("-d"),
                isPhotos.optionalArg("--photo"),
                sharingUser?.name?.optionalArg("--sharing-username"),
                sharingUser?.password?.optionalArg("--sharing-user-pass"),
            ).toEncodedArgs()
        )
        .build()
        .let {
            client.executeQuarkRequest(it)
        }

private fun Boolean.optionalArg(
    name: String,
): Pair<String, String>? = takeIf { it }?.let { name to it.toString() }

private fun String.optionalArg(
    name: String,
): Pair<String, String>? = takeIf { it.isNotEmpty() }?.let { name to it }

fun QuarkCommand.quotaSetUsedSpace(
    userId: String,
    usedSpace: String,
    product: String,
): Response =
    route("quark/drive:quota:set-used-space")
        .args(
            listOf(
                "--user-id" to userId,
                "--used-space" to usedSpace,
                "--product" to product,
            ).toEncodedArgs()
        )
        .build()
        .let {
            client.executeQuarkRequest(it)
        }

fun QuarkCommand.volumeCreate(
    user: User
): Response =
    route("quark/drive:volume:create")
        .args(
            listOf(
                "--username" to user.name,
                "--pass" to user.password,
            ).toEncodedArgs()
        )
        .build()
        .let {
            client.executeQuarkRequest(it)
        }

fun QuarkCommand.userCreatePrimaryAddress(
    decryptedUserId: Long,
    password: String,
    email: String,
    genKeys: GenKeys = GenKeys.Curve25519
): CreateUserAddressQuarkResponse =
    route(USERS_CREATE_ADDRESS)
        .args(
            listOf(
                "userID" to decryptedUserId.toString(),
                "password" to password,
                "email" to email,
                "--gen-keys" to genKeys.name,
                "--primary" to "true",
                "--format" to "json"
            ).toEncodedArgs()
        )
        .build()
        .let {
            client.executeQuarkRequest(it)
        }
        .let {
            json.decodeFromString(it.body!!.string())
        }

