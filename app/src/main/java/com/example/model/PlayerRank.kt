package com.example.model

import androidx.compose.runtime.Immutable

enum class RankTier(
    val level: Int,
    val title: String,
    val badge: String,
    val minTotalScore: Long,
    val minBestScore: Int,
    val colorHex: Long
) {
    BRONZE(
        level = 1,
        title = "Bronze",
        badge = "🥉",
        minTotalScore = 0L,
        minBestScore = 0,
        colorHex = 0xFFCD7F32
    ),
    SILVER(
        level = 2,
        title = "Silver",
        badge = "🥈",
        minTotalScore = 5_000L,
        minBestScore = 1_500,
        colorHex = 0xFFE0E0E0
    ),
    GOLD(
        level = 3,
        title = "Gold",
        badge = "🥇",
        minTotalScore = 25_000L,
        minBestScore = 4_000,
        colorHex = 0xFFFFD700
    ),
    PLATINUM(
        level = 4,
        title = "Platinum",
        badge = "💠",
        minTotalScore = 75_000L,
        minBestScore = 8_000,
        colorHex = 0xFF00E5FF
    ),
    DIAMOND(
        level = 5,
        title = "Diamond",
        badge = "💎",
        minTotalScore = 200_000L,
        minBestScore = 15_000,
        colorHex = 0xFF7C4DFF
    ),
    GRANDMASTER(
        level = 6,
        title = "Grandmaster",
        badge = "👑",
        minTotalScore = 500_000L,
        minBestScore = 25_000,
        colorHex = 0xFFFF4081
    )
}

object RankTierCatalog {
    fun fromProgress(totalScore: Long, bestScore: Int): RankTier {
        var resolved = RankTier.BRONZE
        for (tier in RankTier.entries) {
            if (totalScore >= tier.minTotalScore || bestScore >= tier.minBestScore) {
                resolved = tier
            }
        }
        return resolved
    }

    fun nextTier(current: RankTier): RankTier? {
        val all = RankTier.entries
        val idx = all.indexOf(current)
        return if (idx in 0 until all.lastIndex) all[idx + 1] else null
    }

    fun progressToNextTier(totalScore: Long, bestScore: Int): Float {
        val current = fromProgress(totalScore, bestScore)
        val next = nextTier(current) ?: return 1f
        val totalFraction = if (next.minTotalScore > current.minTotalScore) {
            ((totalScore - current.minTotalScore).toFloat() /
                (next.minTotalScore - current.minTotalScore).toFloat()).coerceIn(0f, 1f)
        } else 0f
        val bestFraction = if (next.minBestScore > current.minBestScore) {
            ((bestScore - current.minBestScore).toFloat() /
                (next.minBestScore - current.minBestScore).toFloat()).coerceIn(0f, 1f)
        } else 0f
        return maxOf(totalFraction, bestFraction)
    }
}

@Immutable
data class AvatarBadgeSpec(
    val id: String,
    val emoji: String,
    val title: String,
    val requiredRank: RankTier = RankTier.BRONZE,
    val requiredStars: Int = 0
) {
    fun isUnlocked(currentRank: RankTier, totalAdventureStars: Int): Boolean {
        return currentRank.level >= requiredRank.level && totalAdventureStars >= requiredStars
    }

    fun unlockRequirementLabel(): String = when {
        requiredStars > 0 && requiredRank.level > 1 -> "${requiredRank.title} + $requiredStars⭐"
        requiredStars > 0 -> "$requiredStars⭐ Adventure"
        requiredRank.level > 1 -> "${requiredRank.title} Rank"
        else -> "Unlocked"
    }
}

object PlayerAvatarCatalog {
    val allAvatars: List<AvatarBadgeSpec> = listOf(
        AvatarBadgeSpec(id = "rookie", emoji = "🧱", title = "Block Rookie"),
        AvatarBadgeSpec(id = "striker", emoji = "⚡", title = "Neon Striker"),
        AvatarBadgeSpec(id = "blaze", emoji = "🔥", title = "Combo Blaze", requiredRank = RankTier.SILVER),
        AvatarBadgeSpec(id = "frost", emoji = "❄️", title = "Frost Breaker", requiredStars = 10),
        AvatarBadgeSpec(id = "hunter", emoji = "💎", title = "Gem Hunter", requiredRank = RankTier.GOLD),
        AvatarBadgeSpec(id = "artist", emoji = "🎨", title = "Color Master", requiredRank = RankTier.PLATINUM),
        AvatarBadgeSpec(id = "astral", emoji = "🚀", title = "Astral Ace", requiredStars = 30),
        AvatarBadgeSpec(id = "crown", emoji = "👑", title = "Crown Legend", requiredRank = RankTier.DIAMOND)
    )

    fun byId(id: String): AvatarBadgeSpec {
        return allAvatars.firstOrNull { it.id == id } ?: allAvatars.first()
    }
}
