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

package me.proton.android.drive.usecase

import me.proton.android.drive.lock.domain.usecase.DisableAppLock
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.DeleteAllFolders
import me.proton.core.drive.drivelink.crypto.domain.usecase.RemoveAllDecryptedText
import me.proton.core.drive.key.domain.usecase.RemoveAllKeys
import me.proton.core.drive.log.domain.usecase.ClearLogs
import javax.inject.Inject

class CleanUpAccount @Inject constructor(
    private val deleteAllFolders: DeleteAllFolders,
    private val removeAllKeys: RemoveAllKeys,
    private val removeAllDecryptedText: RemoveAllDecryptedText,
    private val disableAppLock: DisableAppLock,
    private val clearLogs: ClearLogs,
) {

    suspend operator fun invoke(userId: UserId) {
        deleteAllFolders(userId)
        removeAllKeys(userId)
        removeAllDecryptedText(userId)
        disableAppLock(userAuthenticationRequired = false)
        clearLogs(userId)
    }
}
