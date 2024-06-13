/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.drivelink.data.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.UpdateDriveLinkDisplayName
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class UpdateDriveLinkDisplayNameImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getShare: GetShare,
) : UpdateDriveLinkDisplayName {

    override suspend operator fun <T : DriveLink> invoke(driveLink: T): T =
        if (driveLink is DriveLink.Folder && driveLink.parentId == null) {
            val shareType = getShare(driveLink.id.shareId).toResult().getOrNull()?.type
            @Suppress("UNCHECKED_CAST")
            driveLink.copy(displayName = shareType?.displayName) as T
        } else {
            driveLink
        }

    override suspend operator fun <T : Link> invoke(link: T): T =
        if (link is Link.Folder && link.parentId == null) {
            val shareType = getShare(link.id.shareId).toResult().getOrNull()?.type
            link.copy(name = shareType?.displayName ?: link.name) as T
        } else {
            link
        }

    private val Share.Type.displayName: String? get() = when (this) {
        Share.Type.MAIN -> appContext.getString(I18N.string.title_my_files)
        Share.Type.PHOTO -> appContext.getString(I18N.string.photos_title)
        else -> null
    }
}
