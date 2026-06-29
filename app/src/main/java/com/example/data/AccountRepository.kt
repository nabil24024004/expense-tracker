package com.example.data

import kotlinx.coroutines.flow.Flow

class AccountRepository(private val accountDao: AccountDao) {
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()

    suspend fun getAllSync(): List<Account> = accountDao.getAllAccountsSync()
    suspend fun getById(id: Int): Account? = accountDao.getAccountById(id)
    suspend fun insert(account: Account): Long = accountDao.insertAccount(account)
    suspend fun update(account: Account) = accountDao.updateAccount(account)
    suspend fun delete(account: Account) = accountDao.deleteAccount(account)
}
