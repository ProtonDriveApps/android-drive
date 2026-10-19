/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.usecase

import android.app.Activity
import com.google.android.play.core.review.ReviewManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import me.proton.android.drive.log.DriveLogTag.UI
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.drive.base.data.entity.LoggerLevel.WARNING
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.coroutines.ApplicationScope
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class ShowRatingBooster @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val markRatingBoosterAsShown: MarkRatingBoosterAsShown,
    private val reviewManager: ReviewManager,
    private val accountManager: AccountManager,
) {

    operator fun invoke(activity: Activity) {
        coRunCatching {
            reviewManager.requestReviewFlow().addOnCompleteListener { infoTask ->
                if (infoTask.isSuccessful) {
                    reviewManager.launchReviewFlow(activity, infoTask.result)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                applicationScope.launch {
                                    accountManager.getPrimaryUserId().firstOrNull()?.let { userId ->
                                        markRatingBoosterAsShown(userId = userId).onFailure { error ->
                                            error.log(UI, "Marking rating booster as shown failed")
                                        }
                                    } ?: {
                                        CoreLogger.w(UI, "User Id is null, please investigate")
                                    }()
                                }
                            } else {
                                task.exception?.log(UI, "Cannot launch review", WARNING)
                            }
                        }
                } else {
                    infoTask.exception?.log(UI, "Cannot request review", WARNING)
                }
            }
        }.onFailure { error ->
            error.log(UI, "Review manager failed to request review")
        }
    }
}
