package com.neosparkx.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["date"]), Index(value = ["accountId"])]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val description: String,
    val category: String,
    val date: Long,
    val isSynced: Boolean = false,
    val sheetRow: Int? = null,
    val type: String = "EXPENSE",
    val accountId: Int? = null,
    val toAccountId: Int? = null,
    val tags: String = ""
)

