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
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.drive.announce.event.domain.usecase.AsyncAnnounceEvent
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.percentageOfAsciiChars
import me.proton.core.drive.base.domain.usecase.DeviceInfo
import me.proton.core.util.android.sentry.TimberLogger
import me.proton.core.util.kotlin.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import me.proton.core.drive.i18n.R as I18N

@Singleton
class DriveLogger @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val deviceInfo: DeviceInfo,
    asyncAnnounceEvent: AsyncAnnounceEvent,
    accountManager: AccountManager,
    coroutineContext: CoroutineContext,
) : UserLogger(asyncAnnounceEvent, accountManager, coroutineContext) {

    override fun d(tag: String, message: String) {
        super.d(tag, message)
        TimberLogger.d(tag, message)
    }

    override fun d(tag: String, e: Throwable, message: String) {
        super.d(tag, e, message)
        TimberLogger.d(tag, e, message)
    }

    override fun e(tag: String, message: String) {
        super.e(tag, message)
        TimberLogger.e(tag, message)
        DriveSentry.setInternalErrorTag(appContext, message)
    }

    override fun e(tag: String, e: Throwable) {
        super.e(tag, e)
        TimberLogger.e(tag, e)
        DriveSentry.setInternalErrorTag(appContext, e)
    }

    override fun e(tag: String, e: Throwable, message: String) {
        super.e(tag, e, message)
        TimberLogger.e(tag, e, message)
        DriveSentry.setInternalErrorTag(appContext, e)
    }

    override fun i(tag: String, message: String) {
        super.i(tag, message)
        TimberLogger.i(tag, message)
    }

    override fun i(tag: String, e: Throwable, message: String) {
        super.i(tag, e, message)
        TimberLogger.i(tag, e, message)
    }

    override fun v(tag: String, message: String) {
        super.v(tag, message)
        TimberLogger.v(tag, message)
    }

    override fun v(tag: String, e: Throwable, message: String) {
        super.v(tag, e, message)
        TimberLogger.v(tag, e, message)
    }

    override fun w(tag: String, message: String) {
        super.w(tag, message)
        TimberLogger.w(tag, message)
    }

    override fun w(tag: String, e: Throwable) {
        super.w(tag, e)
        TimberLogger.w(tag, e)
    }

    override fun w(tag: String, e: Throwable, message: String) {
        super.w(tag, e, message)
        TimberLogger.w(tag, e, message)
    }

    fun deviceInfo() {
        deviceInfo { info ->
            i(info)
        }
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

fun Logger.v(message: String) = v(DriveLogTag.DEFAULT, message)
fun Logger.d(message: String) = d(DriveLogTag.DEFAULT, message)
fun Logger.i(message: String) = i(DriveLogTag.DEFAULT, message)
fun Logger.w(message: String) = w(DriveLogTag.DEFAULT, message)
fun Logger.e(throwable: Throwable) = e(DriveLogTag.DEFAULT, throwable)
