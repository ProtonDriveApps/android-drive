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
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message
import me.proton.core.drive.base.presentation.extension.getDefaultMessage
import me.proton.core.util.kotlin.Logger
import me.proton.core.util.kotlin.LoggerLogTag
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import me.proton.core.drive.base.presentation.R as BasePresentation

@Singleton
class DriveLogger @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : Logger {

    override fun v(tag: String, message: String) {
        DriveSentry.addBreadcrumb(tag, message)
        Timber.tag(tag).v(message)
    }
    override fun v(tag: String, e: Throwable, message: String) {
        DriveSentry.addBreadcrumb(tag, e, message)
        Timber.tag(tag).v(e, message)
    }
    override fun d(tag: String, message: String) {
        DriveSentry.addBreadcrumb(tag, message)
        Timber.tag(tag).d(message)
    }
    override fun d(tag: String, e: Throwable, message: String) {
        DriveSentry.addBreadcrumb(tag, e, message)
        Timber.tag(tag).d(e, message)
    }
    override fun i(tag: String, message: String) {
        DriveSentry.addBreadcrumb(tag, message, SentryLevel.INFO)
        Timber.tag(tag).i(message)
    }
    override fun i(tag: String, e: Throwable, message: String) {
        DriveSentry.addBreadcrumb(tag, e, message, SentryLevel.INFO)
        Timber.tag(tag).i(e, message)
    }
    override fun e(tag: String, e: Throwable) {
        DriveSentry.captureException(appContext, tag, e)
        Timber.tag(tag).e(e)
    }
    override fun e(tag: String, e: Throwable, message: String) {
        DriveSentry.captureException(appContext, tag, e, message)
        Timber.tag(tag).e(e, message)
    }
    override fun log(tag: LoggerLogTag, message: String) = i(tag.name, message)

    private fun withoutUploadFileContent(tag: String, message: String, block: (tag: String, message: String) -> Unit) {
        val isCoreNetwork = tag.startsWith("core.network")
        var numAsciiChars = 0
        message.forEach { c ->
            if (c.code > 33 && c.code < 127) numAsciiChars += 1
        }
        if (isCoreNetwork && numAsciiChars < (message.length / 2)) {
            // upload file content detected, do nothing
        } else {
            block(tag, message)
        }
    }

    private object DriveSentry {

        fun captureException(
            context: Context,
            tag: String,
            e: Throwable,
        ) {
            setInternalErrorTag(context, e)
            Sentry.setTag("CoreLogger", tag)
            Sentry.captureException(e)
        }

        fun captureException(
            context: Context,
            tag: String,
            e: Throwable,
            message: String,
            level: SentryLevel = SentryLevel.ERROR,
        ) {
            setInternalErrorTag(context, e)
            Sentry.setTag("CoreLogger", tag)
            Sentry.captureEvent(
                SentryEvent(e).apply {
                    this.level = level
                    this.message = Message().apply {
                        this.message = message
                    }
                }
            )
        }

        fun addBreadcrumb(tag: String, message: String, level: SentryLevel = SentryLevel.DEBUG) {
            Sentry.addBreadcrumb(
                Breadcrumb().apply {
                    this.category = tag.substringAfterLast('.')
                    this.level = level
                    this.message = message
                }
            )
        }

        fun addBreadcrumb(tag: String, e: Throwable, message: String, level: SentryLevel = SentryLevel.DEBUG) {
            Sentry.addBreadcrumb(
                Breadcrumb().apply {
                    this.category = tag.substringAfterLast('.')
                    this.level = level
                    this.message = message
                    this.setData("Throwable", e.stackTraceToString())
                }
            )
        }

        private fun setInternalErrorTag(context: Context, e: Throwable) {
            val internalErrorMessage = context.getString(BasePresentation.string.common_error_internal)
            val errorMessage = e.getDefaultMessage(
                context = context,
                useExceptionMessage = false,
            )
            Sentry.setTag("InternalError", (errorMessage == internalErrorMessage).toString())
        }
    }
}

class NoOpLogger : Logger {
    override fun d(tag: String, message: String) = Unit
    override fun d(tag: String, e: Throwable, message: String) = Unit
    override fun e(tag: String, e: Throwable) = Unit
    override fun e(tag: String, e: Throwable, message: String) = Unit
    override fun i(tag: String, message: String) = Unit
    override fun i(tag: String, e: Throwable, message: String) = Unit
    override fun log(tag: LoggerLogTag, message: String) = Unit
    override fun v(tag: String, message: String) = Unit
    override fun v(tag: String, e: Throwable, message: String) = Unit
}

fun Logger.v(message: String) = v(DriveLogTag.DEFAULT, message)
fun Logger.d(message: String) = d(DriveLogTag.DEFAULT, message)
fun Logger.i(message: String) = i(DriveLogTag.DEFAULT, message)
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
    i("-----------------------------------------")
}
