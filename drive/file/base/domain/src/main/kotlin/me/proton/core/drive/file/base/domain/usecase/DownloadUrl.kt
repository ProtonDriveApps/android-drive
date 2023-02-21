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
package me.proton.core.drive.file.base.domain.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.coroutines.FileScope
import me.proton.core.drive.file.base.domain.extension.saveToFile
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class DownloadUrl @Inject constructor(
    private val getUrlInputStream: GetUrlInputStream,
) {
    suspend operator fun invoke(
        userId: UserId,
        url: String,
        destination: File,
        progress: MutableStateFlow<Long>,
        isCancelled: () -> Boolean = { false },
        coroutineContext: CoroutineContext = FileScope.coroutineContext,
    ): Result<File> = coRunCatching(coroutineContext) {
            getUrlInputStream(userId, url)
                .getOrThrow()
                .saveToFile(
                    file = destination,
                    mutableStateFlow = progress,
                    isCancelled = isCancelled,
                )
            destination
        }
}
