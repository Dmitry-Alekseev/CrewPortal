package com.example.crewportal

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.example.crewportal.data.local.AppDatabase
import com.example.crewportal.data.remote.WeatherRepository
import com.example.crewportal.data.repository.AuthRepository
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.ui.auth.LoginScreen
import com.example.crewportal.ui.navigation.MainNavigation
import com.example.crewportal.ui.theme.CrewPortalTheme
import com.example.crewportal.util.NotificationHelper

class MainActivity : FragmentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferencesRepository = PreferencesRepository(applicationContext)
        val authRepository = AuthRepository(preferencesRepository)
        val db = AppDatabase.get(applicationContext)
        val flightRepository = FlightRepository(applicationContext, db.flightDao(), preferencesRepository)
        val weatherRepository = WeatherRepository()

        NotificationHelper.ensureChannel(applicationContext)
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val darkTheme by preferencesRepository.darkTheme.collectAsState(initial = false)
            CrewPortalTheme(darkTheme = darkTheme) {
                var sessionAuthenticated by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    authRepository.signOut()
                    flightRepository.refreshBuiltInRosterOnAppUpdate("1.7.0")
                    flightRepository.refreshCompletedFlights()
                }

                if (sessionAuthenticated) {
                    MainNavigation(
                        flightRepository = flightRepository,
                        preferencesRepository = preferencesRepository,
                        weatherRepository = weatherRepository,
                        onLogout = {
                            authRepository.signOut()
                            sessionAuthenticated = false
                        }
                    )
                } else {
                    LoginScreen(
                        activity = this,
                        authRepository = authRepository,
                        preferencesRepository = preferencesRepository,
                        onAuthenticated = { sessionAuthenticated = true }
                    )
                }
            }
        }
    }
}
