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

package me.proton.android.drive.photos.presentation.viewmodel

import android.content.Context
import android.content.res.Resources
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.drive.photos.presentation.viewstate.PhotosStatusViewState
import me.proton.core.drive.backup.domain.entity.BackupState
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.i18n.R
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

class BackupStatusFormatter(
    private val resources: Resources,
    private val locale: Locale,
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(context.resources, Locale.getDefault())

    fun toViewState(backupState: BackupState, count: Int ?= null): PhotosStatusViewState {
        return if (!backupState.isBackupEnabled) {
            PhotosStatusViewState.Disabled(backupState.hasDefaultFolder)
        } else {
            when (val backupStatus = backupState.backupStatus) {
                is BackupStatus.Complete -> PhotosStatusViewState.Complete(
                    count?.let { c ->
                        resources.getQuantityString(
                            R.plurals.photos_backup_state_completed_items_saved,
                            c,
                            NumberFormat.getNumberInstance(locale)
                                .format(c),
                        )
                    }
                )
                is BackupStatus.Uncompleted -> PhotosStatusViewState.Uncompleted
                is BackupStatus.Failed -> PhotosStatusViewState.Failed(
                    errors = backupStatus.errors,
                )

                is BackupStatus.InProgress -> PhotosStatusViewState.InProgress(
                    progress = backupStatus.progress,
                    labelItemsLeft = resources.getQuantityString(
                        R.plurals.photos_backup_state_uploading_items_left,
                        backupStatus.pendingBackupPhotos,
                        NumberFormat.getNumberInstance(locale)
                            .format(backupStatus.pendingBackupPhotos),
                    )
                )

                is BackupStatus.Preparing -> PhotosStatusViewState.Preparing(
                    progress = backupStatus.progress,
                )

                null -> error("Enabled backup should always have a status")
            }
        }
    }
}
