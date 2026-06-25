package com.example.group.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_payers",
    indices = [
        Index(value = ["expenseId"]),
        Index(value = ["memberId"])
    ]
)
data class ExpensePayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expenseId: Int,
    val memberId: Int,
    val paidAmount: Double
)
