package com.example.crewportal

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import com.example.crewportal.data.local.AppDatabase
import com.example.crewportal.data.remote.WeatherRepository
import com.example.crewportal.data.repository.AuthRepository
import com.example.crewportal.data.repository.FlightRepository
import com.example.crewportal.data.repository.PreferencesRepository
import com.example.crewportal.ui.auth.LoginScreen
import com.example.crewportal.ui.navigation.MainNavigation
import com.example.crewportal.ui.theme.CrewPortalTheme
import com.example.crewportal.ui.theme.ThaiPurple
import com.example.crewportal.util.NotificationHelper
import kotlinx.coroutines.delay

class MainActivity : FragmentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialDestination = intent?.getStringExtra("destination")

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
                var showSplash by remember { mutableStateOf(true) }
                var sessionAuthenticated by remember { mutableStateOf(false) }
                var showSyncDialog by remember { mutableStateOf(false) }
                val language by preferencesRepository.appLanguage.collectAsState(initial = "en")
                val ru = language == "ru"

                LaunchedEffect(Unit) {
                    authRepository.signOut()
                    flightRepository.refreshBuiltInRosterOnAppUpdate(BuildConfig.VERSION_NAME)
                    flightRepository.refreshCompletedFlights(showNotifications = false)
                    delay(1800)
                    showSplash = false
                }

                Crossfade(targetState = showSplash, animationSpec = tween(450), label = "appSplash") { splash ->
                    if (splash) {
                        SplashScreen()
                    } else if (sessionAuthenticated) {
                        Box(Modifier.fillMaxSize()) {
                            MainNavigation(
                                flightRepository = flightRepository,
                                preferencesRepository = preferencesRepository,
                                weatherRepository = weatherRepository,
                                initialRoute = initialDestination,
                                onLogout = {
                                    authRepository.signOut()
                                    sessionAuthenticated = false
                                }
                            )
                            AnimatedVisibility(
                                visible = showSyncDialog,
                                enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.96f),
                                exit = fadeOut(tween(250)) + scaleOut(targetScale = 0.98f),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                SyncDialogCard(ru = ru)
                            }
                        }
                    } else {
                        LoginScreen(
                            activity = this,
                            authRepository = authRepository,
                            preferencesRepository = preferencesRepository,
                            onAuthenticated = {
                                sessionAuthenticated = true
                                showSyncDialog = true
                            }
                        )
                    }
                }

                LaunchedEffect(sessionAuthenticated) {
                    if (sessionAuthenticated) {
                        delay(2800)
                        showSyncDialog = false
                        Toast.makeText(
                            applicationContext,
                            if (ru) "Синхронизация успешно завершена" else "Synchronization completed successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splash_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(34.dp),
                color = Color(0xFFE2C45B),
                strokeWidth = 3.dp
            )
            Text(
                text = "Connecting to company network...",
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                color = Color.White.copy(alpha = 0.74f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SyncDialogCard(ru: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = ThaiPurple,
                strokeWidth = 3.dp
            )
            Text(
                text = if (ru) "Синхронизация с базой данных компании" else "Synchronizing with company database",
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (ru) "Пожалуйста, подождите..." else "Please wait...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
