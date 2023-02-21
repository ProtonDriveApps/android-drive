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
package me.proton.core.drive.upload.data.worker

object WorkerKeys {
    const val KEY_USER_ID = "key.userId"
    const val KEY_VOLUME_ID = "key.volumeId"
    const val KEY_SHARE_ID = "key.shareId"
    const val KEY_FOLDER_ID = "key.folderId"
    const val KEY_SIZE = "key.size"
    const val KEY_BLOCK_INDEX = "key.blockIndex"
    const val KEY_BLOCK_TOKEN = "key.blockToken"
    const val KEY_BLOCK_URL = "key.blockUrl"
    const val KEY_URI_STRING = "key.uriString"
    const val KEY_UPLOAD_FILE_LINK_ID = "key.uploadFileLinkId"
    const val KEY_SHOULD_DELETE_SOURCE = "key.shouldDeleteSource"
    const val KEY_UPLOAD_FILE_ID = "key.uploadFileId"
    const val KEY_IS_CANCELLED = "key.isCancelled"
    const val KEY_UPLOAD_BULK_ID = "key.uploadBulkId"
    const val KEY_FOLDER_NAME = "key.folderName"
}
