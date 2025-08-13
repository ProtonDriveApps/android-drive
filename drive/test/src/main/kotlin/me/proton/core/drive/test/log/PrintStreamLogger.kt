/*
 * Copyright (c) 2024 Proton AG. 
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

package me.proton.core.drive.test.log

import me.proton.core.util.kotlin.Logger
import java.io.PrintStream

class PrintStreamLogger(
    private val outStream: PrintStream = System.out,
    private val errStream: PrintStream = System.err,
) : Logger {
    override fun d(tag: String, message: String) {
        outStream.println("$tag: $message")
    }

    override fun d(tag: String, e: Throwable, message: String) {
        errStream.println("$tag: $message")
        e.printStackTrace(errStream)
    }

    override fun e(tag: String, message: String) {
        errStream.println("$tag: $message")
    }

    override fun e(tag: String, e: Throwable) {
        errStream.println("$tag:")
        e.printStackTrace(errStream)
    }

    override fun e(tag: String, e: Throwable, message: String) {
        errStream.println("$tag: $message")
        e.printStackTrace(errStream)
    }

    override fun i(tag: String, message: String) {
        outStream.println("$tag: $message")
    }

    override fun i(tag: String, e: Throwable, message: String) {
        errStream.println("$tag: $message")
        e.printStackTrace(errStream)
    }

    override fun v(tag: String, message: String) {
        outStream.println("$tag: $message")
    }

    override fun v(tag: String, e: Throwable, message: String) {
        errStream.println("$tag:")
        e.printStackTrace(errStream)
    }

    override fun w(tag: String, message: String) {
        outStream.println("$tag: $message")
    }

    override fun w(tag: String, e: Throwable) {
        errStream.println("$tag:")
        e.printStackTrace(errStream)
    }

    override fun w(tag: String, e: Throwable, message: String) {
        errStream.println("$tag: $message")
        e.printStackTrace(errStream)
    }

}

