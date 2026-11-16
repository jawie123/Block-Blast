package com.example.model

/**
 * Defines a single day tier inside the 7-day escalating Daily Login Reward cycle.
 */
data class DailyLoginRewardTier(
    val cycleDay: Int,
    val title: String,
    val coins: Int,
    val hammers: Int,
    val isJackpot: Boolean = false
)

enum class DailyLoginDayStatus {
    CLAIMED,
    CLAIMABLE,
    LOCKED
}

data class DailyLoginCalendarDay(
    val tier: DailyLoginRewardTier,
    val status: DailyLoginDayStatus
)

/**
 * Pure schedule & streak progression engine for the 7-day Daily Login Reward System.
 */
object DailyLoginRewardSchedule {
    const val COIN_TO_HAMMER_COST = 100

    val tiers: List<DailyLoginRewardTier> = listOf(
        DailyLoginRewardTier(cycleDay = 1, title = "Day 1", coins = 50, hammers = 0),
        DailyLoginRewardTier(cycleDay = 2, title = "Day 2", coins = 0, hammers = 1),
        DailyLoginRewardTier(cycleDay = 3, title = "Day 3", coins = 100, hammers = 0),
        DailyLoginRewardTier(cycleDay = 4, title = "Day 4", coins = 0, hammers = 2),
        DailyLoginRewardTier(cycleDay = 5, title = "Day 5", coins = 150, hammers = 0),
        DailyLoginRewardTier(cycleDay = 6, title = "Day 6", coins = 100, hammers = 2),
        DailyLoginRewardTier(cycleDay = 7, title = "Jackpot", coins = 500, hammers = 3, isJackpot = true)
    )

    fun tierForStreak(streak: Int): DailyLoginRewardTier {
        val cycleDay = ((streak - 1).coerceAtLeast(0) % tiers.size) + 1
        return tiers[cycleDay - 1]
    }

    fun computeNextStreak(
        lastClaimDate: String,
        storedStreak: Int,
        todayDate: String,
        yesterdayDate: String
    ): Int {
        return when {
            todayDate.isBlank() -> maxOf(1, storedStreak)
            lastClaimDate == todayDate -> maxOf(1, storedStreak)
            lastClaimDate == yesterdayDate && storedStreak > 0 -> storedStreak + 1
            else -> 1
        }
    }

    fun computeActiveStreak(
        lastClaimDate: String,
        storedStreak: Int,
        todayDate: String,
        yesterdayDate: String
    ): Int {
        if (storedStreak <= 0 || lastClaimDate.isBlank()) return 0
        return if (lastClaimDate == todayDate || lastClaimDate == yesterdayDate) {
            storedStreak
        } else {
            0
        }
    }

    fun canClaimToday(lastClaimDate: String, todayDate: String): Boolean {
        return todayDate.isNotBlank() && lastClaimDate != todayDate
    }

    fun nextClaimCycleDay(
        lastClaimDate: String,
        storedStreak: Int,
        todayDate: String,
        yesterdayDate: String
    ): Int {
        return if (canClaimToday(lastClaimDate, todayDate)) {
            tierForStreak(computeNextStreak(lastClaimDate, storedStreak, todayDate, yesterdayDate)).cycleDay
        } else {
            tierForStreak(maxOf(1, storedStreak)).cycleDay
        }
    }

    fun buildCalendar(
        lastClaimDate: String,
        storedStreak: Int,
        todayDate: String,
        yesterdayDate: String
    ): List<DailyLoginCalendarDay> {
        val claimableToday = canClaimToday(lastClaimDate, todayDate)
        return if (claimableToday) {
            val nextStreak = computeNextStreak(lastClaimDate, storedStreak, todayDate, yesterdayDate)
            val claimableCycleDay = tierForStreak(nextStreak).cycleDay
            tiers.map { tier ->
                val status = when {
                    tier.cycleDay < claimableCycleDay -> DailyLoginDayStatus.CLAIMED
                    tier.cycleDay == claimableCycleDay -> DailyLoginDayStatus.CLAIMABLE
                    else -> DailyLoginDayStatus.LOCKED
                }
                DailyLoginCalendarDay(tier = tier, status = status)
            }
        } else {
            val currentCycleDay = tierForStreak(maxOf(1, storedStreak)).cycleDay
            tiers.map { tier ->
                val status = if (tier.cycleDay <= currentCycleDay) {
                    DailyLoginDayStatus.CLAIMED
                } else {
                    DailyLoginDayStatus.LOCKED
                }
                DailyLoginCalendarDay(tier = tier, status = status)
            }
        }
    }
}
