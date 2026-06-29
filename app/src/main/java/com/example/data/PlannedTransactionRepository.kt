package com.example.data

import kotlinx.coroutines.flow.Flow

class PlannedTransactionRepository(private val plannedDao: PlannedTransactionDao) {
    val allPlanned: Flow<List<PlannedTransaction>> = plannedDao.getAllPlanned()
    val activePlanned: Flow<List<PlannedTransaction>> = plannedDao.getActivePlanned()

    suspend fun insert(planned: PlannedTransaction): Long = plannedDao.insertPlanned(planned)
    suspend fun update(planned: PlannedTransaction) = plannedDao.updatePlanned(planned)
    suspend fun delete(planned: PlannedTransaction) = plannedDao.deletePlanned(planned)
}
