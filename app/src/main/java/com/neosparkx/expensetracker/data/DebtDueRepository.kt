package com.neosparkx.expensetracker.data

import kotlinx.coroutines.flow.Flow

class DebtDueRepository(private val debtDueDao: DebtDueDao) {
    val allDebtsDues: Flow<List<DebtDue>> = debtDueDao.getAllDebtsDues()

    suspend fun getAllSync(): List<DebtDue> = debtDueDao.getAllDebtsDuesSync()
    suspend fun insert(debtDue: DebtDue): Long = debtDueDao.insertDebtDue(debtDue)
    suspend fun update(debtDue: DebtDue) = debtDueDao.updateDebtDue(debtDue)
    suspend fun delete(id: Int) = debtDueDao.deleteDebtDueById(id)
    suspend fun deleteAll() = debtDueDao.deleteAll()
}

