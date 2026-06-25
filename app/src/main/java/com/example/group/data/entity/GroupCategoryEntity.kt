package com.example.group.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_categories",
    indices = [Index(value = ["groupId"])]
)
data class GroupCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int?, // null for default/app categories, specific ID for custom group categories
    val name: String,
    val iconName: String?
)
