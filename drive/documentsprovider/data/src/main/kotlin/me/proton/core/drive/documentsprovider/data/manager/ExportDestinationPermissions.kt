/*
 * Copyright (c) 2026 Proton AG.
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
package me.proton.core.drive.documentsprovider.data.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatchingException
import me.proton.core.drive.documentsprovider.data.worker.ExportToDestinationUriWorker
import me.proton.core.util.kotlin.CoreLogger
import java.util.UUID
import javax.inject.Inject

/**
 * Persistable write permissions for destinations picked for an export.
 *
 * Export to a picked destination is the only place that takes a persistable write permission, so
 * once no such export is pending, every persisted write permission can be released. This also
 * covers exports that were cancelled before they ran or were never enqueued because of
 * [androidx.work.ExistingWorkPolicy.KEEP].
 */
class ExportDestinationPermissions @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val workManager: WorkManager,
) {

    /** Must be called only after the export work for [uri] is enqueued, see [releaseUnused]. */
    fun take(uri: Uri) = coRunCatchingException {
        appContext.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }.onFailure { error ->
        // The exception message contains the destination uri which includes the file name
        CoreLogger.d(
            LogTag.DOCUMENTS_PROVIDER,
            "No persistable permission for export destination (${error.javaClass.simpleName}), " +
                "relying on temporary grant",
        )
    }

    /**
     * Permissions are listed before pending work is checked: a permission taken after the listing
     * is not released, and one taken before it belongs to work that is already enqueued.
     */
    suspend fun releaseUnused(excludeWorkId: UUID? = null) = coRunCatchingException {
        val uris = appContext.contentResolver.persistedUriPermissions
            .filter { permission -> permission.isWritePermission }
            .map { permission -> permission.uri }
        val hasPendingExport = workManager
            .getWorkInfosByTagFlow(ExportToDestinationUriWorker.TAG)
            .first()
            .any { workInfo -> workInfo.id != excludeWorkId && !workInfo.state.isFinished }
        if (!hasPendingExport) {
            uris.forEach { uri ->
                appContext.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
    }.onFailure { error ->
        // The exception message can contain a destination uri which includes the file name
        CoreLogger.d(
            LogTag.DOCUMENTS_PROVIDER,
            "Failed to release persistable permissions of export destinations (${error.javaClass.simpleName})",
        )
    }
}
