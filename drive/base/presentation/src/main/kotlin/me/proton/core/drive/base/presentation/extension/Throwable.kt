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
package me.proton.core.drive.base.presentation.extension

import android.content.Context
import android.database.SQLException
import kotlinx.coroutines.CancellationException
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.drive.base.domain.exception.InvalidFieldException
import me.proton.core.drive.base.presentation.R
import me.proton.core.network.data.ProtonErrorException
import me.proton.core.network.domain.ApiException
import me.proton.core.util.kotlin.CoreLogger
import retrofit2.HttpException
import java.io.IOException

fun Throwable.getDefaultMessage(
    context: Context,
    useExceptionMessage: Boolean,
    unhandled: String = context.getString(R.string.common_error_internal),
): String = if (useExceptionMessage) {
    "${this.javaClass.simpleName}: $message"
} else {
    when (this) {
        is ApiException -> getDefaultMessage(context)
        is CancellationException -> getDefaultMessage(context)
        is CryptoException -> getDefaultMessage(context)
        is HttpException -> getDefaultMessage(context)
        is IllegalArgumentException -> getDefaultMessage(context)
        is IllegalStateException -> getDefaultMessage(context)
        is InvalidFieldException -> getDefaultMessage(context)
        is ProtonErrorException -> getDefaultMessage(context)
        is SQLException -> getDefaultMessage(context)
        is UnsupportedOperationException -> getDefaultMessage(context)
        is IOException -> getDefaultMessage(context)
        is RuntimeException -> getDefaultMessage(context)
        else -> unhandled
    }
}

fun Throwable.log(tag: String, message: String? = null): Throwable = this.also {
    when (this) {
        is ApiException -> message?.let { log(tag, message) } ?: log(tag)
        is CancellationException -> message?.let { log(tag, message) } ?: log(tag)
        is CryptoException -> message?.let { log(tag, message) } ?: log(tag)
        is HttpException -> message?.let { log(tag, message) } ?: log(tag)
        is IllegalArgumentException -> message?.let { log(tag, message) } ?: log(tag)
        is IllegalStateException -> message?.let { log(tag, message) } ?: log(tag)
        is ProtonErrorException -> message?.let { log(tag, message) } ?: log(tag)
        is SQLException -> message?.let { log(tag, message) } ?: log(tag)
        is UnsupportedOperationException -> message?.let { log(tag, message) } ?: log(tag)
        is IOException -> message?.let { log(tag, message) } ?: log(tag)
        is RuntimeException -> message?.let { log(tag, message) } ?: log(tag)
        else -> message?.let { CoreLogger.e(tag, this, message) } ?: CoreLogger.e(tag, this)
    }
}

fun <T : Throwable>T.logDefaultMessage(
    context: Context,
    tag: String,
    useExceptionMessage: Boolean = false,
    unhandled: String = context.getString(R.string.common_error_internal),
): String = getDefaultMessage(
    context = context,
    useExceptionMessage = useExceptionMessage,
    unhandled = unhandled,
).also { defaultMessage -> log(tag, defaultMessage) }
