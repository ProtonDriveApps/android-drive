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
package me.proton.core.drive.notification.domain.entity

import kotlinx.serialization.Serializable

@Serializable
sealed class NotificationId {
    abstract val channel: Channel
    abstract val tag: String?
    abstract val id: Int

    @Serializable
    data class User(
        override val channel: Channel.User,
        override val tag: String?,
        override val id: Int,
    ) : NotificationId()

    @Serializable
    data class App(
        override val channel: Channel.App,
        override val tag: String?,
        override val id: Int,
    ) : NotificationId()
}
