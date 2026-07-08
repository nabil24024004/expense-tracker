package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts_dues")
data class DebtDue(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personName: String,
    val amount: Double,
    val description: String,
    val date: Long,
    val dueDate: Long? = null,
    val type: String, // "DEBT" (owed to others) or "DUE" (owed to me)
    val isCleared: Boolean = false,
    val isSynced: Boolean = false,
    val sheetRow: Int? = null,
    val accountId: Int? = null
)
