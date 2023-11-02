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

package me.proton.android.drive.verifier.data.extension

import android.content.Context
import me.proton.android.drive.verifier.domain.exception.VerifierException
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.drive.i18n.R as I18N

fun VerifierException.getDefaultMessage(context: Context): String = when (this) {
    is VerifierException.Initialize -> context.getString(I18N.string.verifier_initialize_failed)
    is VerifierException.VerifyBlock -> context.getString(I18N.string.verifier_verify_blocks_failed)
}

fun VerifierException.log(tag: String, message: String = this.message.orEmpty()): VerifierException = also {
    val logToSentry = this is VerifierException.VerifyBlock
    val log: (String, Throwable, String) -> Unit = if (logToSentry) CoreLogger::e else CoreLogger::d
    log(tag, this, message)
}
