package com.example.crewportal

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.example.crewportal.data.local.AppDatabase
import com.example.crewportal.data.remote.WeatherRepository
import com.example.crewportal.data.repository.AuthRepository
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.ui.auth.LoginScreen
import com.example.crewportal.ui.navigation.MainNavigation
import com.example.crewportal.ui.theme.CrewPortalTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferencesRepository = PreferencesRepository(applicationContext)
        val authRepository = AuthRepository(preferencesRepository)
        val db = AppDatabase.get(applicationContext)
        val flightRepository = FlightRepository(applicationContext, db.flightDao(), preferencesRepository)
        val weatherRepository = WeatherRepository()

        setContent {
            CrewPortalTheme {
                val isLoggedIn by preferencesRepository.isLoggedIn.collectAsState(initial = false)

                LaunchedEffect(Unit) {
                    flightRepository.loadScheduleFromAssetsIfNeeded()
                    flightRepository.refreshCompletedFlights()
                }

                if (isLoggedIn) {
                    MainNavigation(
                        flightRepository = flightRepository,
                        preferencesRepository = preferencesRepository,
                        weatherRepository = weatherRepository,
                        onLogout = { authRepository.signOut() }
                    )
                } else {
                    LoginScreen(
                        activity = this,
                        authRepository = authRepository,
                        preferencesRepository = preferencesRepository
                    )
                }
            }
        }
    }
}
