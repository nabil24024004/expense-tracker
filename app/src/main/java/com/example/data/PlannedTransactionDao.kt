package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedTransactionDao {
    @Query("SELECT * FROM planned_transactions ORDER BY nextDueDate ASC")
    fun getAllPlanned(): Flow<List<PlannedTransaction>>

    @Query("SELECT * FROM planned_transactions WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getActivePlanned(): Flow<List<PlannedTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanned(planned: PlannedTransaction): Long

    @Update
    suspend fun updatePlanned(planned: PlannedTransaction)

    @Delete
    suspend fun deletePlanned(planned: PlannedTransaction)
}
