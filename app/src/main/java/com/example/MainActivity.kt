package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.audio.SoundManager
import com.example.data.GamePreferences
import com.example.ui.GameScreen
import com.example.ui.theme.BlockBlastTheme
import com.example.ui.theme.DarkBackground
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    private val soundManager by lazy { SoundManager(applicationContext) }
    private val preferences by lazy { GamePreferences(applicationContext) }

    private val viewModel: GameViewModel by viewModels {
        GameViewModel.provideFactory(applicationContext, soundManager, preferences)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockBlastTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    GameScreen(viewModel = viewModel)
                }
            }
        }
    }
}
