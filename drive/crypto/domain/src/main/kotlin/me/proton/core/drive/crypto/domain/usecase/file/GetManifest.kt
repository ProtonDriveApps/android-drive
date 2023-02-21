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
package me.proton.core.drive.crypto.domain.usecase.file

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.coroutines.FileScope
import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.file.base.domain.extension.sha256
import me.proton.core.drive.linkupload.domain.entity.UploadBlock
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class GetManifest @Inject constructor() {
    suspend operator fun invoke(
        blocks: List<Block>,
        coroutineContext: CoroutineContext = FileScope.coroutineContext,
    ): Result<ByteArray> = coRunCatching(coroutineContext) {
        blocks.sortedBy { block -> block.index }.fold(byteArrayOf()) { sum, block ->
            sum + File(block.url).sha256
        }
    }

    @JvmName("uploadBlocks")
    suspend operator fun invoke(
        blocks: List<UploadBlock>,
        coroutineContext: CoroutineContext = FileScope.coroutineContext,
    ): Result<ByteArray> = coRunCatching(coroutineContext) {
        blocks.sortedBy { block -> block.index }.fold(byteArrayOf()) { sum, block ->
            sum + block.file.sha256
        }
    }
}
