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

import me.proton.core.drive.base.domain.extension.FORBIDDEN_CHARS_REGEX
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import javax.inject.Inject

class ValidateLinkName @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
) {
    operator fun invoke(name: String): Result<String> {
        val trimmedName = name.trim()
        return when {
            setOf(".", "..").contains(trimmedName) -> Result.failure(Invalid.Periods)
            FORBIDDEN_CHARS_REGEX.containsMatchIn(trimmedName) -> Result.failure(Invalid.ForbiddenCharacters)
            trimmedName.isEmpty() -> Result.failure(Invalid.Empty)
            trimmedName.length > configurationProvider.linkMaxNameLength -> Result.failure(Invalid.ExceedsMaxLength(configurationProvider.linkMaxNameLength))
            else -> Result.success(trimmedName)
        }
    }

    sealed class Invalid : Throwable() {
        object Empty : Invalid()
        data class ExceedsMaxLength(val maxLength: Int) : Invalid()
        object Periods : Invalid()
        object ForbiddenCharacters : Invalid()
    }
}
