package com.example.data

import android.content.Context
import android.content.SharedPreferences

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("block_blast_prefs", Context.MODE_PRIVATE)

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(value) = prefs.edit().putInt(KEY_HIGH_SCORE, value).apply()

    var timeAttackHighScore: Int
        get() = prefs.getInt(KEY_TIME_ATTACK_HIGH_SCORE, 0)
        set(value) = prefs.edit().putInt(KEY_TIME_ATTACK_HIGH_SCORE, value).apply()

    var highestCombo: Int
        get() = prefs.getInt(KEY_HIGHEST_COMBO, 0)
        set(value) = prefs.edit().putInt(KEY_HIGHEST_COMBO, value).apply()

    var totalLinesCleared: Int
        get() = prefs.getInt(KEY_TOTAL_LINES, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_LINES, value).apply()

    var gamesPlayed: Int
        get() = prefs.getInt(KEY_GAMES_PLAYED, 0)
        set(value) = prefs.edit().putInt(KEY_GAMES_PLAYED, value).apply()

    var isMusicEnabled: Boolean
        get() = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MUSIC_ENABLED, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()

    var hasSeenTutorial: Boolean
        get() = prefs.getBoolean(KEY_HAS_SEEN_TUTORIAL, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_SEEN_TUTORIAL, value).apply()

    var isGuestSession: Boolean
        get() = prefs.getBoolean(KEY_IS_GUEST_SESSION, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_GUEST_SESSION, value).apply()

    var dailyLastPlayedDate: String
        get() = prefs.getString(KEY_DAILY_LAST_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DAILY_LAST_DATE, value).apply()

    var dailyStreak: Int
        get() = prefs.getInt(KEY_DAILY_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_DAILY_STREAK, value).apply()

    fun getDailyHighScore(date: String): Int {
        return prefs.getInt("key_daily_score_$date", 0)
    }

    fun setDailyHighScore(date: String, score: Int) {
        val current = getDailyHighScore(date)
        if (score > current) {
            prefs.edit().putInt("key_daily_score_$date", score).apply()
        }
    }

    fun getDailyHistory(): String {
        return prefs.getString(KEY_DAILY_HISTORY, "") ?: ""
    }

    var hammerCount: Int
        get() = prefs.getInt(KEY_HAMMER_COUNT, 3)
        set(value) = prefs.edit().putInt(KEY_HAMMER_COUNT, value).apply()

    var hintCount: Int
        get() = prefs.getInt(KEY_HINT_COUNT, 2).coerceAtLeast(0)
        set(value) = prefs.edit().putInt(KEY_HINT_COUNT, value.coerceAtLeast(0)).apply()

    var shuffleCount: Int
        get() = prefs.getInt(KEY_SHUFFLE_COUNT, 2).coerceAtLeast(0)
        set(value) = prefs.edit().putInt(KEY_SHUFFLE_COUNT, value.coerceAtLeast(0)).apply()

    var bombCount: Int
        get() = prefs.getInt(KEY_BOMB_COUNT, 1).coerceAtLeast(0)
        set(value) = prefs.edit().putInt(KEY_BOMB_COUNT, value.coerceAtLeast(0)).apply()

    var rotateCount: Int
        get() = prefs.getInt(KEY_ROTATE_COUNT, 3).coerceAtLeast(0)
        set(value) = prefs.edit().putInt(KEY_ROTATE_COUNT, value.coerceAtLeast(0)).apply()

    var adventureCurrentLevel: Int
        get() = prefs.getInt(KEY_ADVENTURE_CURRENT_LEVEL, 1).coerceAtLeast(1)
        set(value) = prefs.edit().putInt(KEY_ADVENTURE_CURRENT_LEVEL, value.coerceAtLeast(1)).apply()

    var adventureMaxUnlockedLevel: Int
        get() = prefs.getInt(KEY_ADVENTURE_MAX_UNLOCKED_LEVEL, 1).coerceAtLeast(1)
        set(value) = prefs.edit().putInt(KEY_ADVENTURE_MAX_UNLOCKED_LEVEL, value.coerceAtLeast(1)).apply()

    fun getAdventureLevelStars(level: Int): Int =
        prefs.getInt("adv_stars_$level", 0).coerceIn(0, 3)

    fun setAdventureLevelStars(level: Int, stars: Int) {
        val current = getAdventureLevelStars(level)
        if (stars > current) {
            prefs.edit().putInt("adv_stars_$level", stars.coerceIn(0, 3)).apply()
        }
    }

    fun isAdventureLevelRewardClaimed(level: Int): Boolean =
        prefs.getBoolean("adv_reward_claimed_$level", false)

    fun markAdventureLevelRewardClaimed(level: Int) {
        prefs.edit().putBoolean("adv_reward_claimed_$level", true).apply()
    }

    fun getAdventureHighScore(level: Int): Int =
        prefs.getInt("adv_best_score_$level", 0)

    fun setAdventureHighScore(level: Int, score: Int) {
        val current = getAdventureHighScore(level)
        if (score > current) {
            prefs.edit().putInt("adv_best_score_$level", score).apply()
        }
    }

    var selectedThemeId: String
        get() = prefs.getString(KEY_SELECTED_THEME_ID, com.example.model.BoardThemeId.NEON_ARCADE.name)
            ?: com.example.model.BoardThemeId.NEON_ARCADE.name
        set(value) = prefs.edit().putString(KEY_SELECTED_THEME_ID, value).apply()

    fun getUnlockedThemeIds(): Set<String> {
        val stored = prefs.getStringSet(KEY_UNLOCKED_THEME_IDS, emptySet()) ?: emptySet()
        return stored +
            com.example.model.BoardThemeId.NEON_ARCADE.name +
            com.example.model.BoardThemeId.CLASSIC_BLOCK.name
    }

    fun isThemeUnlocked(themeId: String): Boolean =
        themeId == com.example.model.BoardThemeId.NEON_ARCADE.name ||
            themeId == com.example.model.BoardThemeId.CLASSIC_BLOCK.name ||
            getUnlockedThemeIds().contains(themeId)

    fun unlockTheme(themeId: String) {
        val updated = getUnlockedThemeIds() + themeId
        prefs.edit().putStringSet(KEY_UNLOCKED_THEME_IDS, updated).apply()
    }

    var totalAllTimeScore: Long
        get() = prefs.getLong(KEY_TOTAL_ALL_TIME_SCORE, 0L)
        set(value) = prefs.edit().putLong(KEY_TOTAL_ALL_TIME_SCORE, value.coerceAtLeast(0L)).apply()

    fun addToTotalAllTimeScore(delta: Int): Long {
        if (delta <= 0) return totalAllTimeScore
        val updated = (totalAllTimeScore + delta.toLong()).coerceAtMost(1_000_000_000L)
        totalAllTimeScore = updated
        return updated
    }

    fun getMissionProgress(missionId: String): Int {
        return prefs.getInt("mission_prog_$missionId", 0)
    }

    fun setMissionProgress(missionId: String, value: Int) {
        prefs.edit().putInt("mission_prog_$missionId", value).apply()
    }

    fun isMissionClaimed(missionId: String): Boolean {
        return prefs.getBoolean("mission_claimed_$missionId", false)
    }

    fun setMissionClaimed(missionId: String, claimed: Boolean) {
        prefs.edit().putBoolean("mission_claimed_$missionId", claimed).apply()
    }

    fun recordDailyGame(date: String, score: Int) {
        setDailyHighScore(date, score)
        val history = getDailyHistory()
        val entry = "$date:$score"
        if (!history.contains(date)) {
            val updated = if (history.isEmpty()) entry else "$history,$entry"
            prefs.edit().putString(KEY_DAILY_HISTORY, updated).apply()
        }
    }

    var dailyChallengesCompleted: Int
        get() = prefs.getInt(KEY_DAILY_CHALLENGES_COMPLETED, 0)
        set(value) = prefs.edit().putInt(KEY_DAILY_CHALLENGES_COMPLETED, value.coerceAtLeast(0)).apply()

    fun isDailyDateCompleted(date: String): Boolean {
        if (date.isBlank()) return false
        return prefs.getBoolean("key_daily_completed_$date", false)
    }

    fun markDailyDateCompleted(date: String): Boolean {
        if (date.isBlank() || isDailyDateCompleted(date)) return false
        prefs.edit().putBoolean("key_daily_completed_$date", true).apply()
        dailyChallengesCompleted = dailyChallengesCompleted + 1
        return true
    }

    /**
     * Updates the player's Daily Challenge streak when playing on [todayDate].
     * Increments streak when [dailyLastPlayedDate] equals [yesterdayDate], keeps it if already
     * recorded today, or resets to 1 when a day was missed.
     */
    fun recordDailyParticipation(todayDate: String, yesterdayDate: String): Int {
        if (todayDate.isBlank()) return dailyStreak
        val lastDate = dailyLastPlayedDate
        val updatedStreak = when {
            lastDate == todayDate -> maxOf(1, dailyStreak)
            lastDate == yesterdayDate -> dailyStreak + 1
            else -> 1
        }
        dailyLastPlayedDate = todayDate
        dailyStreak = updatedStreak
        return updatedStreak
    }

    fun isAchievementUnlocked(achievementId: String): Boolean {
        return prefs.getBoolean("ach_unlocked_$achievementId", false)
    }

    fun setAchievementUnlocked(achievementId: String, unlocked: Boolean) {
        prefs.edit().putBoolean("ach_unlocked_$achievementId", unlocked).apply()
    }

    fun getAchievementProgress(achievementId: String): Int {
        return prefs.getInt("ach_prog_$achievementId", 0)
    }

    fun setAchievementProgress(achievementId: String, progress: Int) {
        prefs.edit().putInt("ach_prog_$achievementId", progress.coerceAtLeast(0)).apply()
    }

    var coinsBalance: Int
        get() = prefs.getInt(KEY_COINS_BALANCE, 0)
        set(value) = prefs.edit().putInt(KEY_COINS_BALANCE, value.coerceAtLeast(0)).apply()

    var loginStreak: Int
        get() = prefs.getInt(KEY_LOGIN_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_LOGIN_STREAK, value.coerceAtLeast(0)).apply()

    var longestLoginStreak: Int
        get() = prefs.getInt(KEY_LONGEST_LOGIN_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_LONGEST_LOGIN_STREAK, value.coerceAtLeast(0)).apply()

    var lastLoginClaimDate: String
        get() = prefs.getString(KEY_LAST_LOGIN_CLAIM_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_LOGIN_CLAIM_DATE, value).apply()

    var totalLoginDaysClaimed: Int
        get() = prefs.getInt(KEY_TOTAL_LOGIN_DAYS_CLAIMED, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_LOGIN_DAYS_CLAIMED, value.coerceAtLeast(0)).apply()

    var totalCoinsEarned: Int
        get() = prefs.getInt(KEY_TOTAL_COINS_EARNED, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_COINS_EARNED, value.coerceAtLeast(0)).apply()

    var totalLoginHammersEarned: Int
        get() = prefs.getInt(KEY_TOTAL_LOGIN_HAMMERS_EARNED, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_LOGIN_HAMMERS_EARNED, value.coerceAtLeast(0)).apply()

    var lastLuckySpinDate: String
        get() = prefs.getString(KEY_LAST_LUCKY_SPIN_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_LUCKY_SPIN_DATE, value).apply()

    var bonusSpinCount: Int
        get() = prefs.getInt(KEY_BONUS_SPIN_COUNT, 0).coerceAtLeast(0)
        set(value) = prefs.edit().putInt(KEY_BONUS_SPIN_COUNT, value.coerceAtLeast(0)).apply()

    fun canClaimFreeSpinToday(todayDate: String): Boolean {
        if (todayDate.isBlank()) return false
        return lastLuckySpinDate != todayDate
    }

    companion object {
        private const val KEY_HIGH_SCORE = "key_high_score"
        private const val KEY_HIGHEST_COMBO = "key_highest_combo"
        private const val KEY_TOTAL_LINES = "key_total_lines"
        private const val KEY_GAMES_PLAYED = "key_games_played"
        private const val KEY_MUSIC_ENABLED = "key_music_enabled"
        private const val KEY_SOUND_ENABLED = "key_sound_enabled"
        private const val KEY_HAPTICS_ENABLED = "key_haptics_enabled"
        private const val KEY_HAS_SEEN_TUTORIAL = "key_has_seen_tutorial"
        private const val KEY_DAILY_LAST_DATE = "key_daily_last_date"
        private const val KEY_DAILY_STREAK = "key_daily_streak"
        private const val KEY_DAILY_HISTORY = "key_daily_history"
        private const val KEY_HAMMER_COUNT = "key_hammer_count"
        private const val KEY_HINT_COUNT = "key_hint_count"
        private const val KEY_SHUFFLE_COUNT = "key_shuffle_count"
        private const val KEY_BOMB_COUNT = "key_bomb_count"
        private const val KEY_ROTATE_COUNT = "key_rotate_count"
        private const val KEY_ADVENTURE_CURRENT_LEVEL = "key_adventure_current_level"
        private const val KEY_ADVENTURE_MAX_UNLOCKED_LEVEL = "key_adventure_max_unlocked_level"
        private const val KEY_SELECTED_THEME_ID = "key_selected_theme_id"
        private const val KEY_UNLOCKED_THEME_IDS = "key_unlocked_theme_ids"
        private const val KEY_TOTAL_ALL_TIME_SCORE = "key_total_all_time_score"
        private const val KEY_TIME_ATTACK_HIGH_SCORE = "key_time_attack_high_score"
        private const val KEY_DAILY_CHALLENGES_COMPLETED = "key_daily_challenges_completed"
        private const val KEY_IS_GUEST_SESSION = "key_is_guest_session"
        private const val KEY_COINS_BALANCE = "key_coins_balance"
        private const val KEY_LOGIN_STREAK = "key_login_streak"
        private const val KEY_LONGEST_LOGIN_STREAK = "key_longest_login_streak"
        private const val KEY_LAST_LOGIN_CLAIM_DATE = "key_last_login_claim_date"
        private const val KEY_TOTAL_LOGIN_DAYS_CLAIMED = "key_total_login_days_claimed"
        private const val KEY_TOTAL_COINS_EARNED = "key_total_coins_earned"
        private const val KEY_TOTAL_LOGIN_HAMMERS_EARNED = "key_total_login_hammers_earned"
        private const val KEY_LAST_LUCKY_SPIN_DATE = "key_last_lucky_spin_date"
        private const val KEY_BONUS_SPIN_COUNT = "key_bonus_spin_count"
    }
}
