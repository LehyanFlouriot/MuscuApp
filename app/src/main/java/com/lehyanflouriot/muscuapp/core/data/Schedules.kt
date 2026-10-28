package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.domain.periodAt
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import java.time.LocalDate

/** Sorted by date, without a period that only goes on with the plan of the one before. */
internal fun List<PlanPeriod>.merged(): List<PlanPeriod> {
    val sorted = sortedBy { it.from }
    return sorted.filterIndexed { index, period -> index == 0 || sorted[index - 1].planId != period.planId }
}

/** Follows [planId] from [from] on, until the next period already planned. */
internal fun List<PlanPeriod>.starting(planId: String, from: LocalDate): List<PlanPeriod> =
    if (periodAt(from)?.planId == planId) this else (filterNot { it.from == from } + PlanPeriod(planId, from)).merged()

/** Cancels the start of [planId] on [from], when it is after [today]. */
internal fun List<PlanPeriod>.withoutStart(planId: String, from: LocalDate, today: LocalDate): List<PlanPeriod> =
    if (from <= today) this else filterNot { it.planId == planId && it.from == from }.merged()

/**
 * The indexes of the periods of [planId] begun before [today] where something was [noted] before
 * today (from a day, until another one excluded): changing the plan must not change them.
 */
internal fun List<PlanPeriod>.livedPeriods(
    planId: String,
    today: LocalDate,
    noted: (from: LocalDate, until: LocalDate) -> Boolean,
): Set<Int> = indices.filterTo(HashSet()) { index ->
    val period = this[index]
    if (period.planId != planId || period.from >= today) return@filterTo false
    val end = minOf(getOrNull(index + 1)?.from ?: today, today)
    noted(period.from, end)
}

/**
 * The [lived] periods followed with [keptId], the copy of the plan as it was, and the plan itself
 * followed again from [today] where they went on until then.
 */
internal fun List<PlanPeriod>.keptAs(lived: Set<Int>, planId: String, keptId: String, today: LocalDate): List<PlanPeriod> =
    buildList {
        this@keptAs.forEachIndexed { index, period ->
            if (index in lived) {
                add(period.copy(planId = keptId))
                val next = this@keptAs.getOrNull(index + 1)?.from
                if (next == null || next > today) add(PlanPeriod(planId, today))
            } else {
                add(period)
            }
        }
    }.sortedBy { it.from }
