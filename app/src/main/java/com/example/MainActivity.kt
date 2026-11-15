package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.AiSettingsManager
import com.example.ai.UsageQuotaManager
import com.example.analytics.EngagementManager
import com.example.data.local.SenseiLocalDatabase
import com.example.data.repository.SenseiRepository
import com.example.donation.DonationBillingManager
import com.example.ui.MainViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingPreferences
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SenseiSplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MANDATORY: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PocketSenseiApp()
                }
            }
        }
    }
}

@Composable
fun PocketSenseiApp() {
    val appContext = LocalContext.current.applicationContext
    var showSplash by remember { mutableStateOf(true) }
    var showOnboarding by remember {
        mutableStateOf(!OnboardingPreferences.hasCompletedOnboarding(appContext))
    }
    var pendingOnboardingLanguage by remember {
        mutableStateOf(OnboardingPreferences.getPreferredLanguage(appContext) ?: "Japanese")
    }
    var onboardingJustCompleted by remember { mutableStateOf(false) }
    val isFirebaseInitialized = remember { FirebaseApp.getApps(appContext).isNotEmpty() }
    var currentUser by remember {
        mutableStateOf(if (isFirebaseInitialized) Firebase.auth.currentUser else null)
    }
    var isGuestMode by remember { mutableStateOf(false) }

    DisposableEffect(isFirebaseInitialized) {
        if (!isFirebaseInitialized) {
            onDispose {}
        } else {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                currentUser = auth.currentUser
            }
            Firebase.auth.addAuthStateListener(listener)
            onDispose {
                Firebase.auth.removeAuthStateListener(listener)
            }
        }
    }

    if (showSplash) {
        SenseiSplashScreen(
            onSplashFinished = { showSplash = false }
        )
    } else if (showOnboarding) {
        OnboardingScreen(
            initialLanguage = pendingOnboardingLanguage,
            onComplete = { chosenLanguage ->
                pendingOnboardingLanguage = chosenLanguage
                OnboardingPreferences.setOnboardingCompleted(appContext, true, chosenLanguage)
                onboardingJustCompleted = true
                showOnboarding = false
            }
        )
    } else if (currentUser == null && !isGuestMode) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = if (isFirebaseInitialized) Firebase.auth.currentUser else null
            },
            onContinueAsGuest = {
                isGuestMode = true
            }
        )
    } else {
        val uid = currentUser?.uid ?: "guest_local"
        key(uid) {
            val context = LocalContext.current
            val repository = remember(uid) { SenseiRepository(context) }
            val localDb = remember(uid) { SenseiLocalDatabase.getInstance(context) }
            val firestoreDb = remember(isFirebaseInitialized) {
                if (isFirebaseInitialized) {
                    runCatching {
                        FirebaseFirestore.getInstance(
                            appContext.getString(R.string.firestore_database_id)
                        )
                    }.getOrNull()
                } else {
                    null
                }
            }
            val usageQuotaManager = remember(uid) {
                UsageQuotaManager(
                    context = context,
                    firestore = firestoreDb,
                    currentUserIdProvider = {
                        if (isFirebaseInitialized) Firebase.auth.currentUser?.uid else null
                    }
                )
            }
            val aiSettingsManager = remember(uid) { AiSettingsManager(context, usageQuotaManager) }
            val engagementManager = remember(uid) { EngagementManager(context) }
            val donationBillingManager = remember(uid) {
                DonationBillingManager(context, engagementManager)
            }
            val viewModel: MainViewModel = viewModel(
                key = uid,
                factory = MainViewModel.provideFactory(
                    repository = repository,
                    engagementManager = engagementManager,
                    donationBillingManager = donationBillingManager,
                    aiSettingsManager = aiSettingsManager,
                    cacheDao = localDb.cachedEvaluationDao(),
                    appContext = appContext,
                    customPhraseDao = localDb.customPhraseDao(),
                    customScenarioDao = localDb.customScenarioDao()
                )
            )
            val isActiveSupporter by viewModel.isActiveSupporter.collectAsState()
            LaunchedEffect(uid, currentUser, isActiveSupporter) {
                usageQuotaManager.updateAuthStatus(
                    isSignedIn = currentUser != null,
                    isPro = isActiveSupporter
                )
                aiSettingsManager.refreshSettings()
            }
            LaunchedEffect(onboardingJustCompleted, pendingOnboardingLanguage) {
                if (onboardingJustCompleted) {
                    if (!viewModel.targetLanguage.value.equals(pendingOnboardingLanguage, ignoreCase = true)) {
                        viewModel.setTargetLanguage(pendingOnboardingLanguage)
                    }
                    onboardingJustCompleted = false
                }
            }
            HomeScreen(
                viewModel = viewModel,
                onReplayOnboarding = {
                    pendingOnboardingLanguage = viewModel.targetLanguage.value
                    showOnboarding = true
                },
                onSignOutComplete = {
                    currentUser = null
                    isGuestMode = false
                }
            )
        }
    }
}
