/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.base.data.db

import android.app.ActivityManager
import android.database.Cursor
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import me.proton.core.drive.base.data.entity.LoggerLevel
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.extension.logMemoryInfo
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.util.kotlin.CoreLogger
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

class LoggingOpenHelperFactory<T : RoomDatabase>(
    private val activityManager: ActivityManager,
    private val delegate: SupportSQLiteOpenHelper.Factory,
    private val clazz: Class<T>,
    private val slowThreshold: Duration = 20.milliseconds,
    private val captureStackForSlow: Duration = 300.milliseconds,
) : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val default = delegate.create(configuration)
        val loggingDatabase = {
            LoggingDatabase(
                activityManager = activityManager,
                delegate = default.readableDatabase,
                clazz = clazz,
                slowThreshold = slowThreshold,
                captureStackForSlow = captureStackForSlow,
            )
        }
        return object : SupportSQLiteOpenHelper by default {
            override val readableDatabase: SupportSQLiteDatabase
                get() = loggingDatabase.invoke()

            override val writableDatabase: SupportSQLiteDatabase
                get() = loggingDatabase.invoke()
        }
    }
}

private class SlowQueryException(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

private class LoggingDatabase<T : RoomDatabase>(
    private val activityManager: ActivityManager,
    private val delegate: SupportSQLiteDatabase,
    private val slowThreshold: Duration,
    private val captureStackForSlow: Duration,
    private val clazz: Class<T>,
) : SupportSQLiteDatabase by delegate {

    private val Duration.shouldLog: Boolean get() = this > slowThreshold
    private val Duration.throwable: Throwable? get() = takeIf {
        this > captureStackForSlow && captureStackForSlow > Duration.ZERO
    }?.let {
        SlowQueryException("Slow DB query detected (${activityManager.logMemoryInfo}).").apply {
            stackTrace = Thread.currentThread().stackTrace
        }
    }
    private fun Duration.log(message: String) {
        if (shouldLog) {
            val tag = "${LogTag.DATABASE}[${clazz.simpleName}]"
            throwable?.log(
                tag = tag,
                message = message,
                level = LoggerLevel.WARNING,
            ) ?: CoreLogger.v(
                tag = tag,
                message = message + "\n${activityManager.logMemoryInfo}",
            )
        }
    }

    override fun query(query: String): Cursor {
        val cursor: Cursor
        measureNanoTime { cursor = delegate.query(query) }
            .nanoseconds
            .apply {
                log("SQL query took $inWholeMilliseconds ms, $query")
            }
        return cursor
    }

    override fun query(query: String, bindArgs: Array<out Any?>): Cursor {
        val cursor: Cursor
        measureNanoTime { cursor = delegate.query(query, bindArgs) }
            .nanoseconds
            .apply {
                val argsPreview = bindArgs.joinToString(prefix = "[", postfix = "]")
                log("SQL query took $inWholeMilliseconds ms, $query, args=$argsPreview")
            }
        return cursor
    }

    override fun query(query: SupportSQLiteQuery): Cursor {
        val sql = try { query.sql } catch (_: Exception) { "<unknown>" }
        val cursor: Cursor
        measureNanoTime { cursor = delegate.query(query) }
            .nanoseconds
            .apply {
                log("SQL query (SupportSQLiteQuery) took $inWholeMilliseconds ms, $sql")
            }
        return cursor
    }

    override fun execSQL(sql: String) {
        measureNanoTime { delegate.execSQL(sql) }
            .nanoseconds
            .apply {
                log("execSQL took $inWholeMilliseconds ms, $sql")
            }
    }

    override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
        measureNanoTime { delegate.execSQL(sql, bindArgs) }
            .nanoseconds
            .apply {
                val argsPreview = bindArgs.joinToString(prefix = "[", postfix = "]")
                log("execSQL took $inWholeMilliseconds ms, $sql, args=$argsPreview")
            }
    }

    override fun compileStatement(sql: String): SupportSQLiteStatement = LoggingStatement(
        activityManager = activityManager,
        delegate = delegate.compileStatement(sql),
        clazz = clazz,
        sqlPreview = sql,
        slowThreshold = slowThreshold,
        captureStackForSlow = captureStackForSlow,
    )
}

private class LoggingStatement<T : RoomDatabase>(
    private val activityManager: ActivityManager,
    private val delegate: SupportSQLiteStatement,
    private val clazz: Class<T>,
    private val sqlPreview: String,
    private val slowThreshold: Duration,
    private val captureStackForSlow: Duration,
) : SupportSQLiteStatement by delegate {
    private val Duration.shouldLog: Boolean get() = this > slowThreshold
    private val Duration.throwable: Throwable? get() = takeIf {
        this > captureStackForSlow && captureStackForSlow > Duration.ZERO
    }?.let {
        SlowQueryException("Slow DB query detected (${activityManager.logMemoryInfo}).").apply {
            stackTrace = Thread.currentThread().stackTrace
        }
    }
    private fun Duration.log(message: String) {
        if (shouldLog) {
            val tag = "${LogTag.DATABASE}[${clazz.simpleName}]"
            throwable?.log(
                tag = tag,
                message = message,
            ) ?: CoreLogger.d(
                tag = tag,
                message = message + "\n${activityManager.logMemoryInfo}",
            )
        }
    }

    override fun executeInsert(): Long {
        val res: Long
        measureNanoTime { res = delegate.executeInsert() }
            .nanoseconds
            .apply {
                log("Statement.executeInsert took $inWholeMilliseconds ms, $sqlPreview")
            }
        return res
    }

    override fun executeUpdateDelete(): Int {
        val res: Int
        measureNanoTime { res = delegate.executeUpdateDelete() }
            .nanoseconds
            .apply {
                log("Statement.executeUpdateDelete took $inWholeMilliseconds ms, $sqlPreview")
            }
        return res
    }

    override fun execute() {
        measureNanoTime { delegate.execute() }
            .nanoseconds
            .apply {
                log("Statement.execute took $inWholeMilliseconds ms, $sqlPreview")
            }
    }

    override fun simpleQueryForLong(): Long {
        val res: Long
        measureNanoTime { res = delegate.simpleQueryForLong() }
            .nanoseconds
            .apply {
                log("Statement.simpleQueryForLong took $inWholeMilliseconds ms, $sqlPreview")
            }
        return res
    }

    override fun simpleQueryForString(): String {
        val res: String
        measureNanoTime { res = delegate.simpleQueryForString() ?: "" }
            .nanoseconds
            .apply {
                log("Statement.simpleQueryForString took $inWholeMilliseconds ms, $sqlPreview")
            }
        return res
    }
}
