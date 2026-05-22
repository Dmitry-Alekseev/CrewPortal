package com.example.crewportal.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.crewportal.data.remote.WeatherRepository
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.ui.calendar.CalendarScreen
import com.example.crewportal.ui.fleet.FleetScreen
import com.example.crewportal.ui.logbook.LogbookScreen
import com.example.crewportal.ui.notifications.NotificationsScreen
import com.example.crewportal.ui.profile.ProfileScreen
import com.example.crewportal.ui.schedule.FlightDetailsScreen
import com.example.crewportal.ui.schedule.ScheduleScreen
import com.example.crewportal.ui.settings.SettingsScreen
import com.example.crewportal.ui.weather.WeatherScreen

sealed class Screen(val route: String, val label: String) {
    data object Schedule : Screen("schedule", "Schedule")
    data object Calendar : Screen("calendar", "Calendar")
    data object Weather : Screen("weather", "Weather")
    data object Fleet : Screen("fleet", "Fleet")
    data object Logbook : Screen("logbook", "Log")
    data object Alerts : Screen("alerts", "Alerts")
    data object Profile : Screen("profile", "Profile")
    data object Settings : Screen("settings", "Settings")
}

@Composable
fun MainNavigation(
    flightRepository: FlightRepository,
    preferencesRepository: PreferencesRepository,
    weatherRepository: WeatherRepository,
    onLogout: suspend () -> Unit
) {
    val navController = rememberNavController()
    val bottomItems = listOf(Screen.Schedule, Screen.Calendar, Screen.Weather, Screen.Fleet, Screen.Logbook, Screen.Alerts, Screen.Profile, Screen.Settings)

    Scaffold(bottomBar = { BottomBar(navController, bottomItems) }) { padding ->
        NavHost(navController = navController, startDestination = Screen.Schedule.route, modifier = Modifier.padding(padding)) {
            composable(Screen.Schedule.route) {
                ScheduleScreen(flightRepository = flightRepository, onFlightClick = { navController.navigate("details/$it") })
            }
            composable("details/{flightId}") { entry ->
                FlightDetailsScreen(
                    flightId = entry.arguments?.getString("flightId").orEmpty(),
                    flightRepository = flightRepository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Calendar.route) { CalendarScreen(flightRepository) }
            composable(Screen.Weather.route) { WeatherScreen(weatherRepository) }
            composable(Screen.Fleet.route) { FleetScreen() }
            composable(Screen.Alerts.route) { NotificationsScreen(flightRepository) }
            composable(Screen.Logbook.route) { LogbookScreen(flightRepository) }
            composable(Screen.Profile.route) { ProfileScreen(preferencesRepository) }
            composable(Screen.Settings.route) { SettingsScreen(flightRepository = flightRepository, onLogout = onLogout) }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, items: List<Screen>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    NavigationBar {
        items.forEach { screen ->
            val icon = when (screen) {
                Screen.Schedule -> Icons.Default.Flight
                Screen.Calendar -> Icons.Default.DateRange
                Screen.Weather -> Icons.Default.Cloud
                Screen.Fleet -> Icons.Default.AirplanemodeActive
                Screen.Logbook -> Icons.Default.WorkHistory
                Screen.Alerts -> Icons.Default.Notifications
                Screen.Profile -> Icons.Default.Person
                Screen.Settings -> Icons.Default.Settings
            }
            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = screen.label) },
                label = { Text(screen.label) }
            )
        }
    }
}
