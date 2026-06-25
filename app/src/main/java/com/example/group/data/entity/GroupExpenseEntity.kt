package com.example.group.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_expenses",
    indices = [Index(value = ["groupId"])]
)
data class GroupExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val title: String,
    val amount: Double,
    val currency: String,
    val category: String,
    val date: Long,
    val notes: String?,
    val receiptBytes: ByteArray?,
    val createdDate: Long,
    val updatedDate: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupExpenseEntity

        if (id != other.id) return false
        if (groupId != other.groupId) return false
        if (title != other.title) return false
        if (amount != other.amount) return false
        if (currency != other.currency) return false
        if (category != other.category) return false
        if (date != other.date) return false
        if (notes != other.notes) return false
        if (receiptBytes != null) {
            if (other.receiptBytes == null) return false
            if (!receiptBytes.contentEquals(other.receiptBytes)) return false
        } else if (other.receiptBytes != null) return false
        if (createdDate != other.createdDate) return false
        if (updatedDate != other.updatedDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + groupId
        result = 31 * result + title.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + currency.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + (receiptBytes?.contentHashCode() ?: 0)
        result = 31 * result + createdDate.hashCode()
        result = 31 * result + updatedDate.hashCode()
        return result
    }
}
