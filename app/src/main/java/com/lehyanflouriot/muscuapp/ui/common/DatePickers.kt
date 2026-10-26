package com.lehyanflouriot.muscuapp.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.format.Fmt
import java.time.LocalDate

/** "Aujourd'hui", "Hier" and "Autre jour" chips to choose the day of what is noted. */
@Composable
fun DateChips(
    date: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onPickDate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val yesterday = today.minusDays(1)
    val custom = date != today && date != yesterday
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = date == today,
            onClick = { onSelect(today) },
            label = { Text(stringResource(R.string.date_today)) },
        )
        FilterChip(
            selected = date == yesterday,
            onClick = { onSelect(yesterday) },
            label = { Text(stringResource(R.string.date_yesterday)) },
        )
        FilterChip(
            selected = custom,
            onClick = onPickDate,
            label = { Text(if (custom) Fmt.day(date) else stringResource(R.string.date_other)) },
            leadingIcon = {
                Icon(
                    Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )
    }
}

/** Calendar where only today and the days before can be chosen. */
@Composable
fun PastDatePickerDialog(
    initialDate: LocalDate,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val todayMillis = today.toUtcMillis()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis
            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = pickerState.selectedDateMillis
                    if (selected != null) onConfirm(utcMillisToLocalDate(selected)) else onDismiss()
                },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}
