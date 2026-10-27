package com.lehyanflouriot.muscuapp.core.sync

import com.lehyanflouriot.muscuapp.core.data.AppJson
import com.lehyanflouriot.muscuapp.core.data.DefaultMealPlan
import com.lehyanflouriot.muscuapp.core.data.WorkoutRepository
import com.lehyanflouriot.muscuapp.core.model.AppData

/**
 * Merges what changed on this device ([local]) and on the others ([remote]) since [base], the
 * data both had at the last sync. Without [base], everything both sides have is kept.
 */
internal fun mergeData(base: AppData?, local: AppData, remote: AppData): AppData {
    val merged = mergeJson(base?.let(AppJson::tree), AppJson.tree(local), AppJson.tree(remote)) ?: return local
    return WorkoutRepository.sanitize(AppJson.fromTree(merged))
}

/** Whether nothing was noted yet: the data is only what the app comes with. */
internal fun AppData.isPristine(): Boolean =
    exercises.isEmpty() &&
        entries.isEmpty() &&
        bodyWeights.isEmpty() &&
        nutrition.days.isEmpty() &&
        nutrition.shopping.isEmpty() &&
        nutrition.plans.all { it.copy(createdAt = 0L) == DefaultMealPlan.create() }
