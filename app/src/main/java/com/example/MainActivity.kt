package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockBlastTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    AppContent(
                        soundManager = soundManager,
                        preferences = preferences,
                        localSessionStorage = localSessionStorage
                    )
                }
            }
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
    localSessionStorage: LocalSessionStorage
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        // Gate all Firestore queries & feature ViewModels until Google Sign-In completes
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Player"
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val credentialManager = remember(context) { CredentialManager.create(context) }

        key(uid) {
            val firestoreRepository = remember(uid) {
                GameFirestoreRepository(context.applicationContext)
            }
            val gameViewModel: GameViewModel = viewModel(
                key = uid,
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
                onSignOut = {
                    gameViewModel.flushSession(syncToCloud = true)
                    signOut(
                        credentialManager = credentialManager,
                        scope = coroutineScope
                    )
                }
            )
        }
    }
}
