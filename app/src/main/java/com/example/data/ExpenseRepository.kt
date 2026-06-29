package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun insert(expense: Expense): Long = expenseDao.insertExpense(expense)
    suspend fun update(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun delete(id: Int) = expenseDao.deleteExpenseById(id)
    suspend fun getUnsynced() = expenseDao.getUnsyncedExpenses()
    suspend fun getAllExpensesSync() = expenseDao.getAllExpensesSync()
}
