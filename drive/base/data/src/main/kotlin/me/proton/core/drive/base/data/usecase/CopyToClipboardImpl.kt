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
package me.proton.core.drive.base.data.usecase

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.usecase.CopyToClipboard
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class CopyToClipboardImpl @Inject constructor(
    private val clipboardManager: ClipboardManager,
    private val broadcastMessages: BroadcastMessages,
    @ApplicationContext private val appContext: Context,
) : CopyToClipboard {
    override fun invoke(text: String): Result<Unit> = coRunCatching {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", text))
    }

    override fun invoke(
        userId: UserId,
        label: String,
        text: String,
        isSensitive: Boolean,
    ): Result<Unit> = coRunCatching {
        val clipData = ClipData.newPlainText(label, text)
        if (isSensitive) {
            clipData.description.extras = PersistableBundle().apply {
                putBoolean(EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboardManager.setPrimaryClip(clipData)
        broadcastMessages(
            userId = userId,
            message = appContext.getString(
                I18N.string.common_in_app_notification_copied_to_clipboard,
                label,
            ),
            type = BroadcastMessage.Type.INFO,
        )
    }

    private companion object {
        val EXTRA_IS_SENSITIVE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ClipDescription.EXTRA_IS_SENSITIVE
        } else {
            "android.content.extra.IS_SENSITIVE"
        }
    }
}
