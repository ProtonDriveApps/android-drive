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

@file:Suppress("unused")

package me.proton.android.drive.log

import android.content.Context
import android.os.Build
import android.os.LocaleList
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import me.proton.android.drive.BuildConfig
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.percentageOfAsciiChars
import me.proton.core.util.android.sentry.TimberLogger
import me.proton.core.util.kotlin.Logger
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import me.proton.core.drive.i18n.R as I18N

@Singleton
class DriveLogger @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : Logger by TimberLogger {

    override fun e(tag: String, e: Throwable) {
        DriveSentry.setInternalErrorTag(appContext, e)
        TimberLogger.e(tag, e)
    }

    override fun e(tag: String, message: String) {
        DriveSentry.setInternalErrorTag(appContext, message)
        TimberLogger.e(tag, message)
    }

    override fun e(tag: String, e: Throwable, message: String) {
        DriveSentry.setInternalErrorTag(appContext, e)
        TimberLogger.e(tag, e, message)
    }

    private fun withoutUploadFileContent(tag: String, message: String, block: (tag: String, message: String) -> Unit) {
        val isCoreNetwork = tag.startsWith("core.network")
        if (isCoreNetwork && message.percentageOfAsciiChars < Percentage(50)) {
            // upload file content detected, do nothing
        } else {
            block(tag, message)
        }
    }

    private object DriveSentry {
        fun setInternalErrorTag(context: Context, e: Throwable) {
            setInternalErrorTag(
                context = context,
                errorMessage = e.getDefaultMessage(
                    context = context,
                    useExceptionMessage = false,
                )
            )
        }

        fun setInternalErrorTag(context: Context, errorMessage: String) {
            val internalErrorMessage = context.getString(I18N.string.common_error_internal)
            Sentry.setTag("InternalError", (errorMessage == internalErrorMessage).toString())
        }
    }
}

class NoOpLogger : Logger {
    override fun d(tag: String, message: String) = Unit
    override fun d(tag: String, e: Throwable, message: String) = Unit
    override fun e(tag: String, message: String) = Unit
    override fun e(tag: String, e: Throwable) = Unit
    override fun e(tag: String, e: Throwable, message: String) = Unit
    override fun w(tag: String, message: String) = Unit
    override fun w(tag: String, e: Throwable) = Unit
    override fun w(tag: String, e: Throwable, message: String) = Unit
    override fun i(tag: String, message: String) = Unit
    override fun i(tag: String, e: Throwable, message: String) = Unit
    override fun v(tag: String, message: String) = Unit
    override fun v(tag: String, e: Throwable, message: String) = Unit
}

fun Logger.v(message: String) = v(DriveLogTag.DEFAULT, message)
fun Logger.d(message: String) = d(DriveLogTag.DEFAULT, message)
fun Logger.i(message: String) = i(DriveLogTag.DEFAULT, message)
fun Logger.w(message: String) = w(DriveLogTag.DEFAULT, message)
fun Logger.e(throwable: Throwable) = e(DriveLogTag.DEFAULT, throwable)

fun Logger.deviceInfo() {
    i("-----------------------------------------")
    i("OS:          Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    i("DEVICE:      ${Build.MANUFACTURER} ${Build.MODEL}")
    i("FINGERPRINT: ${Build.FINGERPRINT}")
    i("ABI:         ${Build.SUPPORTED_ABIS.joinToString(",")}")
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        i("LOCALE:      ${Locale.getDefault().toLanguageTag()}")
    } else {
        i("LOCALE:      ${LocaleList.getDefault().toLanguageTags()}")
    }
    i("APP VERSION: ${BuildConfig.VERSION_NAME}")
    i("-----------------------------------------")
}
