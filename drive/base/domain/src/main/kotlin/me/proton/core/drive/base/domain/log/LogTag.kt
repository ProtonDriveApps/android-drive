/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.base.domain.log

import androidx.lifecycle.ViewModel

object LogTag {
    const val DEFAULT = "core.drive"
    const val DOCUMENTS_PROVIDER = "$DEFAULT.documents"
    const val DOWNLOAD = "$DEFAULT.download"
    const val ENCRYPTION = "$DEFAULT.encryption"
    const val EVENTS = "$DEFAULT.events"
    const val GET_FILE = "$DEFAULT.getfile"
    const val KEY = "$DEFAULT.key"
    const val OPERATION = "$DEFAULT.operation"
    const val MEDIA = "$DEFAULT.media"
    const val MOVE = "$OPERATION.move"
    const val RENAME = "$OPERATION.rename"
    const val SHARE = "$OPERATION.share"
    const val SHARING = "$DEFAULT.sharing"
    const val PAGING = "$DEFAULT.paging"
    const val BACKUP = "$DEFAULT.backup"
    const val ALBUM = "$DEFAULT.album"
    const val PHOTO = "$DEFAULT.photo"
    const val TELEMETRY = "$DEFAULT.telemetry"
    const val UPLOAD = "$DEFAULT.upload"
    const val UPLOAD_BULK = "$UPLOAD.bulk"
    const val ANNOUNCE_EVENT = "$DEFAULT.announce.event"
    const val BROADCAST_RECEIVER = "$DEFAULT.broadcast.receiver"
    const val NOTIFICATION = "$DEFAULT.notification"
    const val TRASH = "$DEFAULT.trash"
    const val THUMBNAIL = "$DEFAULT.thumbnail"
    const val FEATURE_FLAG = "$DEFAULT.feature.flag"
    const val ENTITLEMENT = "$DEFAULT.entitlement"
    const val LOG = "$DEFAULT.log"
    const val WEBVIEW = "$DEFAULT.webview"
    const val PROTON_DOCS = "$WEBVIEW.proton.docs"
    const val FOLDER = "$DEFAULT.folder"
    const val TRACKING = "$DEFAULT.tracking"
    const val METRIC = "$DEFAULT.metric"
    val ViewModel.VIEW_MODEL: String get() = "$DEFAULT.view.model[${this.javaClass.simpleName}]"

    object UploadTag {
        fun Long.logTag() = "$UPLOAD.$this"
    }
}

fun String.logId(): String = this.take(8)
