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

package me.proton.core.drive.thumbnail.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.coroutines.unhandledErrorHandler
import me.proton.core.drive.base.domain.log.LogTag.THUMBNAIL
import me.proton.core.drive.base.domain.util.RequestBatcher
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.link.domain.entity.RevisionContext
import me.proton.core.drive.link.domain.provider.ProtonSdkClientProvider
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.entity.NodeUid
import java.util.concurrent.atomic.AtomicInteger
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import me.proton.drive.sdk.entity.ThumbnailType as SdkThumbnailType

@Singleton
class GetThumbnailSdk @Inject constructor(
    private val protonSdkClientProvider: ProtonSdkClientProvider,
) {
    private data class BatchKey(
        val userId: UserId,
        val type: SdkThumbnailType,
        val volumeType: Volume.Type,
    )

    private val batchCounter = AtomicInteger(0)

    companion object {
        private const val UID_LOG_LENGTH = 8
    }

    private val batcher = RequestBatcher<BatchKey, NodeUid, InputStream>(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + unhandledErrorHandler(THUMBNAIL)),
        delay = 50.milliseconds,
        timeout = 30.seconds,
    ) { key, uids ->
        val batchId = batchCounter.getAndIncrement()
        fun logThumbnail(message: String) {
            CoreLogger.d(THUMBNAIL, "Batch[$batchId/${key.type}] $message")
        }
        val shortUids = uids.map { it.value.takeLast(UID_LOG_LENGTH) }
        logThumbnail("${key.volumeType}: $shortUids")
        protonSdkClientProvider
            .getOrCreate(key.userId, key.volumeType)
            .getOrThrow()
            .enumerateThumbnails(
                nodeUids = uids,
                type = key.type
            ).withIndex().map { (index, thumbnail) ->
                val status = if (thumbnail.result.isSuccess) {
                    "Success"
                } else {
                    "Failure(${thumbnail.result.exceptionOrNull()?.message})"
                }
                logThumbnail(
                    "(${index + 1}/${uids.size}) received: ${
                        thumbnail.uid.value.takeLast(UID_LOG_LENGTH)
                    } in $status"
                )
                thumbnail.uid to thumbnail.result.map { bytes -> bytes.inputStream() }
            }
    }

    suspend operator fun invoke(
        revisionContext: RevisionContext,
        thumbnailType: ThumbnailType,
    ): Result<InputStream> = coRunCatching {
        batcher.enqueue(
            key = BatchKey(
                userId = revisionContext.userId,
                type = thumbnailType.toSdkType(),
                volumeType = revisionContext.volumeType,
            ),
            item = revisionContext.nodeUid,
        )
    }
}

private fun ThumbnailType.toSdkType() = when (this) {
    ThumbnailType.DEFAULT -> SdkThumbnailType.THUMBNAIL
    ThumbnailType.PHOTO -> SdkThumbnailType.PREVIEW
}
