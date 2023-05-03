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
package me.proton.core.drive.base.data.db

object Column {
    const val ADDRESS_ID = "address_id"
    const val ATTRIBUTES = "attributes"
    const val BLOCK_SIZE = "block_size"
    const val CHANNEL_TYPE = "channel_type"
    const val CONTENT = "content"
    const val CONTENT_KEY_PACKET = "content_key_packet"
    const val CONTENT_KEY_PACKET_SIGNATURE = "content_key_packet_signature"
    const val CREATION_TIME = "creation_time"
    const val CREATOR_EMAIL = "creatior_email"
    const val DIGESTS = "digests"
    const val DURATION = "duration"
    const val ENCRYPTED = "encrypted"
    const val ENCRYPTED_NAME = "encrypted_name"
    const val ENCRYPTED_SIGNATURE = "encrypted_signature"
    const val EXPIRATION_TIME = "expiration_time"
    const val FLAGS = "flags"
    const val HAS_THUMBNAIL = "has_thumbnail"
    const val HASH = "hash"
    const val INDEX = "index"
    const val ID = "id"
    const val IS_SHARED = "is_shared"
    const val KEY = "key"
    const val LAST_ACCESS_TIME = "last_access_time"
    const val LAST_FETCH_CHILDREN_TIMESTAMP = "last_fetch_children_timestamp"
    const val LAST_FETCH_TRASH_TIMESTAMP = "last_fetch_trash_timestamp"
    const val LAST_MODIFIED = "last_modified"
    const val LAYOUT_TYPE = "layout_type"
    const val LINK_ID = "link_id"
    const val LOCKED = "locked"
    const val MANIFEST_SIGNATURE = "manifest_signature"
    const val MAX_ACCESSES = "max_accesses"
    const val MAX_SPACE = "max_space"
    const val MEDIA_RESOLUTION_HEIGHT = "media_resolution_height"
    const val MEDIA_RESOLUTION_WIDTH = "media_resolution_width"
    const val MIME_TYPE = "mime_type"
    const val NAME = "name"
    const val NAME_SIGNATURE_EMAIL = "name_signature_email"
    const val NEXT_KEY = "next_key"
    const val NODE_HASH_KEY = "node_hash_key"
    const val NODE_KEY = "node_key"
    const val NODE_PASSPHRASE = "node_passphrase"
    const val NODE_PASSPHRASE_SIGNATURE = "node_passphrase_signature"
    const val NOTIFICATION_ID = "notification_id"
    const val NOTIFICATION_EVENT = "notification_event"
    const val NOTIFICATION_EVENT_ID = "notification_event_id"
    const val NOTIFICATION_TAG = "notification_tag"
    const val NUMBER_OF_ACCESSES = "number_of_accesses"
    const val PARENT_ID = "parent_id"
    const val PASSPHRASE = "passphrase"
    const val PASSPHRASE_SIGNATURE = "passphrase_signature"
    const val PASSWORD = "password"
    const val PERMISSIONS = "permissions"
    const val PREVIOUS_KEY = "previous_key"
    const val PUBLIC_URL = "public_url"
    const val RAW_SIZE = "raw_size"
    const val REVISION_ID = "revision_id"
    const val RUN_AT = "run_at"
    const val SELECTION_ID = "selection_id"
    const val SIGNATURE_ADDRESS = "signature_address"
    const val SIGNATURE_EMAIL = "signature_email"
    const val SIZE = "size"
    const val SHARE_ID = "share_id"
    const val SHARE_PASSPHRASE_KEY_PACKET = "share_passphrase_key_packet"
    const val SHARE_PASSWORD_SALT = "share_password_salt"
    const val SHARE_URL_EXPIRATION_TIME = "share_url_expiration_time"
    const val SHARE_URL_ID = "share_url_id"
    const val SHARE_URL_SHARE_ID = "share_url_share_id"
    const val SHOULD_DELETE_SOURCE_URI = "should_delete_source_uri"
    const val SORTING_BY = "sorting_by"
    const val SORTING_DIRECTION = "sorting_direction"
    const val SRP_MODULUS_ID = "srp_modulus_id"
    const val SRP_VERIFIER = "srp_verifier"
    const val STATE = "state"
    const val TAG = "tag"
    const val THEME_STYLE = "theme_style"
    const val THUMBNAIL = "thumbnail"
    const val TOKEN = "token"
    const val TRASH_STATE = "trash_state"
    const val TRASHED_TIME = "trashed_time"
    const val TYPE = "type"
    const val UPLOADED_BY = "uploaded_by"
    const val UPLOAD_BULK_ID = "upload_bulk_id"
    const val UPLOAD_LINK_ID = "upload_link_id"
    const val URI = "uri"
    const val URL = "url"
    const val URL_PASSWORD_SALT = "url_password_salt"
    const val USED_SPACE = "used_space"
    const val USER_ID = "user_id"
    const val VOLUME_ID = "volume_id"
    const val WORK_ID = "work_id"
    const val WORKER_ID = "worker_id"
    const val X_ATTR = "x_attr"

    object Core {
        const val USER_ID = "userId"
    }
}
