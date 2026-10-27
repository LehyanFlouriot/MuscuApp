package com.lehyanflouriot.muscuapp.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lehyanflouriot.muscuapp.AppContainer
import com.lehyanflouriot.muscuapp.R
import com.lehyanflouriot.muscuapp.core.model.AppData
import com.lehyanflouriot.muscuapp.core.timer.TimerState
import com.lehyanflouriot.muscuapp.ui.common.LocalMessenger
import com.lehyanflouriot.muscuapp.ui.common.LocalSnackbarHostState
import com.lehyanflouriot.muscuapp.ui.common.Motion
import com.lehyanflouriot.muscuapp.ui.common.SnackbarMessenger
import com.lehyanflouriot.muscuapp.ui.exercise.ExerciseDetailRoute
import com.lehyanflouriot.muscuapp.ui.nutrition.NutritionRoute
import com.lehyanflouriot.muscuapp.ui.nutrition.PlanEditorRoute
import com.lehyanflouriot.muscuapp.ui.nutrition.ShoppingRoute
import com.lehyanflouriot.muscuapp.ui.progress.ProgressRoute
import com.lehyanflouriot.muscuapp.ui.rest.ADJUST_STEP_MS
import com.lehyanflouriot.muscuapp.ui.rest.MiniTimerBar
import com.lehyanflouriot.muscuapp.ui.rest.RestRoute
import com.lehyanflouriot.muscuapp.ui.rest.displayed
import com.lehyanflouriot.muscuapp.ui.rest.rememberTimerNow
import com.lehyanflouriot.muscuapp.ui.settings.SettingsRoute
import com.lehyanflouriot.muscuapp.ui.week.WeekRoute
import com.lehyanflouriot.muscuapp.ui.weight.WeightRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import java.time.DayOfWeek
import java.time.LocalDate

object Routes {
    const val WEEK = "week"
    const val NUTRITION = "nutrition"
    const val PROGRESS = "progress"
    const val WEIGHT = "weight"
    const val REST = "rest"
    const val SETTINGS = "settings"
    const val EXERCISE = "exercise/{id}"
    const val PLAN = "plan/{id}?day={day}"
    const val SHOPPING = "shopping?week={week}"

    fun exercise(id: String) = "exercise/$id"

    fun plan(id: String, day: DayOfWeek) = "plan/$id?day=${day.value}"

    /** The shopping list of the week starting [weekStart], or of the week to shop for. */
    fun shopping(weekStart: LocalDate?) = "shopping?week=${weekStart?.toEpochDay() ?: -1}"
}

private class Tab(val route: String, @param:StringRes val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.WEEK, R.string.tab_week, Icons.Rounded.FitnessCenter),
    Tab(Routes.NUTRITION, R.string.tab_nutrition, Icons.Rounded.Restaurant),
    Tab(Routes.PROGRESS, R.string.tab_progress, Icons.Rounded.Insights),
    Tab(Routes.WEIGHT, R.string.tab_weight, Icons.Rounded.MonitorWeight),
    Tab(Routes.REST, R.string.tab_rest, Icons.Rounded.Timer),
)

private val tabRoutes: Set<String> = tabs.mapTo(HashSet()) { it.route }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes

// Another tab: the current screen fades out quickly and the new one fades in while settling.
private val tabEnter: EnterTransition =
    fadeIn(tween(240, delayMillis = 70)) + scaleIn(tween(360, delayMillis = 70, easing = Motion.EaseOut), initialScale = 0.96f)
private val tabExit: ExitTransition = fadeOut(tween(90))

// A detail screen slides over the current one, which moves back a little; going back undoes it.
private val pushEnter: EnterTransition = slideInHorizontally(tween(Motion.SCREEN_MS, easing = Motion.EaseOut)) { it }
private val pushExit: ExitTransition = slideOutHorizontally(tween(Motion.SCREEN_MS, easing = Motion.EaseOut)) { -it / 4 }
private val popEnter: EnterTransition = slideInHorizontally(tween(Motion.SCREEN_MS, easing = Motion.EaseOut)) { -it / 4 }
private val popExit: ExitTransition = slideOutHorizontally(tween(Motion.SCREEN_MS, easing = Motion.EaseOut)) { it }

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun MuscuApp(
    data: AppData,
    container: AppContainer,
    openRestRequests: StateFlow<Int>,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val repository = container.repository
    val timer = container.restTimer

    val snackbarHostState = remember { SnackbarHostState() }
    val appScope = rememberCoroutineScope()
    val messenger = remember(appScope, snackbarHostState) { SnackbarMessenger(appScope, snackbarHostState) }

    val rawTimerState by timer.state.collectAsStateWithLifecycle()
    val now = rememberTimerNow(rawTimerState, timer::now)
    val timerState = rawTimerState.displayed(now)

    // While the app is open, end the rest exactly on time even if the alarm is a bit late.
    LaunchedEffect(rawTimerState) {
        val running = rawTimerState as? TimerState.Running ?: return@LaunchedEffect
        delay((running.endAt - timer.now()).coerceAtLeast(0))
        timer.finishIfDue()
    }

    val openRest by openRestRequests.collectAsStateWithLifecycle()
    LaunchedEffect(openRest) {
        if (openRest > 0) navController.navigateToTab(Routes.REST)
    }

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState,
        LocalMessenger provides messenger,
    ) {
        MuscuScaffold(
            currentRoute = currentRoute,
            timerState = timerState,
            now = now,
            onNavigate = { navController.navigateToTab(it) },
            onTimerToggle = timer::toggle,
            onTimerAdd = { timer.adjust(ADJUST_STEP_MS) },
            onTimerRestart = timer::restart,
            onTimerStop = timer::stop,
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.WEEK,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
                enterTransition = { if (isTabSwitch()) tabEnter else pushEnter },
                exitTransition = { if (isTabSwitch()) tabExit else pushExit },
                popEnterTransition = { if (isTabSwitch()) tabEnter else popEnter },
                popExitTransition = { if (isTabSwitch()) tabExit else popExit },
            ) {
                composable(Routes.WEEK) {
                    WeekRoute(
                        data = data,
                        repository = repository,
                        timer = timer,
                        onOpenExercise = { navController.navigate(Routes.exercise(it)) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
                composable(Routes.NUTRITION) {
                    NutritionRoute(
                        data = data,
                        repository = repository,
                        onEditPlan = { id, day -> navController.navigate(Routes.plan(id, day)) },
                        onOpenWeight = { navController.navigateToTab(Routes.WEIGHT) },
                        onOpenShopping = { navController.navigate(Routes.shopping(it)) },
                    )
                }
                composable(Routes.PROGRESS) {
                    ProgressRoute(
                        data = data,
                        onOpenExercise = { navController.navigate(Routes.exercise(it)) },
                    )
                }
                composable(Routes.WEIGHT) {
                    WeightRoute(data = data, repository = repository)
                }
                composable(Routes.REST) {
                    RestRoute(presets = data.restPresets, repository = repository, timer = timer)
                }
                composable(
                    route = Routes.EXERCISE,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { entry ->
                    ExerciseDetailRoute(
                        exerciseId = entry.arguments?.getString("id").orEmpty(),
                        data = data,
                        repository = repository,
                        timer = timer,
                        // Popping this precise destination makes repeated calls harmless.
                        onBack = { navController.popBackStack(entry.destination.id, inclusive = true) },
                    )
                }
                composable(
                    route = Routes.PLAN,
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType },
                        navArgument("day") {
                            type = NavType.IntType
                            defaultValue = DayOfWeek.MONDAY.value
                        },
                    ),
                ) { entry ->
                    PlanEditorRoute(
                        planId = entry.arguments?.getString("id").orEmpty(),
                        initialDay = DayOfWeek.of((entry.arguments?.getInt("day") ?: 1).coerceIn(1, 7)),
                        data = data,
                        repository = repository,
                        onBack = { navController.popBackStack(entry.destination.id, inclusive = true) },
                    )
                }
                composable(
                    route = Routes.SHOPPING,
                    arguments = listOf(
                        navArgument("week") {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                    ),
                ) { entry ->
                    ShoppingRoute(
                        initialWeek = entry.arguments?.getLong("week")?.takeIf { it >= 0 }?.let(LocalDate::ofEpochDay),
                        data = data,
                        repository = repository,
                        onBack = { navController.popBackStack(entry.destination.id, inclusive = true) },
                    )
                }
                composable(Routes.SETTINGS) { entry ->
                    SettingsRoute(
                        data = data,
                        repository = repository,
                        sync = container.sync,
                        onBack = { navController.popBackStack(entry.destination.id, inclusive = true) },
                    )
                }
            }
        }
    }
}

/** Frame of the app: the screen, with the rest timer and the tabs at the bottom. */
@Composable
fun MuscuScaffold(
    currentRoute: String?,
    timerState: TimerState,
    now: Long,
    onNavigate: (String) -> Unit,
    onTimerToggle: () -> Unit,
    onTimerAdd: () -> Unit,
    onTimerRestart: () -> Unit,
    onTimerStop: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val showNavigationBar = currentRoute == null || currentRoute in tabRoutes
    val showMiniTimer = timerState !is TimerState.Idle && currentRoute != Routes.REST
    // While the bar slides away under a detail screen, it keeps showing the tab it came from.
    var lastTab by remember { mutableStateOf(Routes.WEEK) }
    val selectedTab = currentRoute?.takeIf { it in tabRoutes } ?: lastTab
    LaunchedEffect(selectedTab) { lastTab = selectedTab }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                AnimatedVisibility(visible = showMiniTimer) {
                    MiniTimerBar(
                        state = timerState,
                        now = now,
                        onOpen = { onNavigate(Routes.REST) },
                        onToggle = onTimerToggle,
                        onAdd = onTimerAdd,
                        onRestart = onTimerRestart,
                        onStop = onTimerStop,
                        applyNavigationBarInsets = !showNavigationBar,
                    )
                }
                AnimatedVisibility(
                    visible = showNavigationBar,
                    enter = expandVertically(tween(Motion.SCREEN_MS, easing = Motion.EaseOut), expandFrom = Alignment.Top),
                    exit = shrinkVertically(tween(Motion.SCREEN_MS, easing = Motion.EaseOut), shrinkTowards = Alignment.Top),
                ) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = selectedTab == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onNavigate(tab.route) },
                                icon = {
                                    TabIcon(
                                        icon = tab.icon,
                                        selected = selected,
                                        badge = tab.route == Routes.REST && timerState is TimerState.Running,
                                    )
                                },
                                label = { Text(stringResource(tab.label)) },
                            )
                        }
                    }
                }
            }
        },
        content = content,
    )
}

/** Icon of a tab, which gives a little bounce when the tab gets selected. */
@Composable
private fun TabIcon(icon: ImageVector, selected: Boolean, badge: Boolean) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            scale.snapTo(0.8f)
            scale.animateTo(1f, Motion.bounce())
        }
    }
    val modifier = Modifier.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
    if (badge) {
        BadgedBox(badge = { Badge() }, modifier = modifier) { Icon(icon, contentDescription = null) }
    } else {
        Icon(icon, contentDescription = null, modifier = modifier)
    }
}
