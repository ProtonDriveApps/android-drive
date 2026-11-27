/*
 * Copyright (c) 2026 Proton AG.
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

import android.os.Looper
import me.proton.core.drive.base.data.db.SlowDatabaseOperationException.Operation
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

data class SlowDatabaseThresholds(
    val mainThread: Duration = 100.milliseconds,
    val read: Duration = 1.seconds,
    val statement: Duration = 1.seconds,
    val lockWait: Duration = 1.seconds,
    val writeTransaction: Duration = 2.seconds,
)

internal class SlowDatabaseMonitor(
    private val database: String,
    private val thresholds: SlowDatabaseThresholds,
    private val report: (SlowDatabaseOperationException) -> Unit,
    val nanoTime: () -> Long = System::nanoTime,
    val isMainThread: () -> Boolean = { Looper.myLooper() == Looper.getMainLooper() },
) {
    private val reported = ConcurrentHashMap.newKeySet<List<String>>()
    private val transaction = ThreadLocal<Transaction?>()

    fun onQueryStarted(sql: String) {
        transaction.get()?.recordFirst(sql)
    }

    fun onRead(sql: String, elapsedNanos: Long, isMainThread: Boolean) {
        val limit = if (isMainThread) thresholds.mainThread else thresholds.read
        reportIfSlow(Operation.READ, sql, elapsedNanos, isMainThread, limit)
    }

    fun <R> read(sql: String, block: () -> R): R {
        onQueryStarted(sql)
        val isMainThread = isMainThread()
        val start = nanoTime()
        try {
            return block()
        } finally {
            onRead(sql, nanoTime() - start, isMainThread)
        }
    }

    fun <R> statement(sql: String, block: () -> R): R {
        val current = transaction.get()
        current?.recordFirst(sql)
        val isMainThread = isMainThread()
        val start = nanoTime()
        try {
            return block()
        } finally {
            val limit = when {
                isMainThread -> thresholds.mainThread
                current != null -> null
                else -> thresholds.statement
            }
            reportIfSlow(Operation.STATEMENT, sql, nanoTime() - start, isMainThread, limit)
        }
    }

    fun beginTransaction(isWrite: Boolean, block: () -> Unit) {
        val current = transaction.get()
        if (current != null) {
            block()
            current.depth++
            return
        }
        val isMainThread = isMainThread()
        val limit = if (isMainThread) thresholds.mainThread else thresholds.lockWait
        val start = nanoTime()
        val acquired = try {
            block()
            nanoTime()
        } catch (error: Throwable) {
            reportIfSlow(Operation.LOCK_WAIT, null, nanoTime() - start, isMainThread, limit)
            throw error
        }
        reportIfSlow(Operation.LOCK_WAIT, null, acquired - start, isMainThread, limit)
        transaction.set(Transaction(isWrite = isWrite, startNanos = acquired, isMainThread = isMainThread))
    }

    fun endTransaction(block: () -> Unit) {
        try {
            block()
        } finally {
            transaction.get()?.let { current ->
                current.depth--
                if (current.depth == 0) {
                    transaction.remove()
                    onTransactionEnded(current)
                }
            }
        }
    }

    private fun onTransactionEnded(current: Transaction) {
        val limit = when {
            !current.isWrite -> null
            current.isMainThread -> thresholds.mainThread
            else -> thresholds.writeTransaction
        }
        reportIfSlow(
            operation = Operation.WRITE_TRANSACTION,
            sql = current.firstSql,
            elapsedNanos = nanoTime() - current.startNanos,
            isMainThread = current.isMainThread,
            limit = limit,
        )
    }

    private fun reportIfSlow(
        operation: Operation,
        sql: String?,
        elapsedNanos: Long,
        isMainThread: Boolean,
        limit: Duration?,
    ) {
        val elapsed = elapsedNanos.nanoseconds
        if (limit == null || elapsed <= limit) return
        val exception = SlowDatabaseOperationException(
            database = database,
            operation = operation,
            durationMs = elapsed.inWholeMilliseconds,
            isMainThread = isMainThread,
            sql = sql?.let(::normalizeSql),
            message = buildString {
                append("Slow database ${operation.name.lowercase()} in $database took ")
                append("${elapsed.inWholeMilliseconds} ms")
                if (isMainThread) append(" on the main thread")
            },
        )
        if (reported.add(exception.fingerprint)) {
            report(exception)
        }
    }

    private class Transaction(
        val isWrite: Boolean,
        val startNanos: Long,
        val isMainThread: Boolean,
    ) {
        var depth: Int = 1
        var firstSql: String? = null
            private set

        fun recordFirst(sql: String) {
            if (firstSql == null) firstSql = sql
        }
    }
}

internal fun normalizeSql(sql: String): String = sql
    .replace(STRING_LITERAL, "?")
    .replace(NUMBER_LITERAL, "?")
    .replace(PLACEHOLDER_LIST, "?")
    .replace(WHITESPACE, " ")
    .trim()
    .take(MAX_SQL_LENGTH)

private val STRING_LITERAL = Regex("""'(?:[^']|'')*'""")
private val NUMBER_LITERAL = Regex("""(?<![\w.])-?\d+(?:\.\d+)?(?![\w.])""")
private val PLACEHOLDER_LIST = Regex("""\?(?:\s*,\s*\?)+""")
private val WHITESPACE = Regex("""\s+""")
private const val MAX_SQL_LENGTH = 2000
