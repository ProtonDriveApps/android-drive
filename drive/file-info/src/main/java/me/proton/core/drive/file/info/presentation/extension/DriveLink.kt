/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.file.info.presentation.extension

import android.content.Context
import android.os.Build
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.base.presentation.extension.labelResId
import me.proton.core.drive.base.presentation.extension.toPermissionLabel
import me.proton.core.drive.base.presentation.extension.toReadableDate
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.hasShareLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.domain.extension.isSharedByLinkOrWithUsers
import me.proton.core.drive.file.info.presentation.entity.Item
import me.proton.core.drive.link.domain.entity.BaseLink
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.extension.isProtonCloudFile
import me.proton.core.drive.link.presentation.extension.lastModified
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.i18n.R as I18N

fun DriveLink.toItems(
    context: Context,
    parentPath: String,
    capturedOn: TimestampS? = null,
    shareType: Share.Type = Share.Type.PHOTO,
): List<Item> = listOfNotNull(
    Item(
        name = context.getString(I18N.string.file_info_name_entry),
        value = name,
        isValueEncrypted = isNameEncrypted,
    ),
    Item(
        name = context.getString(I18N.string.file_info_uploaded_by_entry),
        value = uploadedBy.takeUnless { it.isEmpty() }
            ?: context.getString(I18N.string.file_info_uploaded_by_anonymous),
    ),
    takeIf { shareType == Share.Type.MAIN }?.let {
        Item(
            name = context.getString(I18N.string.file_info_location_entry),
            value = parentPath,
        )
    },
    Item(
        name = context.getString(I18N.string.file_info_last_modified_entry),
        value = lastModified(),
    ),
    takeIf { this !is Folder && shareType == Share.Type.MAIN }?.let {
        Item(
            name = context.getString(I18N.string.file_info_type_entry),
            value = getType(context),
        )
    },
    takeIf { this !is Folder && shareType == Share.Type.MAIN }?.let {
        Item(
            name = context.getString(I18N.string.file_info_mime_type_entry),
            value = mimeType,
        )
    },
    takeIf { this !is Folder && this.isProtonCloudFile.not() }?.let {
        Item(
            name = context.getString(I18N.string.file_info_size_entry),
            value = size.asHumanReadableString(context),
        )
    },
    Item(
        name = context.getString(I18N.string.file_info_shared_entry),
        value = context.getString(
            if (isSharedByLinkOrWithUsers) {
                I18N.string.common_yes
            } else {
                I18N.string.common_no
            }
        ),
    ),
    takeIf { hasShareLink }?.let {
        Item(
            name = context.getString(I18N.string.file_info_number_of_accesses_entry),
            value = numberOfAccesses.toString(),
        )
    },
    takeIf { hasShareLink }?.let {
        Item(
            name = context.getString(I18N.string.file_info_share_url_expiration_time),
            value = shareUrlExpirationTime?.toReadableDate()
                ?: context.getString(I18N.string.file_info_share_url_no_expiration_time),
        )
    },
    takeIf { shareType == Share.Type.PHOTO }?.let {
        capturedOn?.let { timestamp ->
            Item(
                name = context.getString(I18N.string.file_info_captured_on),
                value = timestamp.asHumanReadableString(context).toString(),
            )
        }
    },
    sharePermissions?.let { permissions ->
        Item(
            name = context.getString(I18N.string.file_info_permissions),
            value = permissions.toPermissionLabel(context),
        )
    }
)

private fun BaseLink.getType(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return context.contentResolver.getTypeInfo(mimeType).label.toString()
    }
    return context.getString(mimeType.toFileTypeCategory().labelResId)
}
