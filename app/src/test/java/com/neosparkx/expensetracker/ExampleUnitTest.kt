package com.neosparkx.expensetracker

import com.neosparkx.expensetracker.data.Account
import com.neosparkx.expensetracker.data.Expense
import com.neosparkx.expensetracker.data.DebtDue
import com.neosparkx.expensetracker.data.PlannedTransaction
import com.neosparkx.expensetracker.data.ExcelHelper
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ExampleUnitTest {
  @Test
  fun testExcelExportAndImportAllData() {
    val accounts = listOf(
      Account(
        id = 1,
        name = "Cash",
        balance = 500.0,
        colorHex = "#EA3B35",
        icon = "wallet",
        currency = "৳",
        includeInBalance = true,
        displayOrder = 0
      )
    )

    val expenses = listOf(
      Expense(
        id = 1,
        amount = 120.50,
        description = "Lunch",
        category = "Food",
        date = System.currentTimeMillis(),
        accountId = 1,
        tags = "lunch,food"
      ),
      Expense(
        id = 2,
        amount = 45.00,
        description = "Bus fare",
        category = "Transport",
        date = System.currentTimeMillis(),
        accountId = 1,
        tags = "transport"
      )
    )

    val debtsDues = listOf(
      DebtDue(
        id = 1,
        personName = "Alice",
        amount = 100.0,
        description = "Borrowed for coffee",
        date = System.currentTimeMillis(),
        type = "DEBT",
        isCleared = false,
        accountId = 1
      )
    )

    val plannedTransactions = listOf(
      PlannedTransaction(
        id = 1,
        title = "Internet Subscription",
        amount = 1500.0,
        category = "Bills",
        type = "EXPENSE",
        accountId = 1,
        startDate = System.currentTimeMillis(),
        intervalType = "MONTH",
        intervalN = 1,
        oneTime = false,
        nextDueDate = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
        isActive = true,
        description = "Monthly wifi bill"
      )
    )

    val outputStream = ByteArrayOutputStream()
    ExcelHelper.exportAllData(outputStream, accounts, expenses, debtsDues, plannedTransactions)

    val byteArray = outputStream.toByteArray()
    assertTrue(byteArray.isNotEmpty())

    val inputStream = ByteArrayInputStream(byteArray)
    val importedData = ExcelHelper.importAllData(inputStream)

    // Verify imported accounts
    assertEquals(1, importedData.accounts.size)
    assertEquals("Cash", importedData.accounts[0].name)
    assertEquals(500.0, importedData.accounts[0].balance, 0.01)

    // Verify imported expenses
    assertEquals(2, importedData.rawExpenses.size)
    assertEquals(120.50, importedData.rawExpenses[0].amount, 0.01)
    assertEquals("Lunch", importedData.rawExpenses[0].description)
    assertEquals("Food", importedData.rawExpenses[0].category)
    assertEquals("lunch,food", importedData.rawExpenses[0].tags)

    assertEquals(45.00, importedData.rawExpenses[1].amount, 0.01)
    assertEquals("Bus fare", importedData.rawExpenses[1].description)
    assertEquals("Transport", importedData.rawExpenses[1].category)

    // Verify imported debts
    assertEquals(1, importedData.debtsDues.size)
    assertEquals("Alice", importedData.debtsDues[0].personName)
    assertEquals(100.0, importedData.debtsDues[0].amount, 0.01)

    // Verify imported planned transactions
    assertEquals(1, importedData.rawPlanned.size)
    assertEquals("Internet Subscription", importedData.rawPlanned[0].title)
    assertEquals(1500.0, importedData.rawPlanned[0].amount, 0.01)
  }
}
