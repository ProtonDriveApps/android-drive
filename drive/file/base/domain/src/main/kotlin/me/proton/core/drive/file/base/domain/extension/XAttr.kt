/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.file.base.domain.extension

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.proton.core.drive.file.base.domain.entity.XAttr
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val json = Json {
    ignoreUnknownKeys = true
}

fun String.toXAttr(): Result<XAttr> = try {
    Result.success(json.decodeFromString(this))
} catch (e: SerializationException) {
    Result.failure(e)
}

fun XAttr.asJson(): String = json.encodeToString(XAttr.serializer(), this)

val XAttr.mediaDuration: Duration?
    get() = media?.duration?.seconds
