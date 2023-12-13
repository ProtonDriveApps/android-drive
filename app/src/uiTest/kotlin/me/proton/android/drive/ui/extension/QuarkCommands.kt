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

package me.proton.android.drive.ui.extension

import me.proton.core.test.quark.data.User
import me.proton.core.test.quark.v2.QuarkCommand
import me.proton.core.test.quark.v2.toEncodedArgs
import me.proton.core.util.kotlin.EMPTY_STRING
import okhttp3.OkHttpClient
import okhttp3.Response
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun QuarkCommand.populate(
    user: User,
    isDevice: Boolean = false,
    isPhotos: Boolean = false
): Response =
    route("quark/drive:populate")
        .args(
            listOf(
                "-u" to user.name,
                "-p" to user.password,
                "-S" to user.dataSetScenario,
                "--d" to if (isDevice) true.toString() else EMPTY_STRING,
                "--photo" to if (isPhotos) true.toString() else EMPTY_STRING
            ).toEncodedArgs()
        )
        .build()
        .let {
            client.executeQuarkRequest(it)
        }

val quarkClient by lazy {
    val timeout = 60.seconds.toJavaDuration()
    OkHttpClient
        .Builder()
        .connectTimeout(timeout)
        .readTimeout(timeout)
        .writeTimeout(timeout)
        .build()
}