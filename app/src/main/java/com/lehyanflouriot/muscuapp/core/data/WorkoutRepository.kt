package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.domain.BODY_WEIGHT_RANGE
import com.lehyanflouriot.muscuapp.core.domain.MAX_WATER_GLASSES
import com.lehyanflouriot.muscuapp.core.domain.hasHistory
import com.lehyanflouriot.muscuapp.core.domain.periodAt
import com.lehyanflouriot.muscuapp.core.domain.planAt
import com.lehyanflouriot.muscuapp.core.domain.roundBodyWeight
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.BodyWeightEntry
import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.FoodItem
import com.lehyanflouriot.muscuapp.core.model.FoodSwap
import com.lehyanflouriot.muscuapp.core.model.MealMoment
import com.lehyanflouriot.muscuapp.core.model.MealPlan
import com.lehyanflouriot.muscuapp.core.model.MealSlot
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.Nutrition
import com.lehyanflouriot.muscuapp.core.model.PlanNote
import com.lehyanflouriot.muscuapp.core.model.RestPreset
import com.lehyanflouriot.muscuapp.core.model.Settings
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.model.WorkoutEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Where the JSON document holding all the user data is kept. */
interface DataStorage {
    /** Returns the saved document, or null when nothing has been saved yet. */
    fun read(): String?

    fun write(content: String)

    /** Keeps a copy of a document that could not be parsed, so it is never silently lost. */
    fun keepUnreadableCopy(content: String)
}

/** Everything needed to create or edit an exercise. */
data class ExerciseDraft(
    val name: String,
    val muscleGroup: MuscleGroup = MuscleGroup.OTHER,
    val usesWeight: Boolean = true,
    val measure: Measure = Measure.REPS,
    val restSeconds: Int? = null,
    val notes: String = "",
)

/** An exercise removed together with its history, kept to allow "undo". */
data class DeletedExercise(val exercise: Exercise, val entries: List<WorkoutEntry>)

/**
 * Single source of truth of the app. Every change is applied in memory first (so the UI
 * updates immediately) and then written to [storage] in the background.
 */
class WorkoutRepository(
    private val storage: DataStorage,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    private val state = MutableStateFlow<AppData?>(null)

    /** Current data, null until [load] has read the saved document. */
    val data: StateFlow<AppData?> = state.asStateFlow()

    private val loadStarted = AtomicBoolean(false)

    fun load() {
        if (!loadStarted.compareAndSet(false, true)) return
        scope.launch(ioDispatcher) {
            val stored = runCatching { storage.read() }.getOrNull()
            val loaded = stored?.let(::decodeOrNull)
            if (stored != null && loaded == null) runCatching { storage.keepUnreadableCopy(stored) }
            val initial = withDefaultPlan(loaded ?: AppData.initial())
            // Data that was just created or completed still has to be written once.
            var lastWritten: AppData? = if (initial === loaded) initial else null
            state.value = initial
            state.filterNotNull().collect { current ->
                if (current !== lastWritten) {
                    // On failure the next change will try again with the whole document.
                    val written = runCatching { storage.write(AppJson.encode(current)) }.isSuccess
                    if (written) lastWritten = current
                }
            }
        }
    }

    private fun decodeOrNull(text: String): AppData? =
        runCatching { sanitize(AppJson.decode(text)) }.getOrNull()

    private fun mutate(transform: (AppData) -> AppData) {
        state.update { current -> current?.let(transform) }
    }

    private fun withDefaultPlan(data: AppData): AppData =
        if (data.nutrition.seeded) data else data.copy(nutrition = data.nutrition.withDefaultPlan(today(), clock()))

    // region Exercises

    fun addExercise(draft: ExerciseDraft): String {
        val id = newId()
        mutate { data ->
            data.copy(
                exercises = data.exercises + Exercise(
                    id = id,
                    name = draft.name.trim(),
                    muscleGroup = draft.muscleGroup,
                    usesWeight = draft.usesWeight,
                    measure = draft.measure,
                    restSeconds = draft.restSeconds,
                    notes = draft.notes.trim(),
                    createdAt = clock(),
                ),
            )
        }
        return id
    }

    fun updateExercise(id: String, draft: ExerciseDraft) {
        mutate { data ->
            data.copy(
                exercises = data.exercises.map {
                    if (it.id != id) {
                        it
                    } else {
                        it.copy(
                            name = draft.name.trim(),
                            muscleGroup = draft.muscleGroup,
                            usesWeight = draft.usesWeight,
                            measure = draft.measure,
                            restSeconds = draft.restSeconds,
                            notes = draft.notes.trim(),
                        )
                    }
                },
            )
        }
    }

    fun setExerciseArchived(id: String, archived: Boolean) {
        mutate { data ->
            data.copy(exercises = data.exercises.map { if (it.id == id) it.copy(archived = archived) else it })
        }
    }

    fun deleteExercise(id: String): DeletedExercise? {
        var deleted: DeletedExercise? = null
        mutate { data ->
            val exercise = data.exercise(id) ?: return@mutate data
            deleted = DeletedExercise(exercise, data.entriesOf(id))
            data.copy(
                exercises = data.exercises.filterNot { it.id == id },
                entries = data.entries.filterNot { it.exerciseId == id },
            )
        }
        return deleted
    }

    fun restoreExercise(deleted: DeletedExercise) {
        mutate { data ->
            if (data.exercise(deleted.exercise.id) != null) {
                data
            } else {
                data.copy(
                    exercises = data.exercises + deleted.exercise,
                    entries = data.entries + deleted.entries,
                )
            }
        }
    }

    // endregion

    // region Entries

    /** Creates an entry when [entryId] is null, updates it otherwise. Returns the entry id. */
    fun saveEntry(
        entryId: String?,
        exerciseId: String,
        date: LocalDate,
        sets: List<WorkSet>,
        note: String = "",
    ): String {
        val cleanSets = cleanSets(sets)
        val id = entryId ?: newId()
        mutate { data ->
            val existing = data.entries.firstOrNull { it.id == id }
            if (existing == null) {
                data.copy(
                    entries = data.entries + WorkoutEntry(
                        id = id,
                        exerciseId = exerciseId,
                        date = date,
                        sets = cleanSets,
                        note = note.trim(),
                        createdAt = clock(),
                    ),
                )
            } else {
                data.copy(
                    entries = data.entries.map {
                        if (it.id == id) it.copy(date = date, sets = cleanSets, note = note.trim()) else it
                    },
                )
            }
        }
        return id
    }

    fun deleteEntry(id: String): WorkoutEntry? {
        var deleted: WorkoutEntry? = null
        mutate { data ->
            deleted = data.entries.firstOrNull { it.id == id }
            if (deleted == null) data else data.copy(entries = data.entries.filterNot { it.id == id })
        }
        return deleted
    }

    fun restoreEntry(entry: WorkoutEntry) {
        mutate { data ->
            if (data.exercise(entry.exerciseId) == null || data.entries.any { it.id == entry.id }) {
                data
            } else {
                data.copy(entries = data.entries + entry)
            }
        }
    }

    // endregion

    // region Body weight

    /** Notes the user's weight for [date], replacing the one already noted that day. */
    fun saveBodyWeight(date: LocalDate, weight: Double) {
        if (weight !in BODY_WEIGHT_RANGE) return
        mutate { data ->
            val existing = data.bodyWeights.firstOrNull { it.date == date }
            val entry = BodyWeightEntry(date = date, weight = roundBodyWeight(weight), createdAt = existing?.createdAt ?: clock())
            data.copy(bodyWeights = sortBodyWeights(data.bodyWeights.filterNot { it.date == date } + entry))
        }
    }

    fun deleteBodyWeight(date: LocalDate): BodyWeightEntry? {
        var deleted: BodyWeightEntry? = null
        mutate { data ->
            deleted = data.bodyWeights.firstOrNull { it.date == date }
            if (deleted == null) data else data.copy(bodyWeights = data.bodyWeights.filterNot { it.date == date })
        }
        return deleted
    }

    fun restoreBodyWeight(entry: BodyWeightEntry) {
        mutate { data ->
            if (data.bodyWeights.any { it.date == entry.date }) {
                data
            } else {
                data.copy(bodyWeights = sortBodyWeights(data.bodyWeights + entry))
            }
        }
    }

    // endregion

    // region Rest presets

    fun addPreset(seconds: Int, label: String = "") {
        val preset = RestPreset(id = newId(), seconds = seconds.coerceIn(MIN_REST, MAX_REST), label = label.trim())
        mutate { data -> data.copy(restPresets = sortPresets(data.restPresets + preset)) }
    }

    fun updatePreset(id: String, seconds: Int, label: String) {
        mutate { data ->
            data.copy(
                restPresets = sortPresets(
                    data.restPresets.map {
                        if (it.id == id) it.copy(seconds = seconds.coerceIn(MIN_REST, MAX_REST), label = label.trim()) else it
                    },
                ),
            )
        }
    }

    fun deletePreset(id: String) {
        mutate { data -> data.copy(restPresets = data.restPresets.filterNot { it.id == id }) }
    }

    // endregion

    // region Nutrition

    private fun mutateNutrition(transform: (Nutrition) -> Nutrition) {
        mutate { data -> data.copy(nutrition = transform(data.nutrition)) }
    }

    /** Marks a food as eaten on [date], or as not eaten any more. */
    fun toggleFood(date: LocalDate, itemId: String) {
        mutateNutrition { nutrition ->
            nutrition.updateDay(date) { day ->
                day.copy(eaten = if (itemId in day.eaten) day.eaten - itemId else day.eaten + itemId)
            }
        }
    }

    /** Marks all the foods of a meal as eaten on [date], or none of them. */
    fun setMealEaten(date: LocalDate, slotId: String, eaten: Boolean) {
        mutateNutrition { nutrition ->
            val ids = nutrition.planAt(date)?.day(date.dayOfWeek)?.meal(slotId)?.items?.map { it.id }
                ?: return@mutateNutrition nutrition
            nutrition.updateDay(date) { day ->
                day.copy(eaten = if (eaten) (day.eaten + ids).distinct() else day.eaten - ids.toSet())
            }
        }
    }

    fun setWater(date: LocalDate, glasses: Int) {
        mutateNutrition { nutrition ->
            nutrition.updateDay(date) { it.copy(water = glasses.coerceIn(0, MAX_WATER_GLASSES)) }
        }
    }

    /** Eats something else than the food [itemId] on [date] only: the plan does not change. */
    fun swapFood(date: LocalDate, itemId: String, name: String, quantity: String) {
        mutateNutrition { nutrition ->
            val item = nutrition.planAt(date)?.item(itemId) ?: return@mutateNutrition nutrition
            val swap = FoodSwap(itemId, name.trim(), quantity.trim())
            // Coming back to what was planned simply removes the swap.
            val changed = swap.name.isNotEmpty() && (swap.name != item.name || swap.quantity != item.quantity)
            nutrition.updateDay(date) { day ->
                day.copy(swaps = day.swaps.filterNot { it.itemId == itemId } + listOfNotNull(swap.takeIf { changed }))
            }
        }
    }

    fun clearSwap(date: LocalDate, itemId: String) {
        mutateNutrition { nutrition ->
            nutrition.updateDay(date) { day -> day.copy(swaps = day.swaps.filterNot { it.itemId == itemId }) }
        }
    }

    /** Changes a program from today on: the days already lived keep what was planned for them. */
    fun updatePlan(planId: String, transform: (MealPlan) -> MealPlan) {
        mutateNutrition { it.changePlan(planId, today(), newId, transform) }
    }

    fun addFood(planId: String, day: DayOfWeek, slotId: String, name: String, quantity: String, note: String = "") {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val item = FoodItem(id = newId(), name = cleanName, quantity = quantity.trim(), note = note.trim())
        updatePlan(planId) { plan ->
            if (plan.slot(slotId) == null) {
                plan
            } else {
                plan.updateDay(day) { it.updateMeal(slotId) { meal -> meal.copy(items = meal.items + item) } }
            }
        }
    }

    fun updateFood(planId: String, itemId: String, name: String, quantity: String, note: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        updatePlan(planId) { plan ->
            plan.updateFood(itemId) { it.copy(name = cleanName, quantity = quantity.trim(), note = note.trim()) }
        }
    }

    fun addSlot(planId: String, name: String, hint: String, moment: MealMoment, kcal: Int?, protein: Int?) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val slot = MealSlot(id = newId(), name = cleanName, hint = hint.trim(), moment = moment, kcal = kcal, protein = protein)
        updatePlan(planId) { it.copy(slots = it.slots + slot) }
    }

    fun addNote(planId: String, title: String, text: String) {
        if (title.isBlank() && text.isBlank()) return
        val note = PlanNote(id = newId(), title = title.trim(), text = text.trim())
        updatePlan(planId) { it.copy(notes = it.notes + note) }
    }

    /** Copies the meals of [from] to the days [to], replacing what they had. */
    fun copyDay(planId: String, from: DayOfWeek, to: Collection<DayOfWeek>) {
        updatePlan(planId) { it.copyDay(from, to, newId) }
    }

    /** Creates a program, empty or copied from [copyOf], followed from [start] when given. Returns its id. */
    fun createProgram(name: String, copyOf: String?, start: LocalDate?): String {
        val id = newId()
        val cleanName = name.trim().ifEmpty { "Programme" }
        mutateNutrition { nutrition ->
            val source = copyOf?.let(nutrition::plan)
            val plan = if (source != null) {
                copiedPlan(source, id, cleanName, clock())
            } else {
                blankPlan(id, cleanName, clock(), newId)
            }
            val added = nutrition.copy(plans = nutrition.plans + plan)
            if (start == null) added else added.startPlan(id, maxOf(start, today()))
        }
        return id
    }

    /** Follows a program from [from] on (today at the earliest), until the next start already planned. */
    fun startProgram(planId: String, from: LocalDate) {
        mutateNutrition { it.startPlan(planId, maxOf(from, today())) }
    }

    fun cancelProgramStart(planId: String, from: LocalDate) {
        mutateNutrition { it.cancelStart(planId, from, today()) }
    }

    /** Deletes a program that is not followed today and was never followed on a day where something was noted. */
    fun deleteProgram(planId: String): Boolean {
        var deleted = false
        mutateNutrition { nutrition ->
            deleted = false
            val plan = nutrition.plan(planId)
            if (plan == null || plan.versionOf != null) return@mutateNutrition nutrition
            if (nutrition.periodAt(today())?.planId == planId || nutrition.hasHistory(planId)) return@mutateNutrition nutrition
            deleted = true
            nutrition.copy(
                plans = nutrition.plans.filterNot { it.id == planId },
                schedule = nutrition.schedule.filterNot { it.planId == planId }.merged(),
            )
        }
        return deleted
    }

    // endregion

    fun updateSettings(transform: (Settings) -> Settings) {
        mutate { data -> data.copy(settings = transform(data.settings)) }
    }

    // region Backup

    /** The current data as a readable JSON document. */
    fun exportBackup(): String = AppJson.encode(state.value ?: AppData.initial(), pretty = true)

    /** Parses a backup document. Throws [IllegalArgumentException] when it is not a MuscuApp backup. */
    fun parseBackup(text: String): AppData {
        val parsed = runCatching { AppJson.decode(text) }.getOrElse {
            throw IllegalArgumentException("Not a MuscuApp backup", it)
        }
        return sanitize(parsed)
    }

    fun replaceAll(data: AppData) {
        state.update { withDefaultPlan(sanitize(data)) }
    }

    // endregion

    companion object {
        const val MIN_REST = 5
        const val MAX_REST = 60 * 60

        private fun cleanSets(sets: List<WorkSet>): List<WorkSet> =
            sets.filter { it.reps > 0 }.map { it.copy(weight = it.weight.coerceAtLeast(0.0)) }

        private fun sortPresets(presets: List<RestPreset>): List<RestPreset> = presets.sortedBy { it.seconds }

        private fun sortBodyWeights(weights: List<BodyWeightEntry>): List<BodyWeightEntry> = weights.sortedBy { it.date }

        /** Drops anything that would break the app (orphan entries, empty sets, duplicates...). */
        internal fun sanitize(data: AppData): AppData {
            val exercises = data.exercises
                .filter { it.name.isNotBlank() }
                .distinctBy { it.id }
            val ids = exercises.mapTo(HashSet()) { it.id }
            val entries = data.entries
                .filter { it.exerciseId in ids }
                .map { it.copy(sets = cleanSets(it.sets)) }
                .filter { it.sets.isNotEmpty() }
                .distinctBy { it.id }
            // One weight per day: the last one noted wins.
            val bodyWeights = data.bodyWeights
                .filter { it.weight in BODY_WEIGHT_RANGE }
                .map { it.copy(weight = roundBodyWeight(it.weight)) }
                .sortedWith(compareBy<BodyWeightEntry> { it.date }.thenByDescending { it.createdAt })
                .distinctBy { it.date }
            val presets = data.restPresets
                .filter { it.seconds in MIN_REST..MAX_REST }
                .distinctBy { it.id }
            val step = data.settings.weightStep
            val settings = if (step > 0.0 && step <= 50.0) data.settings else data.settings.copy(weightStep = 2.5)
            return data.copy(
                schemaVersion = AppData.CURRENT_SCHEMA_VERSION,
                exercises = exercises,
                entries = entries,
                bodyWeights = bodyWeights,
                restPresets = sortPresets(presets),
                settings = settings,
                nutrition = sanitizeNutrition(data.nutrition),
            )
        }
    }
}
