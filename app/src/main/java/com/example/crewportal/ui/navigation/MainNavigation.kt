package com.example.crewportal.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    data object Schedule : Screen("schedule", "Roster")
    data object Calendar : Screen("calendar", "Cal")
    data object Weather : Screen("weather", "WX")
    data object Fleet : Screen("fleet", "Fleet")
    data object Logbook : Screen("logbook", "Log")
    data object More : Screen("more", "More")
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
    val bottomItems = listOf(
        Screen.Schedule,
        Screen.Calendar,
        Screen.Weather,
        Screen.Fleet,
        Screen.Logbook,
        Screen.More
    )

    Scaffold(
        bottomBar = {
            BottomBar(navController = navController, items = bottomItems)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Schedule.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Schedule.route) {
                ScheduleScreen(
                    flightRepository = flightRepository,
                    onFlightClick = { navController.navigate("details/$it") }
                )
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
            composable(Screen.Logbook.route) { LogbookScreen(flightRepository) }
            composable(Screen.Alerts.route) { NotificationsScreen(flightRepository) }
            composable(Screen.Profile.route) { ProfileScreen(preferencesRepository) }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    flightRepository = flightRepository,
                    onLogout = onLogout
                )
            }
            composable(Screen.More.route) {
                MoreScreen(navController = navController)
            }
        }
    }
}

@Composable
private fun BottomBar(
    navController: NavHostController,
    items: List<Screen>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    NavigationBar {
        items.forEach { screen ->
            val icon = when (screen) {
                Screen.Schedule -> Icons.Default.Flight
                Screen.Calendar -> Icons.Default.DateRange
                Screen.Weather -> Icons.Default.Cloud
                Screen.Fleet -> Icons.Default.AirplanemodeActive
                Screen.Logbook -> Icons.Default.WorkHistory
                Screen.More -> Icons.Default.Menu
                Screen.Alerts -> Icons.Default.Notifications
                Screen.Profile -> Icons.Default.Person
                Screen.Settings -> Icons.Default.Settings
            }

            val selected = if (screen == Screen.More) {
                currentRoute in listOf(
                    Screen.More.route,
                    Screen.Alerts.route,
                    Screen.Profile.route,
                    Screen.Settings.route
                )
            } else {
                currentDestination?.hierarchy?.any { it.route == screen.route } == true
            }

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = screen.label
                    )
                },
                label = {
                    Text(
                        text = screen.label,
                        maxLines = 1,
                        softWrap = false,
                        fontSize = 10.sp
                    )
                }
            )
        }
    }
}

@Composable
private fun MoreScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "More",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        MoreMenuCard(
            title = "Alerts",
            subtitle = "Crew notifications and company messages",
            onClick = { navController.navigate(Screen.Alerts.route) }
        )

        MoreMenuCard(
            title = "Profile",
            subtitle = "Pilot profile, flight time and qualifications",
            onClick = { navController.navigate(Screen.Profile.route) }
        )

        MoreMenuCard(
            title = "Settings",
            subtitle = "Synchronization, appearance and app actions",
            onClick = { navController.navigate(Screen.Settings.route) }
        )
    }
}

@Composable
private fun MoreMenuCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open")
            }
        }
    }
}
