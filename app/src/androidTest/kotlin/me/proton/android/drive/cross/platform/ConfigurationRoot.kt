/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.cross.platform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ConfigurationRoot(
    @SerialName("xTestTitle")
    val title: String,
    @SerialName("user")
    val user: User,
    @SerialName("cTestInfo")
    val info: Info,
)

@Serializable
data class User(
    @SerialName("username")
    val username: String,
    @SerialName("password")
    val password: String,
    @SerialName("email")
    val email: String,
)

@Serializable
data class Info(
    @SerialName("cTest")
    val id: String,
    @SerialName("args")
    val args: JsonObject,
)
