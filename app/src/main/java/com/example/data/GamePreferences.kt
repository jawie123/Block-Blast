package com.example.data

import android.content.Context
import android.content.SharedPreferences

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("block_blast_prefs", Context.MODE_PRIVATE)

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(value) = prefs.edit().putInt(KEY_HIGH_SCORE, value).apply()

    var highestCombo: Int
        get() = prefs.getInt(KEY_HIGHEST_COMBO, 0)
        set(value) = prefs.edit().putInt(KEY_HIGHEST_COMBO, value).apply()

    var totalLinesCleared: Int
        get() = prefs.getInt(KEY_TOTAL_LINES, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_LINES, value).apply()

    var gamesPlayed: Int
        get() = prefs.getInt(KEY_GAMES_PLAYED, 0)
        set(value) = prefs.edit().putInt(KEY_GAMES_PLAYED, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()

    var hasSeenTutorial: Boolean
        get() = prefs.getBoolean(KEY_HAS_SEEN_TUTORIAL, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_SEEN_TUTORIAL, value).apply()

    companion object {
        private const val KEY_HIGH_SCORE = "key_high_score"
        private const val KEY_HIGHEST_COMBO = "key_highest_combo"
        private const val KEY_TOTAL_LINES = "key_total_lines"
        private const val KEY_GAMES_PLAYED = "key_games_played"
        private const val KEY_SOUND_ENABLED = "key_sound_enabled"
        private const val KEY_HAPTICS_ENABLED = "key_haptics_enabled"
        private const val KEY_HAS_SEEN_TUTORIAL = "key_has_seen_tutorial"
    }
}
