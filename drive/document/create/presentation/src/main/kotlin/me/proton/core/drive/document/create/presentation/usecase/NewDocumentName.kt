/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.document.create.presentation.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class NewDocumentName @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    operator fun invoke(): String = appContext.getString(I18N.string.new_document_name, simpleDateFormat.format(Date()))

    private val simpleDateFormat: SimpleDateFormat by lazy { SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.US) }
}
