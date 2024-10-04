/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.feature.flag.domain.entity

import me.proton.core.domain.entity.UserId

data class FeatureFlagId(val userId: UserId, val id: String) {

    companion object {
        const val DRIVE_PHOTOS_UPLOAD_DISABLED = "DrivePhotosUploadDisabled"
        const val DRIVE_SHARING_DEVELOPMENT = "DriveSharingDevelopment"
        const val DRIVE_SHARING_INVITATIONS = "DriveSharingInvitations"
        const val DRIVE_SHARING_DISABLED = "DriveSharingDisabled"
        const val DRIVE_SHARING_EDITING_DISABLED = "DriveSharingEditingDisabled"
        const val DRIVE_SHARING_EXTERNAL_INVITATIONS = "DriveSharingExternalInvitations"
        const val DRIVE_SHARING_EXTERNAL_INVITATIONS_DISABLED = "DriveSharingExternalInvitationsDisabled"
        const val DRIVE_DOCS_WEBVIEW = "DriveDocsWebView"
        const val DRIVE_DOCS_DISABLED = "DriveDocsDisabled"

        internal var developments : List<String> = listOf(
        )

        fun drivePhotosUploadDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_PHOTOS_UPLOAD_DISABLED)
        fun driveSharingDevelopment(userId: UserId) = FeatureFlagId(userId, DRIVE_SHARING_DEVELOPMENT)
        fun driveSharingInvitations(userId: UserId) = FeatureFlagId(userId, DRIVE_SHARING_INVITATIONS)
        fun driveSharingDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_SHARING_DISABLED)
        fun driveSharingEditingDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_SHARING_EDITING_DISABLED)
        fun driveSharingExternalInvitations(userId: UserId) = FeatureFlagId(userId, DRIVE_SHARING_EXTERNAL_INVITATIONS)
        fun driveSharingExternalInvitationsDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_SHARING_EXTERNAL_INVITATIONS_DISABLED)
        fun driveDocsWebView(userId: UserId) = FeatureFlagId(userId, DRIVE_DOCS_WEBVIEW)
        fun driveDocsDisabled(userId: UserId) = FeatureFlagId(userId, DRIVE_DOCS_DISABLED)
    }
}
