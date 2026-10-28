package com.lehyanflouriot.muscuapp.ui

import android.app.Application
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import com.github.takahirom.roborazzi.captureRoboImage
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.data.ExerciseCatalog
import com.lehyanflouriot.muscuapp.core.data.HypertrophyProgram
import com.lehyanflouriot.muscuapp.core.domain.ChartMetric
import com.lehyanflouriot.muscuapp.core.domain.DEFAULT_BODY_WEIGHT
import com.lehyanflouriot.muscuapp.core.domain.WeightPeriod
import com.lehyanflouriot.muscuapp.core.domain.buildProgressItems
import com.lehyanflouriot.muscuapp.core.domain.buildShoppingList
import com.lehyanflouriot.muscuapp.core.domain.buildWeekOverview
import com.lehyanflouriot.muscuapp.core.domain.buildWeekSessions
import com.lehyanflouriot.muscuapp.core.domain.history
import com.lehyanflouriot.muscuapp.core.domain.lastEntry
import com.lehyanflouriot.muscuapp.core.domain.metricValue
import com.lehyanflouriot.muscuapp.core.domain.programInfos
import com.lehyanflouriot.muscuapp.core.domain.records
import com.lehyanflouriot.muscuapp.core.domain.startOfWeek
import com.lehyanflouriot.muscuapp.core.domain.suggestedWeight
import com.lehyanflouriot.muscuapp.core.domain.weekStreak
import com.lehyanflouriot.muscuapp.core.domain.weeklySeries
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.PlanPeriod
import com.lehyanflouriot.muscuapp.core.model.WorkSet
import com.lehyanflouriot.muscuapp.core.sync.RemoteDocument
import com.lehyanflouriot.muscuapp.core.sync.SyncProblem
import com.lehyanflouriot.muscuapp.core.sync.SyncStatus
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import com.lehyanflouriot.muscuapp.ui.common.ProgramsSheet
import com.lehyanflouriot.muscuapp.ui.common.programStatusText
import com.lehyanflouriot.muscuapp.ui.exercise.ExerciseDetailScreen
import com.lehyanflouriot.muscuapp.ui.exercise.ExerciseEditorForm
import com.lehyanflouriot.muscuapp.ui.exercise.ExerciseFormState
import com.lehyanflouriot.muscuapp.ui.log.LogEntryForm
import com.lehyanflouriot.muscuapp.ui.log.LogFormState
import com.lehyanflouriot.muscuapp.ui.nutrition.NutritionActions
import com.lehyanflouriot.muscuapp.ui.nutrition.NutritionScreen
import com.lehyanflouriot.muscuapp.ui.nutrition.PlanEditorScreen
import com.lehyanflouriot.muscuapp.ui.nutrition.ShoppingActions
import com.lehyanflouriot.muscuapp.ui.nutrition.ShoppingScreen
import com.lehyanflouriot.muscuapp.ui.progress.ProgressScreen
import com.lehyanflouriot.muscuapp.ui.rest.RestScreen
import com.lehyanflouriot.muscuapp.ui.settings.FirstSyncDialog
import com.lehyanflouriot.muscuapp.ui.settings.SettingsScreen
import com.lehyanflouriot.muscuapp.ui.settings.SyncActions
import com.lehyanflouriot.muscuapp.ui.theme.MuscuTheme
import com.lehyanflouriot.muscuapp.ui.week.ExercisePickerSheet
import com.lehyanflouriot.muscuapp.ui.week.SessionActions
import com.lehyanflouriot.muscuapp.ui.week.SessionLineDialog
import com.lehyanflouriot.muscuapp.ui.week.TrainingEditorScreen
import com.lehyanflouriot.muscuapp.ui.week.WeekScreen
import com.lehyanflouriot.muscuapp.ui.weight.WeighInState
import com.lehyanflouriot.muscuapp.ui.weight.WeightScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Renders the main screens with sample data into app/build/outputs/roborazzi, so that the
 * design can be reviewed without a device (the CI publishes these images).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi", application = Application::class)
class ScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    // A Wednesday.
    private val today = LocalDate.of(2026, 10, 28)
    private val data = SampleData.build(today)
    private val now = 1_000_000L

    private fun capture(
        name: String,
        darkTheme: Boolean = false,
        dialog: Boolean = false,
        before: () -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            MuscuTheme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        before()
        // Let the entrance animations finish: the images show the screens at rest.
        composeRule.waitForIdle()
        // A dialog has a window of its own, shown after the one of the screen.
        val node = if (dialog) composeRule.onAllNodes(isRoot()).onLast() else composeRule.onRoot()
        node.captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Composable
    private fun Week(appData: AppData, timerState: TimerState = TimerState.Idle) {
        MuscuScaffold(
            currentRoute = Routes.WEEK,
            timerState = timerState,
            now = now,
            onNavigate = {},
            onTimerToggle = {},
            onTimerAdd = {},
            onTimerRestart = {},
            onTimerStop = {},
        ) { padding ->
            Surface(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            ) {
                WeekScreen(
                    overview = buildWeekOverview(appData, today),
                    sessions = buildWeekSessions(appData, today),
                    sessionDate = today,
                    sessionActions = SessionActions(onSelectDay = {}, onLog = {}, onEditEntry = {}, onOpenProgram = {}),
                    today = today,
                    hasAnyEntry = appData.entries.isNotEmpty(),
                    suggestions = ExerciseCatalog.popular,
                    onPreviousWeek = {},
                    onNextWeek = {},
                    onThisWeek = {},
                    onLog = {},
                    onEditEntry = {},
                    onOpenExercise = {},
                    onAddExercise = {},
                    onQuickAdd = {},
                    onOpenSettings = {},
                )
            }
        }
    }

    @Composable
    private fun Weight(appData: AppData) {
        MuscuScaffold(
            currentRoute = Routes.WEIGHT,
            timerState = TimerState.Idle,
            now = now,
            onNavigate = {},
            onTimerToggle = {},
            onTimerAdd = {},
            onTimerRestart = {},
            onTimerStop = {},
        ) { padding ->
            Surface(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            ) {
                val weights = appData.bodyWeights
                val form = remember { WeighInState(suggestedWeight(weights, today) ?: DEFAULT_BODY_WEIGHT) }
                WeightScreen(
                    weights = weights,
                    today = today,
                    form = form,
                    period = WeightPeriod.THREE_MONTHS,
                    onDateChange = {},
                    onSave = {},
                    onPeriodChange = {},
                    onUpdate = { _, _ -> },
                    onDelete = {},
                )
            }
        }
    }

    private val nutrition = SampleData.nutrition(today)

    @Composable
    private fun Nutrition(date: LocalDate) {
        MuscuScaffold(
            currentRoute = Routes.NUTRITION,
            timerState = TimerState.Idle,
            now = now,
            onNavigate = {},
            onTimerToggle = {},
            onTimerAdd = {},
            onTimerRestart = {},
            onTimerStop = {},
        ) { padding ->
            Surface(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            ) {
                NutritionScreen(
                    nutrition = nutrition,
                    weights = SampleData.bulkWeights(today),
                    today = today,
                    date = date,
                    actions = NutritionActions(
                        onSelectDate = {},
                        onToggleFood = { _, _ -> },
                        onToggleMeal = { _, _ -> },
                        onWaterChange = { _, _ -> },
                        onSwap = { _, _ -> },
                        onEditPlan = { _, _ -> },
                        onOpenPrograms = {},
                        onCreateProgram = {},
                        onOpenWeight = {},
                        onOpenShopping = {},
                    ),
                )
            }
        }
    }

    @Test
    fun nutrition() = capture("9-nutrition") { Nutrition(today) }

    @Test
    fun nutritionMeals() = capture(
        "9-nutrition-repas",
        before = { composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(2) },
    ) { Nutrition(today) }

    @Test
    fun nutritionPerfectDay() = capture("9-nutrition-journee-parfaite", darkTheme = true) { Nutrition(today.minusDays(2)) }

    @Test
    fun nutritionTomorrow() = capture(
        "9-nutrition-demain",
        before = { composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(2) },
    ) { Nutrition(today.plusDays(1)) }

    @Composable
    private fun Shopping() {
        val weekStart = today.startOfWeek()
        ShoppingScreen(
            list = buildShoppingList(nutrition, weekStart, weekStart.plusDays(6)),
            week = nutrition.shoppingWeek(weekStart),
            today = today,
            fromToday = false,
            actions = ShoppingActions(
                onBack = {},
                onWeekChange = {},
                onFromTodayChange = {},
                onToggle = {},
                onToggleExtra = {},
                onRemoveExtra = {},
                onAddExtra = {},
                onClear = {},
                onShare = {},
            ),
        )
    }

    @Test
    fun shopping() = capture("11-courses") { Shopping() }

    @Test
    fun shoppingAisles() = capture(
        "11-courses-rayons",
        darkTheme = true,
        before = { composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(24) },
    ) { Shopping() }

    @Test
    fun planEditor() = capture("10-programme") {
        PlanEditorScreen(
            plan = nutrition.plans.single(),
            day = DayOfWeek.WEDNESDAY,
            keepsHistory = true,
            onDayChange = {},
            onOpen = {},
            onTrainingChange = {},
            onWaterChange = {},
            onBack = {},
        )
    }

    @Test
    fun planEditorMeals() = capture(
        "10-programme-repas",
        before = { composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(2) },
    ) {
        PlanEditorScreen(
            plan = nutrition.plans.single(),
            day = DayOfWeek.WEDNESDAY,
            keepsHistory = true,
            onDayChange = {},
            onOpen = {},
            onTrainingChange = {},
            onWaterChange = {},
            onBack = {},
        )
    }

    @Test
    fun planEditorSettings() = capture(
        "10-programme-reglages",
        darkTheme = true,
        before = { composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(8) },
    ) {
        PlanEditorScreen(
            plan = nutrition.plans.single(),
            day = DayOfWeek.WEDNESDAY,
            keepsHistory = true,
            onDayChange = {},
            onOpen = {},
            onTrainingChange = {},
            onWaterChange = {},
            onBack = {},
        )
    }

    @Test
    fun welcome() = capture("0-bienvenue") { Week(AppData.initial()) }

    @Test
    fun week() = capture("1-semaine") {
        Week(SampleData.withProgram(today), TimerState.Running(endAt = now + 72_000, total = 90_000, label = "Squat bulgare"))
    }

    @Test
    fun weekDark() = capture("1-semaine-sombre", darkTheme = true) { Week(SampleData.withProgram(today)) }

    @Test
    fun weekExercises() = capture(
        "1-semaine-exercices",
        before = { composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(3) },
    ) { Week(SampleData.withProgram(today)) }

    @Composable
    private fun TrainingEditor(appData: AppData, initialDay: DayOfWeek = DayOfWeek.MONDAY) {
        TrainingEditorScreen(
            program = appData.training.programs.first(),
            exercises = appData.exercises.associateBy { it.id },
            status = programStatusText(appData.training.programInfos(today).first(), today),
            keepsHistory = true,
            today = today.dayOfWeek,
            initialDay = initialDay,
            onOpen = {},
            onOpenPrograms = {},
            onBack = {},
        )
    }

    /** The program of the app, a stronger one starting next week, and a week of deload never followed. */
    private fun withPrograms(): AppData {
        val appData = SampleData.withProgram(today)
        val program = appData.training.programs.single()
        return appData.copy(
            training = appData.training.copy(
                programs = listOf(
                    program,
                    program.copy(id = "force", name = "Force, cycle 2"),
                    program.copy(id = "decharge", name = "Semaine de décharge"),
                ),
                schedule = appData.training.schedule + PlanPeriod("force", today.startOfWeek().plusWeeks(1)),
            ),
        )
    }

    @Test
    fun program() = capture("12-programme") { TrainingEditor(SampleData.withProgram(today)) }

    @Test
    fun programToday() = capture("12-programme-mercredi", darkTheme = true) {
        TrainingEditor(SampleData.withProgram(today), initialDay = DayOfWeek.WEDNESDAY)
    }

    @Test
    fun programTips() = capture(
        "12-programme-conseils",
        before = { composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(8) },
    ) { TrainingEditor(SampleData.withProgram(today)) }

    @Test
    fun programLine() = capture("13-programme-exercice", dialog = true) {
        val appData = SampleData.withProgram(today)
        val line = appData.training.programs.single().day(DayOfWeek.WEDNESDAY).exercises.first()
        SessionLineDialog(
            exercise = appData.exercise(line.exerciseId)!!,
            dayName = "mercredi",
            line = line,
            initialTarget = line.target,
            onDismiss = {},
            onSave = { _, _, _, _ -> },
            onMove = {},
            onReplace = {},
            onRemove = {},
        )
    }

    @Test
    fun programPicker() = capture("13-programme-ajout", dialog = true) {
        val appData = SampleData.withProgram(today)
        ExercisePickerSheet(
            title = stringResource(R.string.training_add_exercise),
            exercises = appData.exercises,
            inSession = appData.training.programs.single().day(DayOfWeek.WEDNESDAY).exercises.map { it.exerciseId }.toSet(),
            onDismiss = {},
            onPick = {},
            onPickSuggestion = {},
            onCreate = {},
        )
    }

    @Test
    fun programs() = capture("13-programmes", dialog = true) {
        ProgramsSheet(
            infos = withPrograms().training.programInfos(today),
            today = today,
            onDismiss = {},
            onEdit = {},
            onStart = {},
            onCancelStart = {},
            onDelete = {},
            onNew = {},
        )
    }

    @Test
    fun weekBeforeTheProgram() = capture("1-semaine-avant-programme") {
        val appData = SampleData.withProgram(today)
        val training = appData.training.copy(schedule = listOf(PlanPeriod(HypertrophyProgram.ID, today.plusDays(1))))
        Week(appData.copy(entries = emptyList(), training = training))
    }

    @Test
    fun logFromSession() = capture("2-saisie-seance") {
        val state = remember { LogFormState(today, listOf(WorkSet(12), WorkSet(12), WorkSet(11), WorkSet(11)), "", Measure.REPS) }
        val appData = SampleData.withProgram(today)
        LogEntryForm(
            exercise = appData.exercise("ex-squat-bulgare")!!,
            state = state,
            today = today,
            isEdit = false,
            previous = appData.entries.first { it.exerciseId == "ex-squat-bulgare" },
            weightStep = 2.5,
            onSave = {},
            sessionTarget = "4 × 8–15 / jambe · RIR 1 · Excentrique 3 s, amplitude complète",
            restButtonText = "Repos 3:00",
            onRestClick = {},
        )
    }

    @Test
    fun logEntry() = capture("2-saisie") {
        val state = remember { LogFormState(today, List(4) { WorkSet(10, 77.5) }, "", Measure.REPS) }
        LogEntryForm(
            exercise = SampleData.bench,
            state = state,
            today = today,
            isEdit = false,
            previous = lastEntry(data.entriesOf(SampleData.bench.id)),
            weightStep = 2.5,
            onSave = {},
            restButtonText = "Repos 2:00",
            onRestClick = {},
        )
    }

    @Test
    fun logEntryDetailed() = capture("2-saisie-detaillee", darkTheme = true) {
        val sets = listOf(WorkSet(10, 77.5), WorkSet(9, 77.5), WorkSet(8, 80.0))
        val state = remember { LogFormState(today, sets, "Dernière série difficile", Measure.REPS) }
        LogEntryForm(
            exercise = SampleData.bench,
            state = state,
            today = today,
            isEdit = true,
            previous = lastEntry(data.entriesOf(SampleData.bench.id)),
            weightStep = 2.5,
            onSave = {},
            restButtonText = "1:12",
            onRestClick = {},
            onDelete = {},
        )
    }

    @Test
    fun restRunning() = capture("3-repos") {
        RestScreen(
            state = TimerState.Running(endAt = now + 72_000, total = 90_000, label = "Développé couché"),
            now = now,
            idleDurationMs = 90_000,
            presets = data.restPresets,
            notificationsEnabled = true,
            onToggle = {},
            onAdjust = {},
            onStop = {},
            onRestart = {},
            onStartPreset = {},
            onAddPreset = {},
            onEditPreset = {},
            onEnableNotifications = {},
            onEditIdleDuration = {},
        )
    }

    @Test
    fun restFinished() = capture("3-repos-termine", darkTheme = true) {
        RestScreen(
            state = TimerState.Finished(finishedAt = now - 23_000, total = 120_000, label = "Squat"),
            now = now,
            idleDurationMs = 90_000,
            presets = data.restPresets,
            notificationsEnabled = false,
            onToggle = {},
            onAdjust = {},
            onStop = {},
            onRestart = {},
            onStartPreset = {},
            onAddPreset = {},
            onEditPreset = {},
            onEnableNotifications = {},
            onEditIdleDuration = {},
        )
    }

    @Test
    fun progress() = capture("4-progres") {
        ProgressScreen(
            items = buildProgressItems(data, today),
            activity = weeklySeries(data.entries, today, 12) { metricValue(it, ChartMetric.SETS) },
            streak = weekStreak(data.entries, today),
            trainedThisWeek = true,
            today = today,
            onOpenExercise = {},
        )
    }

    @Test
    fun exerciseDetail() = capture("5-exercice") {
        val exercise = SampleData.bench
        val entries = data.entriesOf(exercise.id)
        ExerciseDetailScreen(
            exercise = exercise,
            weeks = history(entries),
            records = records(exercise, entries),
            metric = ChartMetric.VOLUME,
            series = weeklySeries(entries, today, 12) { metricValue(it, ChartMetric.VOLUME) },
            today = today,
            restLabel = "Repos 2:00",
            onMetricChange = {},
            onBack = {},
            onEdit = {},
            onToggleArchive = {},
            onDelete = {},
            onLog = {},
            onStartRest = {},
            onEditEntry = {},
        )
    }

    @Test
    fun exerciseEditor() = capture("6-nouvel-exercice") {
        val state = remember { ExerciseFormState(null).apply { name = "Dév" } }
        ExerciseEditorForm(
            state = state,
            isNew = true,
            existingNames = data.exercises.map { it.name },
            restPresets = data.restPresets,
            onCancel = {},
            onSave = {},
        )
    }

    @Test
    fun weight() = capture("8-poids") { Weight(data) }

    @Test
    fun weightChart() = capture(
        "8-poids-courbe",
        darkTheme = true,
        before = { composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(2) },
    ) { Weight(data) }

    @Test
    fun weightFirstTime() = capture("8-poids-vide") { Weight(AppData.initial()) }

    @Composable
    private fun Settings(sync: SyncStatus, connecting: Boolean = false) {
        SettingsScreen(
            settings = data.settings,
            exerciseCount = data.exercises.size,
            entryCount = data.entries.size,
            versionName = "1.0.0",
            dynamicColorAvailable = true,
            sync = sync,
            syncConnecting = connecting,
            today = today,
            syncActions = SyncActions(onConnect = {}, onReconnect = {}, onSyncNow = {}, onDisconnect = {}, onChoose = {}),
            onBack = {},
            onWeightStepChange = {},
            onThemeChange = {},
            onDynamicColorChange = {},
            onOpenNotifications = {},
            onOpenAlertSettings = {},
            onExport = {},
            onImport = {},
        )
    }

    /** 18:42 on the day of the screenshots. */
    private val syncedAt = today.atTime(18, 42).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun settings() = capture("7-reglages") { Settings(SyncStatus()) }

    @Test
    fun settingsSynced() = capture("7-reglages-synchro") {
        Settings(SyncStatus(account = "lehyan@example.com", lastSync = syncedAt))
    }

    @Test
    fun settingsSyncProblem() = capture("7-reglages-synchro-erreur", darkTheme = true) {
        Settings(
            SyncStatus(
                account = "lehyan@example.com",
                lastSync = syncedAt,
                problem = SyncProblem.SETUP,
                detail = "Google Drive API has not been used in project 1234 before or it is disabled.",
            ),
        )
    }

    @Test
    fun firstSyncChoice() = capture("7-reglages-synchro-choix", dialog = true) {
        FirstSyncDialog(
            document = RemoteDocument("doc", "r", device = "Pixel 8", modifiedAt = syncedAt - 86_400_000L),
            today = today,
            onDismiss = {},
            onConfirm = {},
        )
    }
}
