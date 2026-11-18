package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AdMobRewardedCoordinator
import com.example.ads.RewardedAdController
import com.example.audio.SoundManager
import com.example.data.GameFirestoreRepository
import com.example.data.GamePreferences
import com.example.data.LocalSessionStorage
import com.example.ui.GameScreen
import com.example.ui.components.AuthScreen
import com.example.ui.components.signOut
import com.example.ui.theme.BlockBlastTheme
import com.example.ui.theme.DarkBackground
import com.example.viewmodel.GameViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private val soundManager by lazy { SoundManager(applicationContext) }
    private val preferences by lazy { GamePreferences(applicationContext) }
    private val localSessionStorage by lazy { LocalSessionStorage(applicationContext) }
    private val adCoordinator by lazy { AdMobRewardedCoordinator(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            BlockBlastTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    AppContent(
                        soundManager = soundManager,
                        preferences = preferences,
                        localSessionStorage = localSessionStorage,
                        adCoordinator = adCoordinator
                    )
                }
            }
        }
        runCatching {
            adCoordinator.initializeAndGatherConsent(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}

@Composable
private fun AppContent(
    soundManager: SoundManager,
    preferences: GamePreferences,
    localSessionStorage: LocalSessionStorage,
    adCoordinator: RewardedAdController
) {
    val firebaseAuth = remember { runCatching { Firebase.auth }.getOrNull() }
    var currentUser by remember { mutableStateOf(firebaseAuth?.currentUser) }
    var showAuthScreen by remember { mutableStateOf(false) }

    DisposableEffect(firebaseAuth) {
        if (firebaseAuth == null) {
            return@DisposableEffect onDispose {}
        }
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val signedInUser = auth.currentUser
            currentUser = signedInUser
            if (signedInUser != null) {
                preferences.isGuestSession = false
                showAuthScreen = false
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        onDispose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (showAuthScreen && user == null) {
        // Optional Google Sign-In screen opened only when user taps Sign In
        AuthScreen(
            onAuthSuccess = {
                preferences.isGuestSession = false
                showAuthScreen = false
                currentUser = firebaseAuth?.currentUser
            },
            onContinueAsGuest = {
                preferences.isGuestSession = true
                showAuthScreen = false
            }
        )
    } else {
        val isGuestSession = user == null
        val sessionKey = user?.uid ?: "guest"
        val uid = user?.uid ?: ""
        val displayName = if (isGuestSession) {
            "Guest"
        } else {
            user?.displayName?.takeIf { it.isNotBlank() } ?: "Player"
        }
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val credentialManager = remember(context) { CredentialManager.create(context) }

        key(sessionKey) {
            val firestoreRepository = remember(uid) {
                if (uid.isNotBlank()) {
                    GameFirestoreRepository(context.applicationContext)
                } else {
                    null
                }
            }
            val gameViewModel: GameViewModel = viewModel(
                key = sessionKey,
                factory = GameViewModel.provideFactory(
                    context = context.applicationContext,
                    soundManager = soundManager,
                    preferences = preferences,
                    localSessionStorage = localSessionStorage,
                    firestoreRepository = firestoreRepository,
                    currentUserId = uid,
                    currentUserDisplayName = displayName
                )
            )
            GameScreen(
                viewModel = gameViewModel,
                adCoordinator = adCoordinator,
                onSignOut = {
                    gameViewModel.flushSession(syncToCloud = !isGuestSession)
                    if (isGuestSession) {
                        showAuthScreen = true
                    } else {
                        signOut(
                            credentialManager = credentialManager,
                            scope = coroutineScope,
                            onSignOutComplete = {
                                preferences.isGuestSession = true
                            }
                        )
                    }
                }
            )
        }
    }
}
