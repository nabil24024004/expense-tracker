package com.example.data

import java.util.Calendar

data class PeriodProjection(
    val spent: Double,
    val projected: Double,
    val predictedSavings: Double,
    val dailyRate: Double,
    val daysTotal: Int,
    val daysElapsed: Int
)

object BudgetPeriodHelper {
    fun getPeriodRange(
        periodType: String,
        customStart: Long,
        customEnd: Long
    ): Pair<Long, Long> {
        return when (periodType) {
            "weekly" -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val end = Calendar.getInstance().apply {
                    timeInMillis = start
                    add(Calendar.DAY_OF_YEAR, 6)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                
                Pair(start, end)
            }
            "custom" -> {
                if (customStart == 0L || customEnd == 0L) {
                    val start = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    val end = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    Pair(start, end)
                } else {
                    val startCal = Calendar.getInstance().apply {
                        timeInMillis = customStart
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val endCal = Calendar.getInstance().apply {
                        timeInMillis = customEnd
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    Pair(startCal.timeInMillis, endCal.timeInMillis)
                }
            }
            else -> { // "monthly"
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val end = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                
                Pair(start, end)
            }
        }
    }

    fun getPeriodProjection(
        expenses: List<Expense>,
        budgetLimit: Double,
        periodType: String,
        customStart: Long,
        customEnd: Long
    ): PeriodProjection {
        val range = getPeriodRange(periodType, customStart, customEnd)
        val start = range.first
        val end = range.second
        
        val spent = expenses.filter { it.date in start..end }.sumOf { it.amount }
        
        // Total days in the period
        val diffMs = end - start
        val daysTotal = maxOf(1, ((diffMs + 5000) / (1000 * 60 * 60 * 24)).toInt() + 1)
        
        // Days elapsed so far
        val nowMs = System.currentTimeMillis()
        val daysElapsed = if (nowMs < start) {
            1 // Avoid division by zero, start with day 1
        } else if (nowMs > end) {
            daysTotal
        } else {
            val elapsedMs = nowMs - start
            minOf(daysTotal, ((elapsedMs + 5000) / (1000 * 60 * 60 * 24)).toInt() + 1)
        }
        
        val dailyRate = if (daysElapsed > 0) spent / daysElapsed else 0.0
        val projected = dailyRate * daysTotal
        val predictedSavings = budgetLimit - projected
        
        return PeriodProjection(
            spent = spent,
            projected = projected,
            predictedSavings = predictedSavings,
            dailyRate = dailyRate,
            daysTotal = daysTotal,
            daysElapsed = daysElapsed
        )
    }
}
