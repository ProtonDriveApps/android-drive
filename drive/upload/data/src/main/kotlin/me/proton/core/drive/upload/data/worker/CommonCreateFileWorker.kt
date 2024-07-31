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

package me.proton.core.drive.upload.data.worker

import android.content.Context
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.data.workmanager.onProtonHttpException
import me.proton.core.drive.base.domain.extension.avoidDuplicateFileName
import me.proton.core.drive.base.domain.extension.trimForbiddenChars
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.feature.flag.domain.repository.FeatureFlagRepository
import me.proton.core.drive.feature.flag.domain.usecase.RefreshFeatureFlags
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.UpdateName
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run

@OptIn(ExperimentalCoroutinesApi::class)
abstract class CommonCreateFileWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    workManager: WorkManager,
    broadcastMessages: BroadcastMessages,
    getUploadFileLink: GetUploadFileLink,
    uploadErrorManager: UploadErrorManager,
    private val updateName: UpdateName,
    private val refreshFeatureFlags: RefreshFeatureFlags,
    configurationProvider: ConfigurationProvider,
    canRun: CanRun,
    run: Run,
    done: Done,
) : UploadCoroutineWorker(
    appContext = appContext,
    workerParams = workerParams,
    workManager = workManager,
    broadcastMessages = broadcastMessages,
    getUploadFileLink = getUploadFileLink,
    uploadErrorManager = uploadErrorManager,
    configurationProvider = configurationProvider,
    canRun = canRun,
    run = run,
    done = done,
) {
    protected suspend fun Throwable.handle(uploadFileLink: UploadFileLink): Boolean =
        onProtonHttpException { protonCode ->
            if (protonCode == ProtonApiCode.ALREADY_EXISTS) {
                updateName(
                    uploadFileLinkId = uploadFileLink.id,
                    name = uploadFileLink.name
                        .trimForbiddenChars()
                        .avoidDuplicateFileName()
                )
                true
            } else {
                false
            }
        } ?: false

    /**
     * TODO: this should be handled in Core for any response
     */
    protected suspend fun Throwable.handleFeatureDisabled() =
        onProtonHttpException { protonCode ->
            if (protonCode == ProtonApiCode.FEATURE_DISABLED) {
                refreshFeatureFlags(userId, FeatureFlagRepository.RefreshId.API_ERROR_FEATURE_DISABLED).getOrNull()
            }
        }
}
