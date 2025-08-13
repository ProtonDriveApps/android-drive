/*
 * Copyright (c) 2023 Proton AG.
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
package me.proton.core.drive.base.data.test.manager

import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.arch.onSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubbedWorkManager @Inject constructor() {

    var behavior: () -> DataResult<String> = BEHAVIOR_SUCCESS

    data class Work(
        val name: String,
        val data: List<Any?>
    )

    val works = MutableStateFlow(listOf<Work>())

    fun add(name: String, vararg data: Any): DataResult<String> = add(Work(name, listOf(*data)))

    private fun add(work: Work) = behavior().onSuccess {
        works.value = works.value + work
    }

    fun execute() {
        works.value = emptyList()
    }

    companion object {
        val BEHAVIOR_SUCCESS = { DataResult.Success(ResponseSource.Local, "") }
        val BEHAVIOR_ERROR = { DataResult.Error.Local("behavior_error", null) }
    }
}

fun StubbedWorkManager.assertHasWorks(name: String) {
    if (works.value.none { it.name == name }) {
        throw AssertionError("Does not contains work: $name in ${works.value.map { it.name }}")
    }
}

fun StubbedWorkManager.assertHasWork(name: String, vararg data: Any) {
    val namedWorks = works.value.filter { it.name == name }
    if (namedWorks.isEmpty()) {
        throw AssertionError("Does not contains work: $name in ${works.value.map { it.name }}")
    }
    val dataAsList = listOf(*data)
    val dataWorks = namedWorks.filter { it.data == dataAsList }
    if (dataWorks.isEmpty()) {
        throw AssertionError("Does not contains work: $name with the same data, expected:$dataAsList but was: ${namedWorks.map { it.data }}")
    } else if (dataWorks.size > 1) {
        throw AssertionError("Does contains more then one work: $name with data: $dataAsList")
    }
}
