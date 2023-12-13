/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.user.presentation.user

import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.User

import org.junit.Test

class UserKtTest {

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
    )

    @Test
    fun `first letter from display name before anything else`() {
        // region Arrange
        // endregion
        // region Act
        val firstLetter = user.firstLetter
        // endregion
        //region Assert
        assert(firstLetter == 'A')
        // endregion
    }

    @Test
    fun `first letter fallback to name if display name is empty`() {
        // region Arrange
        val user = user.copy(displayName = null)
        // endregion
        // region Act
        val firstLetter = user.firstLetter
        // endregion
        //region Assert
        assert(firstLetter == 'S')
        // endregion
    }

    @Test
    fun `first letter fallback to email if display name and name are empty`() {
        // region Arrange
        val user = user.copy(displayName = null, name = null)
        // endregion
        // region Act
        val firstLetter = user.firstLetter
        // endregion
        //region Assert
        assert(firstLetter == 'p')
        // endregion
    }

    @Test
    fun `first letter fallback to question mark if display name, name and email are empty`() {
        // region Arrange
        val user = user.copy(displayName = null, email = null, name = null)
        // endregion
        // region Act
        val firstLetter = user.firstLetter
        // endregion
        //region Assert
        assert(firstLetter == '?')
        // endregion
    }
}
