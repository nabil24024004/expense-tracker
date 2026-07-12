package com.neosparkx.expensetracker

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neosparkx.expensetracker.data.AppDatabase
import com.neosparkx.expensetracker.data.Expense
import com.neosparkx.expensetracker.data.ExpenseRepository
import com.neosparkx.expensetracker.data.DebtDue
import com.neosparkx.expensetracker.data.DebtDueRepository
import com.neosparkx.expensetracker.data.BudgetPeriodHelper
import com.neosparkx.expensetracker.data.Account
import com.neosparkx.expensetracker.data.AccountRepository
import com.neosparkx.expensetracker.data.PlannedTransaction
import com.neosparkx.expensetracker.data.PlannedTransactionRepository
import com.neosparkx.expensetracker.data.ExcelHelper
import java.util.Locale
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExpenseRepository
    private val debtDueRepository: DebtDueRepository
    private val accountRepository: AccountRepository
    private val plannedTransactionRepository: PlannedTransactionRepository
    private val prefs = application.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)
    private val database: AppDatabase

    val isAuthenticated = MutableStateFlow(false)
    val isFirstLaunch = MutableStateFlow(prefs.getBoolean("first_launch", true))
    val userName = MutableStateFlow(prefs.getString("user_name", "Azwad") ?: "Azwad")
    val profileImageUri = MutableStateFlow(prefs.getString("profile_image_uri", null))
    val budgetLimit = MutableStateFlow(prefs.getFloat("budget_limit", 6000.0f).toDouble())
    val biometricsEnabled = MutableStateFlow(prefs.getBoolean("biometrics_enabled", false))
    val hideBalance = MutableStateFlow(prefs.getBoolean("hide_balance", false))
    val hideIncome = MutableStateFlow(prefs.getBoolean("hide_income", false))
    
    val themeSelection = MutableStateFlow(prefs.getString("theme_selection", "Light") ?: "Light")
    val notificationsLastViewedTime = MutableStateFlow(prefs.getLong("notifications_last_viewed", 0L))

    val budgetPeriodType = MutableStateFlow(prefs.getString("budget_period_type", "monthly") ?: "monthly")
    val budgetCustomStartDate = MutableStateFlow(prefs.getLong("budget_custom_start_date", 0L))
    val budgetCustomEndDate = MutableStateFlow(prefs.getLong("budget_custom_end_date", 0L))

    init {
        database = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(database.expenseDao())
        debtDueRepository = DebtDueRepository(database.debtDueDao())
        accountRepository = AccountRepository(database.accountDao())
        plannedTransactionRepository = PlannedTransactionRepository(database.plannedTransactionDao())
        
        if (!biometricsEnabled.value) {
            isAuthenticated.value = true
        }

        viewModelScope.launch(Dispatchers.IO) {
            // --- Deduplicate Cash accounts (fixes the dual-Cash bug) ---
            val allAccounts = accountRepository.getAllSync()
            val cashAccounts = allAccounts.filter { it.name.trim().equals("Cash", ignoreCase = true) }
            if (cashAccounts.size > 1) {
                // Keep the one with id=1 if present, otherwise keep the first
                val keeper = cashAccounts.firstOrNull { it.id == 1 } ?: cashAccounts.first()
                val duplicates = cashAccounts.filter { it.id != keeper.id }
                duplicates.forEach { dup ->
                    // Re-point any transactions that referenced the duplicate account
                    database.expenseDao().reassignAccountId(dup.id, keeper.id)
                    accountRepository.delete(dup)
                }
            }

            // --- Ensure the canonical Cash account (id=1) exists ---
            var cashAccount = accountRepository.getById(1)
            if (cashAccount == null) {
                val newCash = Account(
                    id = 1,
                    name = "Cash",
                    balance = 0.0,
                    colorHex = "#EA3B35",
                    icon = "wallet",
                    currency = "৳",
                    includeInBalance = true,
                    displayOrder = 0
                )
                accountRepository.insert(newCash)
                cashAccount = accountRepository.getById(1)
            }

            // --- One-time balance sync from transaction history (migration guard) ---
            if (cashAccount != null) {
                val prefs = application.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)
                val isSynced = prefs.getBoolean("cash_balance_synced_v3", false)
                if (!isSynced) {
                    val allExpenses = repository.getAllExpensesSync()
                    val cashTxns = allExpenses.filter { it.accountId == 1 || it.accountId == null }
                    val netBalance = cashTxns.sumOf {
                        if (it.type == "INCOME") it.amount else -it.amount
                    }
                    accountRepository.update(cashAccount.copy(balance = netBalance))
                    prefs.edit().putBoolean("cash_balance_synced_v3", true).apply()
                }
            }

            // Auto-deposit scheduled incomes
            plannedTransactionRepository.activePlanned.collect { plannedList ->
                val now = System.currentTimeMillis()
                plannedList.forEach { planned ->
                    if (planned.type == "INCOME" && planned.nextDueDate <= now) {
                        executePlannedTransaction(planned)
                    }
                }
            }
        }
    }

    val expenses: StateFlow<List<Expense>> = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val debtsDues: StateFlow<List<DebtDue>> = debtDueRepository.allDebtsDues.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val accounts: StateFlow<List<Account>> = accountRepository.allAccounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val plannedTransactions: StateFlow<List<PlannedTransaction>> = plannedTransactionRepository.allPlanned.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val analyticsSnapshot: StateFlow<AnalyticsSnapshot?> = repository.allExpenses
        .map { expenses ->
            val expenseOnly = expenses.filter { it.type == "EXPENSE" }
            val currentTime = System.currentTimeMillis()
            
            // 4 weeks calculations
            val last4Weeks = mutableListOf<Pair<String, Double>>()
            for (w in 3 downTo 0) {
                val weekStart = Calendar.getInstance().apply {
                    timeInMillis = currentTime
                    add(Calendar.DAY_OF_YEAR, -(w * 7 + 6))
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val weekEnd = Calendar.getInstance().apply {
                    timeInMillis = currentTime
                    add(Calendar.DAY_OF_YEAR, -(w * 7))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val label = "W${4 - w}"
                val sum = expenseOnly
                    .filter { it.date in weekStart.timeInMillis..weekEnd.timeInMillis }
                    .sumOf { it.amount }
                last4Weeks.add(label to sum)
            }
            
            // Total spent
            val total = expenseOnly.sumOf { it.amount }
            
            // Category data
            val categories = expenseOnly
                .groupBy { it.category }
                .map { (cat, list) -> cat to list.sumOf { it.amount } }
                .sortedByDescending { it.second }
                
            // Days since first
            val days = if (expenseOnly.isEmpty()) 1
            else {
                val earliest = expenseOnly.minOf { it.date }
                val diff = currentTime - earliest
                maxOf(1, (diff / (1000L * 60 * 60 * 24)).toInt())
            }
            
            // 7 days calculations
            val last7Days = mutableListOf<Pair<String, Double>>()
            val dayFormat = SimpleDateFormat("EEE", Locale.US)
            for (i in 6 downTo 0) {
                val dayStart = Calendar.getInstance().apply {
                    timeInMillis = currentTime
                    add(Calendar.DAY_OF_YEAR, -i)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val dayEnd = Calendar.getInstance().apply {
                    timeInMillis = dayStart.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                val label = dayFormat.format(Date(dayStart.timeInMillis))
                val sum = expenseOnly
                    .filter { it.date in dayStart.timeInMillis..dayEnd.timeInMillis }
                    .sumOf { it.amount }
                last7Days.add(label to sum)
            }
            
            AnalyticsSnapshot(
                last7DaysData = last7Days,
                last4WeeksData = last4Weeks,
                totalSpent = total,
                categoryData = categories,
                daysSinceFirst = days,
                dailyAvg = total / days
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun completeOnboarding(name: String, budget: Double, enableBiometrics: Boolean) {
        prefs.edit().apply {
            putBoolean("first_launch", false)
            putString("user_name", name)
            putFloat("budget_limit", budget.toFloat())
            putBoolean("biometrics_enabled", enableBiometrics)
            apply()
        }
        isFirstLaunch.value = false
        userName.value = name
        budgetLimit.value = budget
        biometricsEnabled.value = enableBiometrics
        
        isAuthenticated.value = !enableBiometrics
    }

    fun updateUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
        userName.value = name
    }

    fun updateProfileImageUri(uri: String?) {
        prefs.edit().putString("profile_image_uri", uri).apply()
        profileImageUri.value = uri
    }

    fun updateBudgetLimit(limit: Double) {
        prefs.edit().putFloat("budget_limit", limit.toFloat()).apply()
        budgetLimit.value = limit
    }

    fun updateBudgetPeriod(periodType: String, customStart: Long = 0L, customEnd: Long = 0L) {
        prefs.edit().apply {
            putString("budget_period_type", periodType)
            putLong("budget_custom_start_date", customStart)
            putLong("budget_custom_end_date", customEnd)
            apply()
        }
        budgetPeriodType.value = periodType
        budgetCustomStartDate.value = customStart
        budgetCustomEndDate.value = customEnd
    }

    fun updateBiometricsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometrics_enabled", enabled).apply()
        biometricsEnabled.value = enabled
    }

    fun updateHideBalance(hide: Boolean) {
        prefs.edit().putBoolean("hide_balance", hide).apply()
        hideBalance.value = hide
    }

    fun updateHideIncome(hide: Boolean) {
        prefs.edit().putBoolean("hide_income", hide).apply()
        hideIncome.value = hide
    }

    fun updateTheme(themeName: String) {
        prefs.edit().putString("theme_selection", themeName).apply()
        themeSelection.value = themeName
    }

    fun authenticate() {
        isAuthenticated.value = true
    }

    fun logout() {
        isAuthenticated.value = false
    }

    fun markNotificationsAsRead() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong("notifications_last_viewed", now).apply()
        notificationsLastViewedTime.value = now
    }

    // Account CRUD
    fun addAccount(name: String, startingBalance: Double, colorHex: String, icon: String, currency: String = "৳", includeInBalance: Boolean = true) {
        viewModelScope.launch {
            val maxOrder = accounts.value.maxOfOrNull { it.displayOrder } ?: 0
            val newAccount = Account(
                name = name,
                balance = startingBalance,
                colorHex = colorHex,
                icon = icon,
                currency = currency,
                includeInBalance = includeInBalance,
                displayOrder = maxOrder + 1
            )
            accountRepository.insert(newAccount)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            accountRepository.update(account)
        }
    }

    fun deleteAccount(account: Account) {
        if (account.id == 1) return // Prevent deleting the default Cash account
        viewModelScope.launch {
            accountRepository.delete(account)
        }
    }

    fun updateAccountsOrder(reorderedList: List<Account>) {
        viewModelScope.launch {
            reorderedList.forEachIndexed { index, account ->
                accountRepository.update(account.copy(displayOrder = index))
            }
        }
    }

    // Expense & Transfer Logging
    fun addExpense(
        amount: Double,
        description: String,
        category: String,
        type: String = "EXPENSE",
        accountId: Int? = null,
        toAccountId: Int? = null,
        tags: String = ""
    ) {
        viewModelScope.launch {
            val date = System.currentTimeMillis()
            val resolvedAccountId = accountId ?: accounts.value.firstOrNull()?.id ?: 1
            val newExpense = Expense(
                amount = amount,
                description = description,
                category = category,
                date = date,
                type = type,
                accountId = resolvedAccountId,
                toAccountId = toAccountId,
                tags = tags
            )
            
            val periodType = budgetPeriodType.value
            val customStart = budgetCustomStartDate.value
            val customEnd = budgetCustomEndDate.value
            val range = BudgetPeriodHelper.getPeriodRange(periodType, customStart, customEnd)
            
            val currentExpenses = expenses.value
            val oldPeriodTotal = currentExpenses.filter { it.type == "EXPENSE" && it.date in range.first..range.second }.sumOf { it.amount }
            val newPeriodTotal = if (type == "EXPENSE") oldPeriodTotal + amount else oldPeriodTotal
            val limit = budgetLimit.value
 
            repository.insert(newExpense)

            // Adjust account balance in database
            if (type == "TRANSFER" && toAccountId != null) {
                // Subtract from sender, add to receiver
                accountRepository.getById(resolvedAccountId)?.let { fromAcc ->
                    accountRepository.update(fromAcc.copy(balance = fromAcc.balance - amount))
                }
                accountRepository.getById(toAccountId)?.let { toAcc ->
                    accountRepository.update(toAcc.copy(balance = toAcc.balance + amount))
                }
            } else if (type == "INCOME") {
                // Add to account
                accountRepository.getById(resolvedAccountId)?.let { acc ->
                    accountRepository.update(acc.copy(balance = acc.balance + amount))
                }
            } else {
                // EXPENSE
                accountRepository.getById(resolvedAccountId)?.let { acc ->
                    accountRepository.update(acc.copy(balance = acc.balance - amount))
                }
            }
 
            if (type == "EXPENSE") {
                checkAndFireBudgetNotification(periodType, oldPeriodTotal, newPeriodTotal, limit)
            }
        }
    }

    /** Fires budget overrun / 80%-warning notifications when a new expense is logged. */
    private fun checkAndFireBudgetNotification(
        periodType: String,
        oldTotal: Double,
        newTotal: Double,
        limit: Double
    ) {
        if (newTotal > limit && oldTotal <= limit) {
            val formattedLimit = String.format(Locale.US, "%,.2f", limit)
            val formattedNewTotal = String.format(Locale.US, "%,.2f", newTotal)
            NotificationHelper.triggerLiveNotification(
                getApplication(),
                "Budget Overrun",
                "Alert: You have exceeded your $periodType budget limit of ৳$formattedLimit! Total spent is now ৳$formattedNewTotal."
            )
        } else if (newTotal >= limit * 0.8 && oldTotal < limit * 0.8 && newTotal <= limit) {
            val formattedLimit = String.format(Locale.US, "%,.2f", limit)
            val formattedNewTotal = String.format(Locale.US, "%,.2f", newTotal)
            NotificationHelper.triggerLiveNotification(
                getApplication(),
                "High Spending Alert",
                "Warning: You have used over 80% of your $periodType budget limit (৳$formattedNewTotal of ৳$formattedLimit used)."
            )
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            // Retrieve expense to adjust back its amount
            val expenseList = expenses.value
            val exp = expenseList.find { it.id == id }
            if (exp != null) {
                val accId = exp.accountId ?: 1
                if (exp.type == "TRANSFER" && exp.toAccountId != null) {
                    accountRepository.getById(accId)?.let { fromAcc ->
                        accountRepository.update(fromAcc.copy(balance = fromAcc.balance + exp.amount))
                    }
                    accountRepository.getById(exp.toAccountId)?.let { toAcc ->
                        accountRepository.update(toAcc.copy(balance = toAcc.balance - exp.amount))
                    }
                } else if (exp.type == "INCOME") {
                    accountRepository.getById(accId)?.let { acc ->
                        accountRepository.update(acc.copy(balance = acc.balance - exp.amount))
                    }
                } else {
                    // EXPENSE
                    accountRepository.getById(accId)?.let { acc ->
                        accountRepository.update(acc.copy(balance = acc.balance + exp.amount))
                    }
                }
            }
            repository.delete(id)
        }
    }

    fun importAllData(importData: ExcelHelper.AppImportData, onComplete: (Boolean, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var importedCount = 0
                val accountMap = mutableMapOf<String, Int>()

                // 1. Resolve and insert Accounts
                val existingAccounts = accountRepository.getAllSync()
                existingAccounts.forEach { acc ->
                    accountMap[acc.name.trim().lowercase(Locale.US)] = acc.id
                }

                importData.accounts.forEach { acc ->
                    val key = acc.name.trim().lowercase(Locale.US)
                    if (!accountMap.containsKey(key)) {
                        val newId = accountRepository.insert(acc.copy(id = 0))
                        accountMap[key] = newId.toInt()
                        importedCount++
                    } else {
                        val existingId = accountMap[key]!!
                        accountRepository.getById(existingId)?.let { existingAcc ->
                            accountRepository.update(existingAcc.copy(
                                balance = acc.balance,
                                colorHex = acc.colorHex,
                                icon = acc.icon,
                                currency = acc.currency,
                                includeInBalance = acc.includeInBalance,
                                displayOrder = acc.displayOrder
                            ))
                        }
                    }
                }

                val defaultAccountId = accountMap["cash"] ?: accountMap.values.firstOrNull() ?: 1

                // 2. Resolve and insert Transactions (Expenses)
                importData.rawExpenses.forEach { rawExp ->
                    val accId = accountMap[rawExp.accountName.trim().lowercase(Locale.US)] ?: defaultAccountId
                    val toAccId = if (rawExp.toAccountName.isNotEmpty()) {
                        accountMap[rawExp.toAccountName.trim().lowercase(Locale.US)]
                    } else {
                        null
                    }

                    val expense = Expense(
                        amount = rawExp.amount,
                        description = rawExp.description,
                        category = rawExp.category,
                        date = rawExp.date,
                        type = rawExp.type,
                        accountId = accId,
                        toAccountId = toAccId,
                        tags = rawExp.tags
                    )
                    repository.insert(expense)

                    // Adjust balance only if accounts sheet is NOT provided (legacy mode fallback)
                    if (importData.accounts.isEmpty()) {
                        if (expense.type == "INCOME") {
                            accountRepository.getById(accId)?.let { acc ->
                                accountRepository.update(acc.copy(balance = acc.balance + expense.amount))
                            }
                        } else if (expense.type == "EXPENSE") {
                            accountRepository.getById(accId)?.let { acc ->
                                accountRepository.update(acc.copy(balance = acc.balance - expense.amount))
                            }
                        }
                    }
                    importedCount++
                }

                // 3. Resolve and insert Debts & Receivables
                importData.debtsDues.forEach { debt ->
                    debtDueRepository.insert(debt.copy(id = 0))
                    importedCount++
                }

                // 4. Resolve and insert Planned Transactions
                importData.rawPlanned.forEach { rawPlanned ->
                    val accId = accountMap[rawPlanned.accountName.trim().lowercase(Locale.US)] ?: defaultAccountId
                    val planned = PlannedTransaction(
                        title = rawPlanned.title,
                        amount = rawPlanned.amount,
                        category = rawPlanned.category,
                        type = rawPlanned.type,
                        accountId = accId,
                        startDate = rawPlanned.startDate,
                        intervalType = rawPlanned.intervalType,
                        intervalN = rawPlanned.intervalN,
                        oneTime = rawPlanned.oneTime,
                        nextDueDate = rawPlanned.nextDueDate,
                        isActive = rawPlanned.isActive,
                        description = rawPlanned.description
                    )
                    plannedTransactionRepository.insert(planned)
                    importedCount++
                }

                withContext(Dispatchers.Main) {
                    onComplete(true, importedCount)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false, 0)
                }
            }
        }
    }

    // Planned Transactions CRUD & Scheduling
    fun addPlannedTransaction(
        title: String,
        amount: Double,
        category: String,
        type: String,
        accountId: Int,
        startDate: Long,
        intervalType: String,
        intervalN: Int,
        oneTime: Boolean,
        description: String = ""
    ) {
        viewModelScope.launch {
            val pt = PlannedTransaction(
                title = title,
                amount = amount,
                category = category,
                type = type,
                accountId = accountId,
                startDate = startDate,
                intervalType = intervalType,
                intervalN = intervalN,
                oneTime = oneTime,
                nextDueDate = startDate,
                isActive = true,
                description = description
            )
            plannedTransactionRepository.insert(pt)
        }
    }

    fun updatePlannedTransaction(planned: PlannedTransaction) {
        viewModelScope.launch {
            plannedTransactionRepository.update(planned)
        }
    }

    fun deletePlannedTransaction(planned: PlannedTransaction) {
        viewModelScope.launch {
            plannedTransactionRepository.delete(planned)
        }
    }

    fun executePlannedTransaction(planned: PlannedTransaction) {
        viewModelScope.launch {
            // Log as real transaction
            addExpense(
                amount = planned.amount,
                description = planned.title,
                category = planned.category,
                type = planned.type,
                accountId = planned.accountId,
                tags = "planned"
            )

            // Advance due date
            if (planned.oneTime) {
                plannedTransactionRepository.update(planned.copy(isActive = false))
            } else {
                val nextDue = calculateNextDueDate(planned.nextDueDate, planned.intervalType, planned.intervalN)
                plannedTransactionRepository.update(planned.copy(nextDueDate = nextDue))
            }
        }
    }

    fun skipPlannedTransaction(planned: PlannedTransaction) {
        viewModelScope.launch {
            if (planned.oneTime) {
                plannedTransactionRepository.update(planned.copy(isActive = false))
            } else {
                val nextDue = calculateNextDueDate(planned.nextDueDate, planned.intervalType, planned.intervalN)
                plannedTransactionRepository.update(planned.copy(nextDueDate = nextDue))
            }
        }
    }

    private fun calculateNextDueDate(currentNextDue: Long, intervalType: String, intervalN: Int): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = currentNextDue
        }
        when (intervalType) {
            "DAY" -> cal.add(java.util.Calendar.DAY_OF_YEAR, intervalN)
            "WEEK" -> cal.add(java.util.Calendar.WEEK_OF_YEAR, intervalN)
            "MONTH" -> cal.add(java.util.Calendar.MONTH, intervalN)
            "YEAR" -> cal.add(java.util.Calendar.YEAR, intervalN)
            else -> cal.add(java.util.Calendar.MONTH, intervalN)
        }
        return cal.timeInMillis
    }

    // Debts & Dues settlement
    fun addDebtDue(
        personName: String,
        amount: Double,
        description: String,
        type: String,
        dueDate: Long?,
        accountId: Int?,
        addToAccountNow: Boolean = false
    ) {
        viewModelScope.launch {
            val date = System.currentTimeMillis()
            val newDebtDue = DebtDue(
                personName = personName,
                amount = amount,
                description = description,
                date = date,
                dueDate = dueDate,
                type = type,
                accountId = accountId
            )
            debtDueRepository.insert(newDebtDue)

            // Optionally log a real transaction so the account balance is updated
            // through the proper expense/income history (not a raw balance mutation)
            if (addToAccountNow && accountId != null) {
                if (type == "DEBT") {
                    // I borrowed money — it's an income into my account
                    addExpense(
                        amount = amount,
                        description = "Debt received from: $personName ($description)",
                        category = "Debt Received",
                        type = "INCOME",
                        accountId = accountId
                    )
                } else {
                    // I lent money — it's an expense out of my account
                    addExpense(
                        amount = amount,
                        description = "Lent to: $personName ($description)",
                        category = "Debt Lent",
                        type = "EXPENSE",
                        accountId = accountId
                    )
                }
            }
        }
    }

    fun settleDebtDuePartial(debtDue: DebtDue, paidAmount: Double, logAsTransaction: Boolean, accountId: Int = 1) {
        viewModelScope.launch {
            if (paidAmount >= debtDue.amount) {
                val updated = debtDue.copy(isCleared = true)
                debtDueRepository.update(updated)
            } else {
                val updated = debtDue.copy(amount = debtDue.amount - paidAmount)
                debtDueRepository.update(updated)
            }

            if (logAsTransaction) {
                if (debtDue.type == "DEBT") {
                    addExpense(
                        amount = paidAmount,
                        description = "Repaid: ${debtDue.personName} (${debtDue.description})",
                        category = "Debt Repayment",
                        type = "EXPENSE",
                        accountId = accountId
                    )
                } else {
                    addExpense(
                        amount = paidAmount,
                        description = "Collected from: ${debtDue.personName} (${debtDue.description})",
                        category = "Debt Received",
                        type = "INCOME",
                        accountId = accountId
                    )
                }
            }
        }
    }

    fun deleteDebtDue(id: Int) {
        viewModelScope.launch {
            // No balance reversal needed: account balances are only affected via
            // logged income/expense transactions (at creation with addToAccountNow=true,
            // or at settlement). Those individual transactions can be deleted separately.
            debtDueRepository.delete(id)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.clearAllTables()
            }
            prefs.edit().clear().apply()
            isFirstLaunch.value = true
            userName.value = "Azwad"
            profileImageUri.value = null
            budgetLimit.value = 6000.0
            biometricsEnabled.value = false
            themeSelection.value = "Light"
            isAuthenticated.value = false
            notificationsLastViewedTime.value = 0L
            budgetPeriodType.value = "monthly"
            budgetCustomStartDate.value = 0L
            budgetCustomEndDate.value = 0L
        }
    }
}

data class AnalyticsSnapshot(
    val last7DaysData: List<Pair<String, Double>>,
    val last4WeeksData: List<Pair<String, Double>>,
    val totalSpent: Double,
    val categoryData: List<Pair<String, Double>>,
    val daysSinceFirst: Int,
    val dailyAvg: Double
)

