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
package me.proton.core.drive.link.domain.usecase

import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ValidateLinkNameTest(
    private val name: String,
    private val expected: Any,
) {
    private val configurationProvider = object : ConfigurationProvider {
        override val host = ""
        override val baseUrl = ""
        override val appVersionHeader = ""
        override val linkMaxNameLength = LINK_MAX_NAME_LENGTH
    }
    private val validateLinkName = ValidateLinkName(configurationProvider)

    @Test
    fun `test valid and invalid file or folder names`() {
        // due to issue with Parametrized conversion between data and expected when dealing with kotlin Result we
        // need to cast here to get the proper Result
        val expectedResult = expected as Result<*>
        assertEquals(expectedResult, validateLinkName(name))
    }

    companion object {
        const val LINK_MAX_NAME_LENGTH = 255

        @get:Parameterized.Parameters
        @get:JvmStatic
        @Suppress("unused")
        val data = listOf(
            // region invalid file or folder names
            arrayOf(".", Result.failure<String>(ValidateLinkName.Invalid.Periods)),
            arrayOf(" . ", Result.failure<String>(ValidateLinkName.Invalid.Periods)),
            arrayOf("\n.", Result.failure<String>(ValidateLinkName.Invalid.Periods)),
            arrayOf("..", Result.failure<String>(ValidateLinkName.Invalid.Periods)),
            arrayOf(" ..", Result.failure<String>(ValidateLinkName.Invalid.Periods)),
            arrayOf(".. ", Result.failure<String>(ValidateLinkName.Invalid.Periods)),
            arrayOf("\t..\t", Result.failure<String>(ValidateLinkName.Invalid.Periods)),
            arrayOf("", Result.failure<String>(ValidateLinkName.Invalid.Empty)),
            arrayOf(" ", Result.failure<String>(ValidateLinkName.Invalid.Empty)),
            arrayOf("\t", Result.failure<String>(ValidateLinkName.Invalid.Empty)),
            arrayOf("\n", Result.failure<String>(ValidateLinkName.Invalid.Empty)),
            arrayOf("a/b", Result.failure<String>(ValidateLinkName.Invalid.ForbiddenCharacters)),
            arrayOf(" /ab", Result.failure<String>(ValidateLinkName.Invalid.ForbiddenCharacters)),
            arrayOf("ab/ ", Result.failure<String>(ValidateLinkName.Invalid.ForbiddenCharacters)),
            arrayOf("\\test/", Result.failure<String>(ValidateLinkName.Invalid.ForbiddenCharacters)),
            arrayOf(
                (1..LINK_MAX_NAME_LENGTH + 1).map { '.' }.joinToString(""),
                Result.failure<String>(ValidateLinkName.Invalid.ExceedsMaxLength(LINK_MAX_NAME_LENGTH))
            ),
            // endregion
            // region valid file or folder names
            arrayOf("drive.txt", Result.success("drive.txt")),
            arrayOf("drive txt", Result.success("drive txt")),
            arrayOf("\tdrive.txt ", Result.success("drive.txt")),
            arrayOf("<drive>", Result.success("<drive>")),
            arrayOf(
                (1..LINK_MAX_NAME_LENGTH).map { '.' }.joinToString(""),
                Result.success((1..LINK_MAX_NAME_LENGTH).map { '.' }.joinToString(""))
            ),
            // endregion
        )
    }
}
