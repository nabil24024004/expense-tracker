package com.neosparkx.expensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDueDao {
    @Query("SELECT * FROM debts_dues ORDER BY date DESC")
    fun getAllDebtsDues(): Flow<List<DebtDue>>

    @Query("SELECT * FROM debts_dues")
    suspend fun getAllDebtsDuesSync(): List<DebtDue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtDue(debtDue: DebtDue): Long

    @Update
    suspend fun updateDebtDue(debtDue: DebtDue)
    
    @Query("DELETE FROM debts_dues WHERE id = :id")
    suspend fun deleteDebtDueById(id: Int)

    @Query("DELETE FROM debts_dues")
    suspend fun deleteAll()
}

