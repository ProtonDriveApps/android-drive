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
package me.proton.core.drive.upload.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.repository.FileRepository
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.usecase.UpdateToken
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.linkupload.domain.manager.UploadSpeedManager
import javax.inject.Inject

class UploadBlock @Inject constructor(
    private val fileRepository: FileRepository,
    private val updateToken: UpdateToken,
    private val updateUploadState: UpdateUploadState,
    private val uploadSpeedManager: UploadSpeedManager,
) {
    suspend operator fun invoke(
        uploadFileLink: UploadFileLink,
        uploadBlock: me.proton.core.drive.linkupload.domain.entity.UploadBlock,
        url: String,
        token: String,
        uploadingProgress: MutableStateFlow<Long>,
    ): Result<me.proton.core.drive.linkupload.domain.entity.UploadBlock> = coRunCatching {
        with(uploadFileLink) {
            updateUploadState(id, UploadState.UPLOADING_BLOCKS).getOrThrow()
            try {
                require(uploadBlock.file.length() != 0L) {
                    "Cannot send empty block: ${uploadBlock.file.name} for file link: ${uploadFileLink.id}"
                }
                val job = Job()
                val scope = CoroutineScope(Dispatchers.IO + job)
                uploadingProgress.scan(Pair(0L, 0L)) { accumulator, value ->
                    accumulator.second to value
                }.drop(1)
                    .onEach{ (prev, next) ->
                        uploadSpeedManager.add(uploadFileLink.userId, next - prev)
                    }.launchIn(scope)
                fileRepository.uploadFile(
                    userId = userId,
                    uploadUrl = url,
                    uploadFile = uploadBlock.file,
                    uploadingProgress = uploadingProgress
                ).getOrThrow()
                job.complete()
                updateToken(
                    uploadFileLinkId = id,
                    index = uploadBlock.index,
                    uploadToken = token,
                ).getOrThrow()
            } catch (e: Throwable) {
                updateUploadState(id, UploadState.IDLE)
                throw e
            }
        }
    }
}
