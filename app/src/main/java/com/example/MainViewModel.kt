package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Expense
import com.example.data.ExpenseRepository
import com.example.data.DebtDue
import com.example.data.DebtDueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExpenseRepository
    private val debtDueRepository: DebtDueRepository
    private val prefs = application.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)
    private val database: AppDatabase

    val isAuthenticated = MutableStateFlow(false)
    val isFirstLaunch = MutableStateFlow(prefs.getBoolean("first_launch", true))
    val userName = MutableStateFlow(prefs.getString("user_name", "Azwad") ?: "Azwad")
    val profileImageUri = MutableStateFlow(prefs.getString("profile_image_uri", null))
    val budgetLimit = MutableStateFlow(prefs.getFloat("budget_limit", 6000.0f).toDouble())
    val biometricsEnabled = MutableStateFlow(prefs.getBoolean("biometrics_enabled", false))
    
    val themeSelection = MutableStateFlow(prefs.getString("theme_selection", "Ocean Blue") ?: "Ocean Blue")
    val isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val notificationsLastViewedTime = MutableStateFlow(prefs.getLong("notifications_last_viewed", 0L))

    init {
        database = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(database.expenseDao())
        debtDueRepository = DebtDueRepository(database.debtDueDao())
        
        if (!biometricsEnabled.value) {
            isAuthenticated.value = true
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
        
        if (enableBiometrics) {
            isAuthenticated.value = false
        } else {
            isAuthenticated.value = true
        }
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

    fun updateBiometricsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometrics_enabled", enabled).apply()
        biometricsEnabled.value = enabled
    }

    fun updateTheme(themeName: String) {
        prefs.edit().putString("theme_selection", themeName).apply()
        themeSelection.value = themeName
    }

    fun updateDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        isDarkMode.value = enabled
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

    fun addExpense(
        amount: Double,
        description: String,
        category: String,
        date: Long = System.currentTimeMillis(),
        imageBytes: ByteArray? = null,
        foodDetails: String? = null
    ) {
        viewModelScope.launch {
            val newExpense = Expense(
                amount = amount,
                description = description,
                category = category,
                date = date,
                imageBytes = imageBytes,
                foodDetails = foodDetails
            )
            
            val currentExpenses = expenses.value
            val oldTotal = currentExpenses.sumOf { it.amount }
            val newTotal = oldTotal + amount
            val limit = budgetLimit.value

            repository.insert(newExpense)

            if (newTotal > limit && oldTotal <= limit) {
                val formattedLimit = String.format(java.util.Locale.US, "%,.2f", limit)
                val formattedNewTotal = String.format(java.util.Locale.US, "%,.2f", newTotal)
                NotificationHelper.triggerLiveNotification(
                    getApplication(),
                    "Budget Overrun",
                    "Alert: You have exceeded your budget limit of ৳$formattedLimit! Total spent is now ৳$formattedNewTotal."
                )
            } else if (newTotal >= limit * 0.8 && oldTotal < limit * 0.8 && newTotal <= limit) {
                val formattedLimit = String.format(java.util.Locale.US, "%,.2f", limit)
                val formattedNewTotal = String.format(java.util.Locale.US, "%,.2f", newTotal)
                NotificationHelper.triggerLiveNotification(
                    getApplication(),
                    "High Spending Alert",
                    "Warning: You have used over 80% of your budget limit (৳$formattedNewTotal of ৳$formattedLimit used)."
                )
            }
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun importExpenses(expensesToImport: List<Expense>, onComplete: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                var importedCount = 0
                expensesToImport.forEach { expense ->
                    repository.insert(expense)
                    importedCount++
                }
                
                if (importedCount > 0) {
                    NotificationHelper.triggerLiveNotification(
                        getApplication(),
                        "Import Complete",
                        "Successfully imported $importedCount transaction logs."
                    )
                    
                    val currentExpenses = expenses.value
                    val newTotal = currentExpenses.sumOf { it.amount }
                    val limit = budgetLimit.value
                    if (newTotal > limit) {
                        val formattedLimit = String.format(java.util.Locale.US, "%,.2f", limit)
                        val formattedNewTotal = String.format(java.util.Locale.US, "%,.2f", newTotal)
                        NotificationHelper.triggerLiveNotification(
                            getApplication(),
                            "Budget Overrun",
                            "Alert: You have exceeded your budget limit of ৳$formattedLimit! Total spent is now ৳$formattedNewTotal."
                        )
                    }
                }
                onComplete(true, importedCount)
            } catch (e: Exception) {
                onComplete(false, 0)
            }
        }
    }

    fun addDebtDue(personName: String, amount: Double, description: String, type: String, dueDate: Long?) {
        viewModelScope.launch {
            val date = System.currentTimeMillis()
            val newDebtDue = DebtDue(
                personName = personName,
                amount = amount,
                description = description,
                date = date,
                dueDate = dueDate,
                type = type
            )
            debtDueRepository.insert(newDebtDue)
        }
    }

    fun settleDebtDue(debtDue: DebtDue, logAsExpense: Boolean) {
        viewModelScope.launch {
            val updated = debtDue.copy(isCleared = true)
            debtDueRepository.update(updated)

            if (logAsExpense && debtDue.type == "DEBT") {
                addExpense(
                    amount = debtDue.amount,
                    description = "Repaid: ${debtDue.personName} (${debtDue.description})",
                    category = "Debt Repayment"
                )
            }
        }
    }

    fun deleteDebtDue(id: Int) {
        viewModelScope.launch {
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
            themeSelection.value = "Blue/Black"
            isDarkMode.value = false
            isAuthenticated.value = false
            notificationsLastViewedTime.value = 0L
        }
    }
}

