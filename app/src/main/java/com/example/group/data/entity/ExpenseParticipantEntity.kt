package com.example.group.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_participants",
    indices = [
        Index(value = ["expenseId"]),
        Index(value = ["memberId"])
    ]
)
data class ExpenseParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expenseId: Int,
    val memberId: Int,
    val shareAmount: Double,
    val splitMethod: String, // EQUAL, EXACT, PERCENTAGE, SHARES, CUSTOM
    val rawWeight: Double? = null // Store the weight/percentage entered
)
