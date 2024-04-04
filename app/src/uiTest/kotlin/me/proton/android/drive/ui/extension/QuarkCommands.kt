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

import me.proton.core.test.quark.data.User
import me.proton.core.test.quark.v2.QuarkCommand
import me.proton.core.test.quark.v2.toEncodedArgs
import okhttp3.Response

fun QuarkCommand.populate(
    user: User,
    isDevice: Boolean = false,
    isPhotos: Boolean = false
): Response =
    route("quark/drive:populate")
        .args(
            listOfNotNull(
                "-u" to user.name,
                "-p" to user.password,
                "-S" to user.dataSetScenario,
                isDevice.optionalArg("-d"),
                isPhotos.optionalArg("--photo"),
            ).toEncodedArgs()
        )
        .build()
        .let {
            client.executeQuarkRequest(it)
        }

private fun Boolean.optionalArg(
    name: String,
): Pair<String, String>? = takeIf { it }?.let { name to it.toString() }

fun QuarkCommand.quotaSetUsedSpace(
    user: User,
    usedSpace: String,
): Response =
    route("quark/drive:quota:set-used-space")
        .args(
            listOf(
                "--username" to user.name,
                "--used-space" to usedSpace
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
