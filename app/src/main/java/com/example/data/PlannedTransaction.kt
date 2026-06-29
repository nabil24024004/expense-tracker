package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planned_transactions")
data class PlannedTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val type: String, // "EXPENSE", "INCOME"
    val accountId: Int, // linked account
    val startDate: Long,
    val intervalType: String, // "DAY", "WEEK", "MONTH", "YEAR"
    val intervalN: Int, // e.g. every N intervals
    val oneTime: Boolean, // is a one-time bill/income
    val nextDueDate: Long, // timestamp for the next time it's due
    val isActive: Boolean = true,
    val description: String = ""
)
