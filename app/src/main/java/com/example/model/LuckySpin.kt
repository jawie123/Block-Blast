package com.example.model

import androidx.compose.runtime.Immutable
import kotlin.random.Random

enum class SpinRewardType {
    COINS,
    HAMMER,
    ROTATE,
    SHUFFLE,
    BOMB
}

@Immutable
data class SpinWheelSlice(
    val index: Int,
    val label: String,
    val emoji: String,
    val rewardType: SpinRewardType,
    val amount: Int,
    val weight: Int,
    val accentHex: Long
)

@Immutable
data class LuckySpinResult(
    val slice: SpinWheelSlice,
    val usedFreeDailySpin: Boolean
)

object LuckySpinCatalog {
    val slices: List<SpinWheelSlice> = listOf(
        SpinWheelSlice(
            index = 0,
            label = "+25 COINS",
            emoji = "🪙",
            rewardType = SpinRewardType.COINS,
            amount = 25,
            weight = 26,
            accentHex = 0xFF00B0FF
        ),
        SpinWheelSlice(
            index = 1,
            label = "+1 HAMMER",
            emoji = "🔨",
            rewardType = SpinRewardType.HAMMER,
            amount = 1,
            weight = 16,
            accentHex = 0xFFFF9100
        ),
        SpinWheelSlice(
            index = 2,
            label = "+50 COINS",
            emoji = "🪙",
            rewardType = SpinRewardType.COINS,
            amount = 50,
            weight = 20,
            accentHex = 0xFF7C4DFF
        ),
        SpinWheelSlice(
            index = 3,
            label = "+1 ROTATE",
            emoji = "↻",
            rewardType = SpinRewardType.ROTATE,
            amount = 1,
            weight = 14,
            accentHex = 0xFFE040FB
        ),
        SpinWheelSlice(
            index = 4,
            label = "+100 COINS",
            emoji = "💰",
            rewardType = SpinRewardType.COINS,
            amount = 100,
            weight = 10,
            accentHex = 0xFF00E676
        ),
        SpinWheelSlice(
            index = 5,
            label = "+1 SHUFFLE",
            emoji = "🔄",
            rewardType = SpinRewardType.SHUFFLE,
            amount = 1,
            weight = 8,
            accentHex = 0xFF2979FF
        ),
        SpinWheelSlice(
            index = 6,
            label = "+1 BOMB",
            emoji = "💣",
            rewardType = SpinRewardType.BOMB,
            amount = 1,
            weight = 4,
            accentHex = 0xFFFF5252
        ),
        SpinWheelSlice(
            index = 7,
            label = "+250 JACKPOT",
            emoji = "👑",
            rewardType = SpinRewardType.COINS,
            amount = 250,
            weight = 2,
            accentHex = 0xFFFFD700
        )
    )

    fun sliceByIndex(index: Int): SpinWheelSlice {
        val safeIdx = ((index % slices.size) + slices.size) % slices.size
        return slices[safeIdx]
    }

    fun spinRandom(random: Random = Random.Default): SpinWheelSlice {
        val totalWeight = slices.sumOf { it.weight }.coerceAtLeast(1)
        var roll = random.nextInt(totalWeight)
        for (slice in slices) {
            roll -= slice.weight
            if (roll < 0) return slice
        }
        return slices.first()
    }
}
