package com.neosparkx.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balance: Double,
    val colorHex: String,
    val icon: String, // "wallet", "bank", "card", "savings"
    val currency: String = "৳",
    val includeInBalance: Boolean = true,
    val displayOrder: Int = 0
)

