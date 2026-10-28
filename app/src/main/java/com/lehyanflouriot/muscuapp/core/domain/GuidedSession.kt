package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.model.WorkSet

/** A set done on the day of a session, with the entry it is noted in and its place there. */
data class DoneSet(val entryId: String, val index: Int, val set: WorkSet)

/** The sets of the exercise done on the day of the session, in the order they were done. */
fun SessionItem.doneSets(): List<DoneSet> =
    entries.flatMap { entry -> entry.sets.mapIndexed { index, set -> DoneSet(entry.id, index, set) } }

/** How many sets the session asks for the exercise. */
val SessionItem.plannedSets: Int get() = line.target.sets

/** Whether all the sets the session asks were done. */
val SessionItem.complete: Boolean get() = doneSets().size >= plannedSets

/**
 * What to aim for on the next set: the goal of that set, with the load of the last set done, so
 * that a load changed on the first set goes on for the next ones.
 */
fun SessionItem.nextSetGoal(): WorkSet {
    val done = doneSets()
    val setGoal = goal.getOrNull(done.size) ?: goal.lastOrNull() ?: WorkSet(line.target.min)
    return setGoal.copy(weight = done.lastOrNull()?.set?.weight ?: setGoal.weight)
}

/** The exercise to do now: the first one with sets still to do, or null when the session is complete. */
fun SessionView.currentIndex(): Int? = items.indexOfFirst { !it.complete }.takeIf { it >= 0 }

/** Whether every set the session asks was done. */
val SessionView.complete: Boolean get() = items.isNotEmpty() && items.all { it.complete }

/** All the sets done in the session, to sum it up. */
fun SessionView.doneSets(): List<WorkSet> = items.flatMap { item -> item.doneSets().map { it.set } }
