package com.example.group.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "members",
    indices = [Index(value = ["groupId"])]
)
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val name: String,
    val avatarUri: String?,
    val defaultWeight: Double = 1.0,
    val notes: String?,
    val isEnabled: Boolean = true,
    val createdDate: Long
)
