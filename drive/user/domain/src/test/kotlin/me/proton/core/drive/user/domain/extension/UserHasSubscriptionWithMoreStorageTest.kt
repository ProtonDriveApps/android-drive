/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.user.domain.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.Type
import me.proton.core.user.domain.entity.User
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UserHasSubscriptionWithMoreStorageTest(
    private val subscribed: Int,
    private val moreStorage: Boolean,
) {
    @Test
    fun test() {
        assertEquals(moreStorage, user.copy(subscribed = subscribed).hasSubscriptionWithMoreStorage)
    }

    companion object {
        private val user = User(
            userId = UserId("id"),
            email = "private.adam.smith.email@protonmail.com",
            name = "Smith",
            displayName = "Adam Smith",
            currency = "€",
            credit = 0,
            createdAtUtc = 0,
            usedSpace = 242_221_056L,
            maxSpace = 2_147_483_648L,
            maxUpload = 0,
            role = null,
            private = false,
            services = 0,
            subscribed = 0,
            delinquent = null,
            keys = emptyList(),
            recovery = null,
            type = Type.Proton,
            flags = emptyMap(),
        )

        private const val MASK_NONE = 0 // 0000
        private const val MASK_MAIL = 1 // 0001
        private const val MASK_DRIVE = 2 // 0010
        private const val MASK_VPN = 4 // 0100

        @get:Parameterized.Parameters(name ="With subscribed of {0} hasSubscriptionWithMoreStorage is {1}")
        @get:JvmStatic
        val data = listOf(
            arrayOf(MASK_NONE, false),
            arrayOf(MASK_MAIL, true),
            arrayOf(MASK_DRIVE, true),
            arrayOf(MASK_MAIL or MASK_DRIVE, true),
            arrayOf(MASK_VPN, false),
        )
    }
}
