package com.example.group.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val currency: String,
    val color: Int, // ARGB color value
    val description: String?,
    val createdDate: Long,
    val isArchived: Boolean = false
)
