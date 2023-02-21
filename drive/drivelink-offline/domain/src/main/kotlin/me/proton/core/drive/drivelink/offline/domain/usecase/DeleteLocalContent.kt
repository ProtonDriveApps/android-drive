/*
 * Copyright (c) 2022-2023 Proton AG.
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

package me.proton.core.drive.drivelink.offline.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.util.kotlin.CoreLogger
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class DeleteLocalContent @Inject constructor(
    private val getCacheFolder: GetCacheFolder,
    private val getPermanentFolder: GetPermanentFolder,
) {

    suspend operator fun invoke(
        file: DriveLink.File,
        coroutineContext: CoroutineContext = Job() + Dispatchers.IO,
    ) = try {
        CoreLogger.d(LogTag.EVENTS, "Deleting local folders for: ${file.id.id.logId()}")
        getCacheFolder(file.userId, file.volumeId.id, file.activeRevisionId, coroutineContext).deleteRecursively()
        getPermanentFolder(file.userId, file.volumeId.id, file.activeRevisionId, coroutineContext).deleteRecursively()
    } catch (ignored: IOException) {
        //
    }
}
