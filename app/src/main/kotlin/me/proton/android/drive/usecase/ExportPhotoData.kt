/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.usecase

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.usecase.GetFolders
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toTimestampMs
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.base.domain.function.pagedList
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.provider.StorageLocationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.list.domain.usecase.GetDecryptedDriveLinks
import me.proton.core.drive.file.base.domain.extension.toXAttr
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@Suppress("LongParameterList")
class ExportPhotoData @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configurationProvider: ConfigurationProvider,
    private val storageLocationProvider: StorageLocationProvider,
    private val dateTimeFormatter: DateTimeFormatter,
    private val getFolders: GetFolders,
    private val getPhotoShare: GetPhotoShare,
    private val getDecryptedDriveLinks: GetDecryptedDriveLinks,
) {
    suspend operator fun invoke(
        userId: UserId,
    ): Result<File> = coRunCatching {
        File(
            storageLocationProvider.getCacheTempFolder(userId),
            "exports/exports-${dateTimeFormatter.formatToIso8601String(Date())}.zip"
        ).apply {
            parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(this)).use { zos ->
                zos.putNextEntry(ZipEntry("local.csv"))
                zos.exportLocalData(userId)
                zos.closeEntry()
                zos.putNextEntry(ZipEntry("drive.csv"))
                zos.exportDriveData(getPhotoShare(userId).toResult().getOrThrow().rootFolderId)
                zos.closeEntry()
            }
        }
    }

    private suspend fun OutputStream.exportLocalData(userId: UserId) {
        writeLine("name", "size", "mimetype", "modificationTime", "bucketId", "bucketName", "_id")
        getFolders(userId).getOrThrow().map { it.bucketId }.onEach { folderBucketId ->
            context.contentResolver.query(
                getFilesContentUri(),
                PROJECTION_BUCKET,
                "${MediaStore.Files.FileColumns.BUCKET_ID} LIKE ?",
                arrayOf(folderBucketId.toString()),
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(INDEX_DISPLAY_NAME)
                    val size = cursor.getInt(INDEX_SIZE).toLong()
                    val mediaType = cursor.getInt(INDEX_MEDIA_TYPE)
                    val mimeType = cursor.getString(INDEX_MIME_TYPE)
                    val dateModified = cursor.getLong(INDEX_DATE_MODIFIED)
                    val bucketId = cursor.getLong(INDEX_BUCKET_ID)
                    val bucketName = cursor.getString(INDEX_BUCKET_DISPLAY_NAME)
                    val id = cursor.getLong(INDEX_ID)
                    val modificationDate = dateTimeFormatter.formatToIso8601String(
                        Date(TimestampS(dateModified).toTimestampMs().value)
                    )
                    if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                        || mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    ) {
                        writeLine(
                            name,
                            size,
                            mimeType,
                            modificationDate,
                            bucketId,
                            bucketName,
                            id
                        )
                    }
                }
            }
        }
    }

    private suspend fun OutputStream.exportDriveData(rootFolderId: FolderId) {
        writeLine("name", "size", "mimetype", "modificationTime", "userId", "shareId", "parentId", "linkId")
        pagedList(configurationProvider.dbPageSize) { fromIndex, count ->
            getDecryptedDriveLinks(rootFolderId, fromIndex, count).first().getOrThrow()
        }.filterIsInstance(DriveLink.File::class.java).filterNot { driveLink ->
            driveLink.isTrashed
        }.onEach { driveLink ->
            val xAttrCommon = driveLink.cryptoXAttr.value?.toXAttr()?.getOrNull()?.common
            with(driveLink) {
                writeLine(
                    name,
                    xAttrCommon?.size,
                    mimeType,
                    xAttrCommon?.modificationTime,
                    userId.id,
                    shareId.id,
                    parentId.id,
                    id.id,
                )
            }
        }
    }

    private fun OutputStream.writeLine(
        vararg data: Any?,
    ) {
        val line = data.joinToString(";")
        write("$line\n".toByteArray())
    }

    private companion object {

        private val PROJECTION_BUCKET = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns._ID,
        )

        // The indices should match the above projections.
        private const val INDEX_DISPLAY_NAME = 0
        private const val INDEX_SIZE = 1
        private const val INDEX_MEDIA_TYPE = 2
        private const val INDEX_MIME_TYPE = 3
        private const val INDEX_DATE_MODIFIED = 4
        private const val INDEX_BUCKET_ID = 5
        private const val INDEX_BUCKET_DISPLAY_NAME = 6
        private const val INDEX_ID = 7

        private const val EXTERNAL_MEDIA = "external"
        private fun getFilesContentUri(): Uri {
            return MediaStore.Files.getContentUri(EXTERNAL_MEDIA)
        }
    }
}
