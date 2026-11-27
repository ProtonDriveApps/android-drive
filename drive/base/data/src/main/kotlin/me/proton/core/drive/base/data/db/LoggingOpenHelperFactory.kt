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
import android.database.CursorWrapper
import android.database.sqlite.SQLiteTransactionListener
import android.os.CancellationSignal
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import me.proton.core.drive.base.data.extension.logMemoryInfo
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.util.kotlin.CoreLogger

class LoggingOpenHelperFactory<T : RoomDatabase>(
    private val activityManager: ActivityManager,
    private val delegate: SupportSQLiteOpenHelper.Factory,
    private val clazz: Class<T>,
    private val thresholds: SlowDatabaseThresholds = SlowDatabaseThresholds(),
) : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val default = delegate.create(configuration)
        val monitor = SlowDatabaseMonitor(
            database = clazz.simpleName,
            thresholds = thresholds,
            report = { exception ->
                CoreLogger.e(
                    tag = "${LogTag.DATABASE}[${clazz.simpleName}]",
                    e = exception,
                    message = "${exception.message} (${activityManager.logMemoryInfo})",
                )
            },
        )
        return object : SupportSQLiteOpenHelper by default {
            override val readableDatabase: SupportSQLiteDatabase
                get() = MonitoredDatabase(default.readableDatabase, monitor)

            override val writableDatabase: SupportSQLiteDatabase
                get() = MonitoredDatabase(default.writableDatabase, monitor)
        }
    }
}

internal class MonitoredDatabase(
    private val delegate: SupportSQLiteDatabase,
    private val monitor: SlowDatabaseMonitor,
) : SupportSQLiteDatabase by delegate {

    override fun query(query: String): Cursor =
        monitoredQuery(query) { delegate.query(query) }

    override fun query(query: String, bindArgs: Array<out Any?>): Cursor =
        monitoredQuery(query) { delegate.query(query, bindArgs) }

    override fun query(query: SupportSQLiteQuery): Cursor =
        monitoredQuery(query.sql) { delegate.query(query) }

    override fun query(query: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor =
        monitoredQuery(query.sql) { delegate.query(query, cancellationSignal) }

    override fun execSQL(sql: String) =
        monitor.statement(sql) { delegate.execSQL(sql) }

    override fun execSQL(sql: String, bindArgs: Array<out Any?>) =
        monitor.statement(sql) { delegate.execSQL(sql, bindArgs) }

    override fun compileStatement(sql: String): SupportSQLiteStatement =
        MonitoredStatement(delegate.compileStatement(sql), sql, monitor)

    override fun beginTransaction() =
        monitor.beginTransaction(isWrite = true) { delegate.beginTransaction() }

    override fun beginTransactionNonExclusive() =
        monitor.beginTransaction(isWrite = true) { delegate.beginTransactionNonExclusive() }

    override fun beginTransactionReadOnly() =
        monitor.beginTransaction(isWrite = false) { delegate.beginTransactionReadOnly() }

    override fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener) =
        monitor.beginTransaction(isWrite = true) { delegate.beginTransactionWithListener(transactionListener) }

    override fun beginTransactionWithListenerNonExclusive(transactionListener: SQLiteTransactionListener) =
        monitor.beginTransaction(isWrite = true) {
            delegate.beginTransactionWithListenerNonExclusive(transactionListener)
        }

    override fun beginTransactionWithListenerReadOnly(transactionListener: SQLiteTransactionListener) =
        monitor.beginTransaction(isWrite = false) {
            delegate.beginTransactionWithListenerReadOnly(transactionListener)
        }

    override fun endTransaction() =
        monitor.endTransaction { delegate.endTransaction() }

    private fun monitoredQuery(sql: String, block: () -> Cursor): Cursor {
        monitor.onQueryStarted(sql)
        val isMainThread = monitor.isMainThread()
        val start = monitor.nanoTime()
        val cursor = try {
            block()
        } catch (error: Throwable) {
            monitor.onRead(sql, monitor.nanoTime() - start, isMainThread)
            throw error
        }
        return MonitoredCursor(cursor, sql, monitor, monitor.nanoTime() - start, isMainThread)
    }
}

private class MonitoredStatement(
    private val delegate: SupportSQLiteStatement,
    private val sql: String,
    private val monitor: SlowDatabaseMonitor,
) : SupportSQLiteStatement by delegate {

    override fun execute() = monitor.statement(sql) { delegate.execute() }

    override fun executeInsert(): Long = monitor.statement(sql) { delegate.executeInsert() }

    override fun executeUpdateDelete(): Int = monitor.statement(sql) { delegate.executeUpdateDelete() }

    override fun simpleQueryForLong(): Long = monitor.read(sql) { delegate.simpleQueryForLong() }

    override fun simpleQueryForString(): String? = monitor.read(sql) { delegate.simpleQueryForString() }
}

private class MonitoredCursor(
    delegate: Cursor,
    private val sql: String,
    private val monitor: SlowDatabaseMonitor,
    private var elapsedNanos: Long,
    private val isMainThread: Boolean,
) : CursorWrapper(delegate) {
    private var reported = false

    override fun getCount(): Int = timed { super.getCount() }

    override fun move(offset: Int): Boolean = timed { super.move(offset) }

    override fun moveToPosition(position: Int): Boolean = timed { super.moveToPosition(position) }

    override fun moveToFirst(): Boolean = timed { super.moveToFirst() }

    override fun moveToLast(): Boolean = timed { super.moveToLast() }

    override fun moveToNext(): Boolean = timed { super.moveToNext() }

    override fun moveToPrevious(): Boolean = timed { super.moveToPrevious() }

    override fun close() {
        super.close()
        if (!reported) {
            reported = true
            monitor.onRead(sql, elapsedNanos, isMainThread)
        }
    }

    private inline fun <R> timed(block: () -> R): R {
        val start = monitor.nanoTime()
        try {
            return block()
        } finally {
            elapsedNanos += monitor.nanoTime() - start
        }
    }
}
