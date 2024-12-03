/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.ui.test.flow.details

import android.content.Context
import android.os.Build
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.android.drive.ui.annotation.Scenario
import me.proton.android.drive.ui.robot.FilesTabRobot
import me.proton.android.drive.ui.robot.PhotosTabRobot
import me.proton.android.drive.ui.robot.SharedTabRobot
import me.proton.android.drive.ui.test.BaseTest
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.base.presentation.extension.labelResId
import me.proton.core.drive.file.info.presentation.FileInfoTestTag
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test
import me.proton.core.drive.i18n.R as I18N

@HiltAndroidTest
class DetailsFlowTest : BaseTest() {

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "main", value = 4, sharedWithUserTag = "sharingUser")
    fun checkFileDetailsAndCloseDetails() {
        PhotosTabRobot
            .clickFilesTab()
            .verify { robotDisplayed() }
            .scrollToItemWithName(sharedImage.name)
            .clickMoreOnItem(sharedImage.name)
            .clickFileDetails()
            .verify {
                robotDisplayed()
                hasHeaderTitle(sharedImage.name)
                hasHeaderWithIconType(FileInfoTestTag.Header.HeaderIconType.THUMBNAIL)
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_name_entry),
                    value = sharedImage.name,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_uploaded_by_entry),
                    value = sharedImage.uploadedBy,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_location_entry),
                    value = requireNotNull(sharedImage.location),
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_last_modified_entry),
                    value = sharedImage.modified,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_type_entry),
                    value = requireNotNull(sharedImage.type),
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_mime_type_entry),
                    value = requireNotNull(sharedImage.mimeType),
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_size_entry),
                    value = requireNotNull(sharedImage.size),
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_shared_entry),
                    value = sharedImage.isShared,
                )
                hasNotInfoItem(
                    name = targetContext.getString(I18N.string.file_info_permissions),
                )
            }
            .clickBack(FilesTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @Scenario(forTag = "main", value = 4)
    fun checkFolderDetailsAndCloseDetails() {
        PhotosTabRobot
            .clickFilesTab()
            .verify { robotDisplayed() }
            .scrollToItemWithName(sharedFolder.name)
            .clickMoreOnItem(sharedFolder.name)
            .clickFolderDetails()
            .verify {
                robotDisplayed()
                hasHeaderTitle(sharedFolder.name)
                hasHeaderWithIconType(FileInfoTestTag.Header.HeaderIconType.PLACEHOLDER)
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_name_entry),
                    value = sharedFolder.name,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_uploaded_by_entry),
                    value = sharedFolder.uploadedBy,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_location_entry),
                    value = requireNotNull(sharedFolder.location),
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_last_modified_entry),
                    value = sharedFolder.modified,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_shared_entry),
                    value = sharedFolder.isShared,
                )
                hasNotInfoItem(
                    name = targetContext.getString(I18N.string.file_info_type_entry),
                )
                hasNotInfoItem(
                    name = targetContext.getString(I18N.string.file_info_mime_type_entry),
                )
                hasNotInfoItem(
                    name = targetContext.getString(I18N.string.file_info_size_entry),
                )
                hasNotInfoItem(
                    name = targetContext.getString(I18N.string.file_info_permissions),
                )
            }
            .clickBack(FilesTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @PrepareUser(withTag = "sharingUser")
    @Scenario(forTag = "sharingUser", value = 6, sharedWithUserTag = "main")
    fun checkSharingDetailsAndCloseDetails() {
        val link = readOnlyFile(protonRule.testDataRule.preparedUsers["sharingUser"]!!.email)
        PhotosTabRobot.waitUntilLoaded()
        PhotosTabRobot
            .clickSharedTab()
            .verify { robotDisplayed() }
            .scrollToItemWithName(link.name)
            .clickMoreOnItem(link.name)
            .clickFileDetails()
            .verify {
                robotDisplayed()
                hasHeaderTitle(link.name)
                hasHeaderWithIconType(FileInfoTestTag.Header.HeaderIconType.PLACEHOLDER)
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_name_entry),
                    value = link.name,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_uploaded_by_entry),
                    value = link.uploadedBy,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_last_modified_entry),
                    value = link.modified,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_size_entry),
                    value = requireNotNull(link.size),
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_shared_entry),
                    value = link.isShared,
                )
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_permissions),
                    value = requireNotNull(link.permissions),
                )
                hasNotInfoItem(
                    name = targetContext.getString(I18N.string.file_info_location_entry),
                )
                hasNotInfoItem(
                    name = targetContext.getString(I18N.string.file_info_type_entry),
                )
                hasNotInfoItem(
                    name = targetContext.getString(I18N.string.file_info_mime_type_entry),
                )
            }
            .clickBack(SharedTabRobot)
            .verify { robotDisplayed() }
    }

    @Test
    @PrepareUser(withTag = "main", loginBefore = true)
    @Scenario(forTag = "main", value = 9)
    fun checkAnonymousDetails() {
        val filename = "anonymous-file"
        PhotosTabRobot
            .clickFilesTab()
            .verify { robotDisplayed() }
            .scrollToItemWithName(filename)
            .clickMoreOnItem(filename)
            .clickFileDetails()
            .verify {
                robotDisplayed()
                hasInfoItem(
                    name = targetContext.getString(I18N.string.file_info_uploaded_by_entry),
                    value = targetContext.getString(I18N.string.file_info_uploaded_by_anonymous),
                )
            }
            .clickBack(FilesTabRobot)
            .verify { robotDisplayed() }
    }

    data class LinkDetails(
        val name: String,
        val uploadedBy: String,
        val modified: String,
        val isShared: String,
        val location: String? = null,
        val type: String? = null,
        val mimeType: String? = null,
        val size: String? = null,
        val permissions: String? = null,
    )

    private val sharedImage get() = LinkDetails(
        name = "shared.jpg",
        uploadedBy = "${protonRule.testDataRule.mainTestUser?.name}@${envConfig.host}",
        location = "/" + targetContext.getString(I18N.string.title_my_files),
        modified = TimestampS().asHumanReadableString(),
        isShared = targetContext.getString(I18N.string.common_yes),
        type = targetContext.getType("image/jpeg"),
        mimeType = "image/jpeg",
        size = 16177.bytes.asHumanReadableString(targetContext),
    )

    private fun Context.getType(mimeType: String): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.getTypeInfo(mimeType).label.toString()
        } else {
            getString(mimeType.toFileTypeCategory().labelResId)
        }

    private val sharedFolder get() = LinkDetails(
        name = "sharedFolder",
        uploadedBy = "${protonRule.testDataRule.mainTestUser?.name}@${envConfig.host}",
        location = "/" + targetContext.getString(I18N.string.title_my_files),
        modified = TimestampS().asHumanReadableString(),
        isShared = targetContext.getString(I18N.string.common_yes),
    )

    private fun readOnlyFile(uploadedBy: String) = LinkDetails(
        name = "ReadOnlyFile.txt",
        uploadedBy = uploadedBy,
        modified = TimestampS().asHumanReadableString(),
        isShared = targetContext.getString(I18N.string.common_yes),
        size = 61.bytes.asHumanReadableString(targetContext),
        permissions = targetContext.getString(I18N.string.common_permission_viewer),
    )
}
