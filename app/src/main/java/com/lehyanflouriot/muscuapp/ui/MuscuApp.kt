package com.lehyanflouriot.muscuapp.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Insights
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.lehyanflouriot.muscuapp.ui.common.SnackbarMessenger
import com.lehyanflouriot.muscuapp.ui.exercise.ExerciseDetailRoute
import com.lehyanflouriot.muscuapp.ui.progress.ProgressRoute
import com.lehyanflouriot.muscuapp.ui.rest.ADJUST_STEP_MS
import com.lehyanflouriot.muscuapp.ui.rest.MiniTimerBar
import com.lehyanflouriot.muscuapp.ui.rest.RestRoute
import com.lehyanflouriot.muscuapp.ui.rest.displayed
import com.lehyanflouriot.muscuapp.ui.rest.rememberTimerNow
import com.lehyanflouriot.muscuapp.ui.settings.SettingsRoute
import com.lehyanflouriot.muscuapp.ui.week.WeekRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

object Routes {
    const val WEEK = "week"
    const val PROGRESS = "progress"
    const val REST = "rest"
    const val SETTINGS = "settings"
    const val EXERCISE = "exercise/{id}"

    fun exercise(id: String) = "exercise/$id"
}

private class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.WEEK, R.string.tab_week, Icons.Rounded.FitnessCenter),
    Tab(Routes.PROGRESS, R.string.tab_progress, Icons.Rounded.Insights),
    Tab(Routes.REST, R.string.tab_rest, Icons.Rounded.Timer),
)

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
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) },
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
                composable(Routes.PROGRESS) {
                    ProgressRoute(
                        data = data,
                        onOpenExercise = { navController.navigate(Routes.exercise(it)) },
                    )
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
                composable(Routes.SETTINGS) { entry ->
                    SettingsRoute(
                        data = data,
                        repository = repository,
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
    val showNavigationBar = currentRoute == null || tabs.any { it.route == currentRoute }
    val showMiniTimer = timerState !is TimerState.Idle && currentRoute != Routes.REST
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
                if (showNavigationBar) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = { onNavigate(tab.route) },
                                icon = {
                                    if (tab.route == Routes.REST && timerState is TimerState.Running) {
                                        BadgedBox(badge = { Badge() }) { Icon(tab.icon, contentDescription = null) }
                                    } else {
                                        Icon(tab.icon, contentDescription = null)
                                    }
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
