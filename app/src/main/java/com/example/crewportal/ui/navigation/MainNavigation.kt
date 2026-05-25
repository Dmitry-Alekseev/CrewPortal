package com.example.crewportal.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
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
import com.example.crewportal.ui.notifications.NotificationsScreen
import com.example.crewportal.ui.profile.ProfileScreen
import com.example.crewportal.ui.schedule.FlightDetailsScreen
import com.example.crewportal.ui.schedule.ScheduleScreen
import com.example.crewportal.ui.settings.SettingsScreen
import com.example.crewportal.ui.mel.MelScreen
import com.example.crewportal.ui.weather.WeatherScreen
import com.example.crewportal.ui.contacts.CompanyContactsScreen
import com.example.crewportal.ui.update.UpdateCenterScreen
import com.example.crewportal.ui.airport.AirportInfoScreen
import com.example.crewportal.ui.history.RosterChangeHistoryScreen
import com.example.crewportal.ui.leave.LeaveManagementScreen

sealed class Screen(val route: String, val label: String) {
    data object Schedule : Screen("schedule", "Roster")
    data object Calendar : Screen("calendar", "Calendar")
    data object Weather : Screen("weather", "Weather")
    data object Fleet : Screen("fleet", "Fleet")
    data object More : Screen("more", "More")
    data object Alerts : Screen("alerts", "Alerts")
    data object Profile : Screen("profile", "Profile")
    data object Settings : Screen("settings", "Settings")
    data object Contacts : Screen("contacts", "Contacts")
    data object UpdateCenter : Screen("update_center", "Update Center")
    data object AirportInfo : Screen("airport_info", "Airport Info")
    data object RosterHistory : Screen("roster_history", "Roster History")
    data object Leave : Screen("leave", "Leave")
    data object Mel : Screen("mel", "MEL")
}

@OptIn(ExperimentalMaterial3Api::class)
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
        Screen.More
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val language by preferencesRepository.appLanguage.collectAsState(initial = "en")
    val ru = language == "ru"
    val showBackButton = currentRoute in listOf(
        Screen.Alerts.route,
        Screen.Profile.route,
        Screen.Contacts.route,
        Screen.UpdateCenter.route,
        Screen.AirportInfo.route,
        Screen.RosterHistory.route,
        Screen.Leave.route,
        Screen.Settings.route,
        "mel/{registration}"
    )

    Scaffold(
        topBar = {
            if (showBackButton) {
                TopAppBar(
                    title = { Text(titleForRoute(currentRoute, ru)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Schedule.route)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            BottomBar(navController = navController, items = bottomItems, ru = ru)
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
                    preferencesRepository = preferencesRepository,
                    onDutyClick = { navController.navigate("details/$it") },
                    onMelClick = { navController.navigate("mel/$it") }
                )
            }

            composable("details/{flightId}") { entry ->
                FlightDetailsScreen(
                    flightId = entry.arguments?.getString("flightId").orEmpty(),
                    flightRepository = flightRepository,
                    onBack = { navController.popBackStack() },
                    onMelClick = { navController.navigate("mel/$it") }
                )
            }

            composable("mel/{registration}") { entry ->
                MelScreen(registration = entry.arguments?.getString("registration").orEmpty())
            }

            composable(Screen.Calendar.route) { CalendarScreen(flightRepository) }
            composable(Screen.Weather.route) { WeatherScreen(weatherRepository) }
            composable(Screen.Fleet.route) { FleetScreen() }
            composable(Screen.Alerts.route) { NotificationsScreen(flightRepository) }
            composable(Screen.Profile.route) { ProfileScreen(preferencesRepository) }
            composable(Screen.Contacts.route) { CompanyContactsScreen() }
            composable(Screen.UpdateCenter.route) { UpdateCenterScreen(flightRepository, preferencesRepository) }
            composable(Screen.AirportInfo.route) { AirportInfoScreen() }
            composable(Screen.RosterHistory.route) { RosterChangeHistoryScreen(flightRepository) }
            composable(Screen.Leave.route) { LeaveManagementScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    flightRepository = flightRepository,
                    preferencesRepository = preferencesRepository,
                    onLogout = onLogout
                )
            }
            composable(Screen.More.route) {
                MoreScreen(navController = navController, ru = ru)
            }
        }
    }
}

private fun titleForRoute(route: String?, ru: Boolean): String {
    return when (route) {
        Screen.Calendar.route -> if (ru) "Календарь" else "Calendar"
        Screen.Weather.route -> if (ru) "Погода" else "Weather"
        Screen.Fleet.route -> if (ru) "Флот" else "Fleet"
        Screen.More.route -> if (ru) "Ещё" else "More"
        Screen.Alerts.route -> if (ru) "Уведомления" else "Alerts"
        Screen.Profile.route -> if (ru) "Профиль" else "Profile"
        Screen.Settings.route -> if (ru) "Настройки" else "Settings"
        Screen.Contacts.route -> if (ru) "Контакты компании" else "Company Contacts"
        Screen.UpdateCenter.route -> if (ru) "Центр обновлений" else "Update Center"
        Screen.AirportInfo.route -> if (ru) "Аэропорты" else "Airport Info"
        Screen.RosterHistory.route -> if (ru) "История ростера" else "Roster History"
        Screen.Leave.route -> if (ru) "Отпуск" else "Leave"
        "mel/{registration}" -> "MEL"
        else -> "Crew Portal"
    }
}

@Composable
private fun BottomBar(
    navController: NavHostController,
    items: List<Screen>,
    ru: Boolean
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
                Screen.More -> Icons.Default.Menu
                Screen.Alerts -> Icons.Default.Notifications
                Screen.Profile -> Icons.Default.Person
                Screen.Settings -> Icons.Default.Settings
                Screen.Contacts -> Icons.Default.Notifications
                Screen.UpdateCenter -> Icons.Default.Settings
                Screen.AirportInfo -> Icons.Default.AirplanemodeActive
                Screen.RosterHistory -> Icons.Default.DateRange
                Screen.Leave -> Icons.Default.DateRange
                Screen.Mel -> Icons.Default.AirplanemodeActive
            }

            val selected = if (screen == Screen.More) {
                currentRoute in listOf(
                    Screen.More.route,
                    Screen.Alerts.route,
                    Screen.Profile.route,
                    Screen.Settings.route,
                    Screen.Contacts.route,
                    Screen.UpdateCenter.route,
                    Screen.AirportInfo.route,
                    Screen.RosterHistory.route,
                    Screen.Leave.route
                )
            } else {
                currentDestination?.hierarchy?.any { it.route == screen.route } == true
            }

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
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
                        text = bottomLabel(screen, ru),
                        maxLines = 1,
                        softWrap = false,
                        fontSize = 10.sp
                    )
                }
            )
        }
    }
}


private fun bottomLabel(screen: Screen, ru: Boolean): String {
    return when (screen) {
        Screen.Schedule -> if (ru) "Ростер" else "Roster"
        Screen.Calendar -> if (ru) "Кален." else "Calendar"
        Screen.Weather -> if (ru) "Метео" else "Weather"
        Screen.Fleet -> if (ru) "Флот" else "Fleet"
        Screen.More -> if (ru) "Ещё" else "More"
        Screen.Alerts -> if (ru) "Алерты" else "Alerts"
        Screen.Profile -> if (ru) "Профиль" else "Profile"
        Screen.Settings -> if (ru) "Настр." else "Settings"
        Screen.Contacts -> if (ru) "Контакты" else "Contacts"
        Screen.UpdateCenter -> if (ru) "Обновл." else "Update"
        Screen.AirportInfo -> if (ru) "Аэроп." else "Airports"
        Screen.RosterHistory -> if (ru) "История" else "History"
        Screen.Leave -> if (ru) "Отпуск" else "Leave"
        Screen.Mel -> "MEL"
    }
}

@Composable
private fun MoreScreen(navController: NavHostController, ru: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (ru) "Ещё" else "More",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        MoreMenuCard(
            title = if (ru) "Профиль" else "Profile",
            subtitle = if (ru) "Профиль пилота, налёт и квалификации" else "Pilot profile, flight time and qualifications",
            onClick = { navController.navigate(Screen.Profile.route) }
        )

        MoreMenuCard(
            title = if (ru) "Уведомления" else "Alerts",
            subtitle = if (ru) "Сообщения экипажа и компании" else "Crew notifications and company messages",
            onClick = { navController.navigate(Screen.Alerts.route) }
        )

        MoreMenuCard(
            title = if (ru) "Контакты компании" else "Company Contacts",
            subtitle = if (ru) "Оперативные службы и контакты BKK" else "Operations, flight planning and BKK ATC contacts",
            onClick = { navController.navigate(Screen.Contacts.route) }
        )


        MoreMenuCard(
            title = if (ru) "Центр обновлений" else "Update Center",
            subtitle = if (ru) "Версия приложения, синхронизация и журнал изменений" else "App version, synchronization and change log",
            onClick = { navController.navigate(Screen.UpdateCenter.route) }
        )

        MoreMenuCard(
            title = if (ru) "Аэропорты" else "Airport Info",
            subtitle = if (ru) "ВПП, часовые пояса и заметки станций" else "Runways, time zones and station notes",
            onClick = { navController.navigate(Screen.AirportInfo.route) }
        )

        MoreMenuCard(
            title = if (ru) "Отпуск и больничный" else "Leave Management",
            subtitle = if (ru) "Отпуск, заявки и больничный" else "Annual leave, personal leave and sick leave",
            onClick = { navController.navigate(Screen.Leave.route) }
        )

        MoreMenuCard(
            title = if (ru) "История ростера" else "Roster Change History",
            subtitle = if (ru) "Назначения бортов, гейтов и изменения" else "Aircraft, gate and roster assignment events",
            onClick = { navController.navigate(Screen.RosterHistory.route) }
        )


        MoreMenuCard(
            title = if (ru) "Настройки" else "Settings",
            subtitle = if (ru) "Синхронизация, внешний вид и действия" else "Synchronization, appearance and app actions",
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
