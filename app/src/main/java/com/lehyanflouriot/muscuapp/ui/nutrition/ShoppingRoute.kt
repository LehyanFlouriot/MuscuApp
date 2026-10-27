package com.lehyanflouriot.muscuapp.ui.nutrition

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.domain.buildShoppingList
import com.lehyanflouriot.muscuapp.core.domain.defaultShoppingWeek
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.format.Fmt
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.rememberToday
import java.time.LocalDate

/** The shopping list of the week of [initialWeek], or of the week to shop for when null. */
@Composable
fun ShoppingRoute(
    initialWeek: LocalDate?,
    data: AppData,
    repository: WorkoutRepository,
    onBack: () -> Unit,
) {
    val today = rememberToday()
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    var weekDay by rememberSaveable { mutableLongStateOf((initialWeek ?: defaultShoppingWeek(today)).startOfWeek().toEpochDay()) }
    var fromToday by rememberSaveable { mutableStateOf(false) }
    val weekStart = LocalDate.ofEpochDay(weekDay)
    // Shopping in the middle of the week: only what is left to eat can be listed.
    val partial = fromToday && weekStart == today.startOfWeek() && today != weekStart
    val from = if (partial) today else weekStart
    val nutrition = data.nutrition
    val list = remember(nutrition, from, weekStart) { buildShoppingList(nutrition, from, weekStart.plusDays(6)) }
    val week = nutrition.shoppingWeek(weekStart)

    ShoppingScreen(
        list = list,
        week = week,
        today = today,
        fromToday = partial,
        actions = ShoppingActions(
            onBack = onBack,
            onWeekChange = { weekDay = it.startOfWeek().toEpochDay() },
            onFromTodayChange = { fromToday = it },
            onToggle = { repository.toggleShoppingItem(weekStart, it) },
            onToggleExtra = { repository.toggleShoppingExtra(weekStart, it) },
            onRemoveExtra = { repository.removeShoppingExtra(weekStart, it) },
            onAddExtra = { repository.addShoppingExtra(weekStart, it) },
            onClear = {
                val previous = week
                repository.clearShoppingChecks(weekStart)
                messenger.show(context.getString(R.string.shopping_cleared), context.getString(R.string.action_undo)) {
                    repository.restoreShoppingWeek(previous)
                }
            },
            onShare = {
                val text = shoppingText(context, list, week, Fmt.weekRange(weekStart, today))
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, text)
                runCatching { context.startActivity(Intent.createChooser(send, context.getString(R.string.shopping_share_chooser))) }
            },
        ),
    )
}
