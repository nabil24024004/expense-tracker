package com.example.group.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "settlements",
    indices = [Index(value = ["groupId"])]
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val payerId: Int,
    val payeeId: Int,
    val amount: Double,
    val date: Long,
    val notes: String?
)
