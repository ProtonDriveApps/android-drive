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

package me.proton.core.drive.user.presentation.quota.extension

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import me.proton.core.drive.base.domain.extension.toPercentString
import me.proton.core.drive.user.domain.entity.QuotaLevel
import me.proton.core.drive.user.presentation.quota.viewstate.QuotaViewState
import java.util.Locale
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

fun QuotaLevel.toState(context: Context) = if (this == QuotaLevel.NULL) {
    null
} else {
    QuotaViewState(
        level = level,
        iconResId = iconResId,
        title = title(context),
        actionLabel = actionLabel(context),
        canDismiss = canDismiss,
    )
}

private val QuotaLevel.level: QuotaViewState.Level
    get() = when (this) {
        QuotaLevel.NULL -> throw IllegalStateException()
        QuotaLevel.INFO -> QuotaViewState.Level.INFO
        QuotaLevel.WARNING -> QuotaViewState.Level.WARNING
        QuotaLevel.ERROR -> QuotaViewState.Level.ERROR
    }

private val iconResId: Int
    get() = CorePresentation.drawable.ic_proton_cloud

private fun QuotaLevel.title(context: Context): AnnotatedString = if (this == QuotaLevel.ERROR) {
    AnnotatedString(
        context.getString(I18N.string.storage_quotas_full_title).format(
            context.getString(I18N.string.app_name)
        )
    )
} else {
    val placeholder = percentage.toPercentString(Locale.getDefault())
    val text =
        context.getString(I18N.string.storage_quotas_not_full_title).format(placeholder)
    val start = text.indexOf(placeholder)
    val spanStyles = listOf(
        AnnotatedString.Range(
            SpanStyle(fontWeight = FontWeight.Bold),
            start = start,
            end = start + placeholder.length
        )
    )
    AnnotatedString(text = text, spanStyles = spanStyles)
}

private fun actionLabel(context: Context): String =
    context.getString(I18N.string.storage_quotas_get_storage_action)

private val QuotaLevel.canDismiss: Boolean
    get() = this != QuotaLevel.ERROR
