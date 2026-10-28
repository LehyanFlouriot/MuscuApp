package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import com.lehyanflouriot.muscuapp.core.model.Program
import java.time.LocalDate

enum class ProgramStatus { ACTIVE, PLANNED, PAST, UNUSED }

data class ProgramInfo<P : Program>(
    val plan: P,
    val status: ProgramStatus,
    /** Since when it is followed when active, when it starts when planned, when it ended when past. */
    val date: LocalDate?,
)

/** The period followed on [date], null before the first one started. */
fun List<PlanPeriod>.periodAt(date: LocalDate): PlanPeriod? = lastOrNull { it.from <= date }

/**
 * Where each of [plans] stands on [today], without the copies kept for the past: the current one
 * first, then the planned ones, then the others.
 */
fun <P : Program> programInfos(plans: List<P>, schedule: List<PlanPeriod>, today: LocalDate): List<ProgramInfo<P>> {
    val byId = plans.associateBy { it.id }
    fun programOf(planId: String): P? = byId[planId]?.let { plan -> plan.versionOf?.let(byId::get) ?: plan }
    val current = schedule.periodAt(today)
    val currentProgram = current?.let { programOf(it.planId) }
    return plans.filter { it.versionOf == null }.map { program ->
        fun followed(period: PlanPeriod) = programOf(period.planId)?.id == program.id
        when {
            currentProgram?.id == program.id -> {
                // Since the first day of the uninterrupted run of periods of this program.
                val index = schedule.indexOf(current)
                var first = index
                while (first > 0 && followed(schedule[first - 1])) first--
                ProgramInfo(program, ProgramStatus.ACTIVE, schedule[first].from)
            }
            schedule.any { it.from > today && followed(it) } ->
                ProgramInfo(program, ProgramStatus.PLANNED, schedule.filter { it.from > today && followed(it) }.minOf { it.from })
            schedule.any { it.from <= today && followed(it) } -> {
                val last = schedule.indexOfLast { it.from <= today && followed(it) }
                val end = schedule.getOrNull(last + 1)?.from?.minusDays(1)
                ProgramInfo(program, ProgramStatus.PAST, end)
            }
            else -> ProgramInfo(program, ProgramStatus.UNUSED, null)
        }
    }.sortedBy { it.status.ordinal }
}
