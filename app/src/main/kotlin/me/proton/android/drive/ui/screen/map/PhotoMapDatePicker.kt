/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.android.drive.ui.screen.map

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.DatePicker as WheelDatePicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToLong
import me.proton.android.drive.R
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
internal fun PhotoMapDatePicker(
    initialRange: MapDateRange,
    zone: ZoneId,
    onDismiss: () -> Unit,
    onConfirm: (MapDateRange) -> Unit,
) {
    val today = remember(zone) { LocalDate.now(zone) }
    var startDay by rememberSaveable { mutableLongStateOf(initialRange.start.toEpochDay()) }
    var endDay by rememberSaveable { mutableLongStateOf(initialRange.end.toEpochDay()) }
    var sliderStart by rememberSaveable { mutableLongStateOf(minOf(initialRange.start, today.minusYears(1)).toEpochDay()) }
    var sliderEnd by rememberSaveable { mutableLongStateOf(maxOf(initialRange.end, today).toEpochDay()) }
    var editing by rememberSaveable { mutableStateOf<MapDateEndpoint?>(null) }
    var useCalendar by rememberSaveable { mutableStateOf(false) }
    val range = MapDateRange(LocalDate.ofEpochDay(startDay), LocalDate.ofEpochDay(endDay))
    val interaction = ProtonTheme.colors.interactionNorm
    val colors = if (ProtonTheme.colors.isDark) darkColorScheme(primary = interaction) else lightColorScheme(primary = interaction)
    MaterialTheme(colorScheme = colors) {
        val endpoint = editing
        if (endpoint == null) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(I18N.string.photos_map_select_dates)) },
                text = {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DateEndpointControl(
                                endpoint = MapDateEndpoint.START,
                                date = range.start,
                                modifier = Modifier.weight(1f),
                                onEdit = {
                                    useCalendar = it
                                    editing = MapDateEndpoint.START
                                },
                            )
                            DateEndpointControl(
                                endpoint = MapDateEndpoint.END,
                                date = range.end,
                                modifier = Modifier.weight(1f),
                                onEdit = {
                                    useCalendar = it
                                    editing = MapDateEndpoint.END
                                },
                            )
                        }
                        RangeSlider(
                            value = startDay.toFloat()..endDay.toFloat(),
                            valueRange = sliderStart.toFloat()..sliderEnd.toFloat(),
                            onValueChange = {
                                startDay = it.start.roundToLong()
                                endDay = it.endInclusive.roundToLong()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onConfirm(range) }) { Text(stringResource(I18N.string.common_done_action)) }
                },
                dismissButton = { DatePickerCancel(onDismiss) },
            )
        } else {
            val allowed = endpoint.allowedDates(range, today)
            val dismissEditor = { editing = null }
            val setDate: (LocalDate) -> Unit = { date ->
                val updated = endpoint.withDate(range, date)
                startDay = updated.start.toEpochDay()
                endDay = updated.end.toEpochDay()
                sliderStart = minOf(sliderStart, startDay)
                sliderEnd = maxOf(sliderEnd, endDay)
                editing = null
            }
            if (useCalendar) {
                CalendarDateDialog(endpoint, endpoint.date(range), allowed, dismissEditor, setDate)
            } else {
                WheelDateDialog(endpoint, endpoint.date(range), allowed, zone, dismissEditor, setDate)
            }
        }
    }
}

@Composable
private fun DateEndpointControl(
    endpoint: MapDateEndpoint,
    date: LocalDate,
    modifier: Modifier,
    onEdit: (calendar: Boolean) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    Column(modifier) {
        Text(stringResource(endpoint.label), style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = { onEdit(false) }) {
            Text(date.format(formatter))
        }
        IconButton(onClick = { onEdit(true) }) {
            Icon(
                painter = painterResource(CorePresentation.drawable.ic_proton_calendar_grid),
                contentDescription = stringResource(endpoint.calendarLabel),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDateDialog(
    endpoint: MapDateEndpoint,
    selected: LocalDate,
    allowed: MapDateRange,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val selectable = remember(allowed) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis.toUtcDate() in allowed.start..allowed.end
        }
    }
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = selected.toUtcMillis(),
        yearRange = allowed.start.year..allowed.end.year,
        selectableDates = selectable,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = picker.selectedDateMillis != null,
                onClick = { picker.selectedDateMillis?.toUtcDate()?.let(onConfirm) },
            ) { Text(stringResource(I18N.string.common_done_action)) }
        },
        dismissButton = { DatePickerCancel(onDismiss) },
    ) {
        DatePicker(
            state = picker,
            modifier = Modifier.verticalScroll(rememberScrollState()),
            title = {
                Text(
                    stringResource(endpoint.label),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                )
            },
            showModeToggle = false,
        )
    }
}

@Composable
private fun WheelDateDialog(
    endpoint: MapDateEndpoint,
    selected: LocalDate,
    allowed: MapDateRange,
    zone: ZoneId,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    var selectedDay by rememberSaveable { mutableLongStateOf(selected.toEpochDay()) }
    var picker by remember { mutableStateOf<WheelDatePicker?>(null) }
    val nativeTheme = if (ProtonTheme.colors.isDark) android.R.style.Theme_Material else android.R.style.Theme_Material_Light
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(endpoint.label)) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AndroidView(factory = { context ->
                    val themedContext = ContextThemeWrapper(context, nativeTheme)
                    (LayoutInflater.from(themedContext).inflate(R.layout.photo_map_date_wheels, null) as WheelDatePicker).apply {
                        val earliest = allowed.start.atStartOfDay(zone).toInstant().toEpochMilli()
                        val latest = allowed.end.atStartOfDay(zone).toInstant().toEpochMilli()
                        if (latest > maxDate) maxDate = latest
                        if (earliest < minDate) minDate = earliest
                        minDate = earliest
                        maxDate = latest
                        val date = LocalDate.ofEpochDay(selectedDay)
                        init(date.year, date.monthValue - 1, date.dayOfMonth) { _, year, month, day ->
                            selectedDay = LocalDate.of(year, month + 1, day).toEpochDay()
                        }
                        picker = this
                    }
                })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                picker?.clearFocus()
                onConfirm(LocalDate.ofEpochDay(selectedDay))
            }) { Text(stringResource(I18N.string.common_done_action)) }
        },
        dismissButton = { DatePickerCancel(onDismiss) },
    )
}

@Composable
private fun DatePickerCancel(onDismiss: () -> Unit) {
    TextButton(onClick = onDismiss) { Text(stringResource(I18N.string.common_cancel_action)) }
}

internal enum class MapDateEndpoint(val label: Int, val calendarLabel: Int) {
    START(I18N.string.photos_map_start_date, I18N.string.photos_map_start_calendar),
    END(I18N.string.photos_map_end_date, I18N.string.photos_map_end_calendar);

    fun date(range: MapDateRange): LocalDate = if (this == START) range.start else range.end

    fun allowedDates(range: MapDateRange, today: LocalDate): MapDateRange = if (this == START) {
        MapDateRange(minOf(range.start, today.minusYears(100).withDayOfYear(1)), range.end)
    } else {
        MapDateRange(range.start, maxOf(range.end, today.plusYears(100).withMonth(12).withDayOfMonth(31)))
    }

    fun withDate(range: MapDateRange, date: LocalDate): MapDateRange = if (this == START) {
        range.copy(start = minOf(date, range.end))
    } else {
        range.copy(end = maxOf(date, range.start))
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atOffset(ZoneOffset.UTC).toLocalDate()
