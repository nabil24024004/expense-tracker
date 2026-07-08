package com.example.data

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelHelper {
    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US)
    )

    // Intermediate structures for importing
    data class RawExpenseImport(
        val amount: Double,
        val description: String,
        val category: String,
        val date: Long,
        val type: String,
        val accountName: String,
        val toAccountName: String,
        val tags: String
    )

    data class RawPlannedImport(
        val title: String,
        val amount: Double,
        val category: String,
        val type: String,
        val accountName: String,
        val startDate: Long,
        val intervalType: String,
        val intervalN: Int,
        val oneTime: Boolean,
        val nextDueDate: Long,
        val isActive: Boolean,
        val description: String
    )

    data class AppImportData(
        val accounts: List<Account>,
        val rawExpenses: List<RawExpenseImport>,
        val debtsDues: List<DebtDue>,
        val rawPlanned: List<RawPlannedImport>
    )

    // New unified import method
    fun importAllData(inputStream: InputStream): AppImportData {
        val accounts = mutableListOf<Account>()
        val rawExpenses = mutableListOf<RawExpenseImport>()
        val debtsDues = mutableListOf<DebtDue>()
        val rawPlanned = mutableListOf<RawPlannedImport>()

        val bytes = inputStream.readBytes()
        val workbook = try {
            HSSFWorkbook(ByteArrayInputStream(bytes))
        } catch (e: Exception) {
            WorkbookFactory.create(ByteArrayInputStream(bytes))
        }

        val hasMultipleSheets = workbook.numberOfSheets > 1

        val accountsSheet = workbook.getSheet("Accounts")
        val expensesSheet = workbook.getSheet("Expenses") ?: if (!hasMultipleSheets) workbook.getSheetAt(0) else null
        val debtsSheet = workbook.getSheet("Debts & Receivables")
        val plannedSheet = workbook.getSheet("Planned Transactions")

        // 1. Parse Accounts Sheet
        if (accountsSheet != null) {
            val headerRow = accountsSheet.getRow(0)
            if (headerRow != null) {
                var nameIdx = -1
                var balIdx = -1
                var colorIdx = -1
                var iconIdx = -1
                var currIdx = -1
                var incIdx = -1
                var orderIdx = -1
                for (cell in headerRow) {
                    val txt = getCellValueAsString(cell).trim().lowercase(Locale.US)
                    when (txt) {
                        "name" -> nameIdx = cell.columnIndex
                        "balance" -> balIdx = cell.columnIndex
                        "colorhex" -> colorIdx = cell.columnIndex
                        "icon" -> iconIdx = cell.columnIndex
                        "currency" -> currIdx = cell.columnIndex
                        "includeinbalance" -> incIdx = cell.columnIndex
                        "displayorder" -> orderIdx = cell.columnIndex
                    }
                }

                if (nameIdx == -1) nameIdx = 0
                if (balIdx == -1) balIdx = 1
                if (colorIdx == -1) colorIdx = 2
                if (iconIdx == -1) iconIdx = 3

                val rowIterator = accountsSheet.iterator()
                if (rowIterator.hasNext()) rowIterator.next() // Skip header
                while (rowIterator.hasNext()) {
                    val row = rowIterator.next()
                    if (isRowEmpty(row)) continue

                    val name = getCellValueAsString(row.getCell(nameIdx)).trim()
                    if (name.isEmpty()) continue

                    val balance = getCellValueAsDouble(row.getCell(balIdx)) ?: 0.0
                    val colorHex = getCellValueAsString(row.getCell(colorIdx)).trim().let {
                        if (it.isEmpty()) "#EA3B35" else it
                    }
                    val icon = getCellValueAsString(row.getCell(iconIdx)).trim().let {
                        if (it.isEmpty()) "wallet" else it
                    }
                    val currency = if (currIdx != -1) getCellValueAsString(row.getCell(currIdx)).trim() else "৳"
                    val incVal = if (incIdx != -1) getCellValueAsString(row.getCell(incIdx)).trim().lowercase() else "true"
                    val includeInBalance = incVal == "true" || incVal == "1" || incVal == "1.0" || incVal == "yes"
                    val displayOrder = if (orderIdx != -1) getCellValueAsDouble(row.getCell(orderIdx))?.toInt() ?: 0 else 0

                    accounts.add(
                        Account(
                            name = name,
                            balance = balance,
                            colorHex = colorHex,
                            icon = icon,
                            currency = if (currency.isEmpty()) "৳" else currency,
                            includeInBalance = includeInBalance,
                            displayOrder = displayOrder
                        )
                    )
                }
            }
        }

        // 2. Parse Expenses Sheet
        if (expensesSheet != null) {
            val headerRow = expensesSheet.getRow(0)
            if (headerRow != null) {
                var dateIdx = -1
                var catIdx = -1
                var descIdx = -1
                var amtIdx = -1
                var typeIdx = -1
                var accIdx = -1
                var toAccIdx = -1
                var tagsIdx = -1

                for (cell in headerRow) {
                    val txt = getCellValueAsString(cell).trim().lowercase(Locale.US)
                    when (txt) {
                        "date" -> dateIdx = cell.columnIndex
                        "category" -> catIdx = cell.columnIndex
                        "description" -> descIdx = cell.columnIndex
                        "amount" -> amtIdx = cell.columnIndex
                        "type" -> typeIdx = cell.columnIndex
                        "account" -> accIdx = cell.columnIndex
                        "toaccount" -> toAccIdx = cell.columnIndex
                        "tags" -> tagsIdx = cell.columnIndex
                    }
                }

                if (dateIdx == -1) dateIdx = 0
                if (catIdx == -1) catIdx = 1
                if (descIdx == -1) descIdx = 2
                if (amtIdx == -1) amtIdx = 3

                val rowIterator = expensesSheet.iterator()
                if (rowIterator.hasNext()) rowIterator.next() // Skip header
                while (rowIterator.hasNext()) {
                    val row = rowIterator.next()
                    if (isRowEmpty(row)) continue

                    val amount = getCellValueAsDouble(row.getCell(amtIdx)) ?: 0.0
                    if (amount <= 0.0) continue

                    val dateMs = getCellValueAsDateMs(row.getCell(dateIdx)) ?: System.currentTimeMillis()
                    val category = getCellValueAsString(row.getCell(catIdx)).trim()
                    val description = getCellValueAsString(row.getCell(descIdx)).trim()
                    val rawType = if (typeIdx != -1) getCellValueAsString(row.getCell(typeIdx)).trim().uppercase(Locale.US) else "EXPENSE"
                    val type = if (rawType == "INCOME" || rawType == "EXPENSE" || rawType == "TRANSFER") rawType else "EXPENSE"
                    val accountName = if (accIdx != -1) getCellValueAsString(row.getCell(accIdx)).trim() else "Cash"
                    val toAccountName = if (toAccIdx != -1) getCellValueAsString(row.getCell(toAccIdx)).trim() else ""
                    val tags = if (tagsIdx != -1) getCellValueAsString(row.getCell(tagsIdx)).trim() else ""

                    rawExpenses.add(
                        RawExpenseImport(
                            amount = amount,
                            description = if (description.isEmpty()) "Imported Transaction" else description,
                            category = if (category.isEmpty()) "Other" else category,
                            date = dateMs,
                            type = type,
                            accountName = if (accountName.isEmpty()) "Cash" else accountName,
                            toAccountName = toAccountName,
                            tags = tags
                        )
                    )
                }
            }
        }

        // 3. Parse Debts Sheet
        if (debtsSheet != null) {
            val headerRow = debtsSheet.getRow(0)
            if (headerRow != null) {
                var dateIdx = -1
                var personIdx = -1
                var typeIdx = -1
                var descIdx = -1
                var amtIdx = -1
                var dueIdx = -1
                var statusIdx = -1

                for (cell in headerRow) {
                    val txt = getCellValueAsString(cell).trim().lowercase(Locale.US)
                    when (txt) {
                        "date" -> dateIdx = cell.columnIndex
                        "person" -> personIdx = cell.columnIndex
                        "type" -> typeIdx = cell.columnIndex
                        "description" -> descIdx = cell.columnIndex
                        "amount" -> amtIdx = cell.columnIndex
                        "due date" -> dueIdx = cell.columnIndex
                        "status" -> statusIdx = cell.columnIndex
                    }
                }

                if (dateIdx == -1) dateIdx = 0
                if (personIdx == -1) personIdx = 1
                if (typeIdx == -1) typeIdx = 2
                if (amtIdx == -1) amtIdx = 4

                val rowIterator = debtsSheet.iterator()
                if (rowIterator.hasNext()) rowIterator.next() // Skip header
                while (rowIterator.hasNext()) {
                    val row = rowIterator.next()
                    if (isRowEmpty(row)) continue

                    val personName = getCellValueAsString(row.getCell(personIdx)).trim()
                    if (personName.isEmpty()) continue

                    val amount = getCellValueAsDouble(row.getCell(amtIdx)) ?: 0.0
                    if (amount <= 0.0) continue

                    val dateMs = getCellValueAsDateMs(row.getCell(dateIdx)) ?: System.currentTimeMillis()
                    val description = getCellValueAsString(row.getCell(descIdx)).trim()
                    val rawType = getCellValueAsString(row.getCell(typeIdx)).trim().uppercase(Locale.US)
                    val type = if (rawType == "DUE" || rawType == "DEBT") rawType else "DEBT"
                    val dueDateMs = if (dueIdx != -1) getCellValueAsDateMs(row.getCell(dueIdx)) else null
                    val statusStr = if (statusIdx != -1) getCellValueAsString(row.getCell(statusIdx)).trim().lowercase() else ""
                    val isCleared = statusStr == "settled" || statusStr == "cleared" || statusStr == "true" || statusStr == "1"

                    debtsDues.add(
                        DebtDue(
                            personName = personName,
                            amount = amount,
                            description = description,
                            date = dateMs,
                            dueDate = dueDateMs,
                            type = type,
                            isCleared = isCleared
                        )
                    )
                }
            }
        }

        // 4. Parse Planned Sheet
        if (plannedSheet != null) {
            val headerRow = plannedSheet.getRow(0)
            if (headerRow != null) {
                var titleIdx = -1
                var amtIdx = -1
                var catIdx = -1
                var typeIdx = -1
                var accIdx = -1
                var startIdx = -1
                var intTypeIdx = -1
                var intNIdx = -1
                var oneTimeIdx = -1
                var nextDueIdx = -1
                var activeIdx = -1
                var descIdx = -1

                for (cell in headerRow) {
                    val txt = getCellValueAsString(cell).trim().lowercase(Locale.US)
                    when (txt) {
                        "title" -> titleIdx = cell.columnIndex
                        "amount" -> amtIdx = cell.columnIndex
                        "category" -> catIdx = cell.columnIndex
                        "type" -> typeIdx = cell.columnIndex
                        "account" -> accIdx = cell.columnIndex
                        "start date" -> startIdx = cell.columnIndex
                        "interval type" -> intTypeIdx = cell.columnIndex
                        "interval n" -> intNIdx = cell.columnIndex
                        "one time" -> oneTimeIdx = cell.columnIndex
                        "next due date" -> nextDueIdx = cell.columnIndex
                        "is active" -> activeIdx = cell.columnIndex
                        "description" -> descIdx = cell.columnIndex
                    }
                }

                if (titleIdx == -1) titleIdx = 0
                if (amtIdx == -1) amtIdx = 1
                if (catIdx == -1) catIdx = 2

                val rowIterator = plannedSheet.iterator()
                if (rowIterator.hasNext()) rowIterator.next() // Skip header
                while (rowIterator.hasNext()) {
                    val row = rowIterator.next()
                    if (isRowEmpty(row)) continue

                    val title = getCellValueAsString(row.getCell(titleIdx)).trim()
                    if (title.isEmpty()) continue

                    val amount = getCellValueAsDouble(row.getCell(amtIdx)) ?: 0.0
                    if (amount <= 0.0) continue

                    val category = getCellValueAsString(row.getCell(catIdx)).trim()
                    val rawType = if (typeIdx != -1) getCellValueAsString(row.getCell(typeIdx)).trim().uppercase(Locale.US) else "EXPENSE"
                    val type = if (rawType == "INCOME" || rawType == "EXPENSE") rawType else "EXPENSE"
                    val accountName = if (accIdx != -1) getCellValueAsString(row.getCell(accIdx)).trim() else "Cash"
                    val startDateMs = if (startIdx != -1) getCellValueAsDateMs(row.getCell(startIdx)) ?: System.currentTimeMillis() else System.currentTimeMillis()
                    val intervalType = if (intTypeIdx != -1) getCellValueAsString(row.getCell(intTypeIdx)).trim().uppercase(Locale.US) else "MONTH"
                    val intervalN = if (intNIdx != -1) getCellValueAsDouble(row.getCell(intNIdx))?.toInt() ?: 1 else 1
                    val oneTimeVal = if (oneTimeIdx != -1) getCellValueAsString(row.getCell(oneTimeIdx)).trim().lowercase() else "false"
                    val oneTime = oneTimeVal == "true" || oneTimeVal == "yes" || oneTimeVal == "1"
                    val nextDueDateMs = if (nextDueIdx != -1) getCellValueAsDateMs(row.getCell(nextDueIdx)) ?: startDateMs else startDateMs
                    val activeVal = if (activeIdx != -1) getCellValueAsString(row.getCell(activeIdx)).trim().lowercase() else "true"
                    val isActive = activeVal == "true" || activeVal == "yes" || activeVal == "1"
                    val description = if (descIdx != -1) getCellValueAsString(row.getCell(descIdx)).trim() else ""

                    rawPlanned.add(
                        RawPlannedImport(
                            title = title,
                            amount = amount,
                            category = if (category.isEmpty()) "Other" else category,
                            type = type,
                            accountName = if (accountName.isEmpty()) "Cash" else accountName,
                            startDate = startDateMs,
                            intervalType = if (intervalType.isEmpty()) "MONTH" else intervalType,
                            intervalN = intervalN,
                            oneTime = oneTime,
                            nextDueDate = nextDueDateMs,
                            isActive = isActive,
                            description = description
                        )
                    )
                }
            }
        }

        workbook.close()
        return AppImportData(accounts, rawExpenses, debtsDues, rawPlanned)
    }

    // Legacy method for backward compatibility / tests
    fun exportExpenses(outputStream: OutputStream, expenses: List<Expense>) {
        val workbook = HSSFWorkbook()
        val sheet = workbook.createSheet("Expenses")

        val headerRow = sheet.createRow(0)
        val headers = listOf("Date", "Category", "Description", "Amount", "Type")
        for (i in headers.indices) {
            val cell = headerRow.createCell(i)
            cell.setCellValue(headers[i])
        }

        var rowIndex = 1
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (expense in expenses) {
            val row = sheet.createRow(rowIndex++)
            row.createCell(0).setCellValue(sdf.format(Date(expense.date)))
            row.createCell(1).setCellValue(expense.category)
            row.createCell(2).setCellValue(expense.description)
            row.createCell(3).setCellValue(expense.amount)
            row.createCell(4).setCellValue(expense.type)
        }

        sheet.setColumnWidth(0, 20 * 256)
        sheet.setColumnWidth(1, 15 * 256)
        sheet.setColumnWidth(2, 30 * 256)
        sheet.setColumnWidth(3, 15 * 256)
        sheet.setColumnWidth(4, 15 * 256)

        workbook.write(outputStream)
        workbook.close()
    }

    // New unified export method supporting all sheets
    fun exportAllData(
        outputStream: OutputStream,
        accounts: List<Account>,
        expenses: List<Expense>,
        debtsDues: List<DebtDue>,
        plannedTransactions: List<PlannedTransaction>
    ) {
        val workbook = HSSFWorkbook()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        val accountMap = accounts.associate { it.id to it.name }

        // Sheet 1: Accounts
        val sheet1 = workbook.createSheet("Accounts")
        val headerRow1 = sheet1.createRow(0)
        val headers1 = listOf("Name", "Balance", "ColorHex", "Icon", "Currency", "IncludeInBalance", "DisplayOrder")
        for (i in headers1.indices) {
            val cell = headerRow1.createCell(i)
            cell.setCellValue(headers1[i])
        }
        var rowIndex1 = 1
        for (acc in accounts) {
            val row = sheet1.createRow(rowIndex1++)
            row.createCell(0).setCellValue(acc.name)
            row.createCell(1).setCellValue(acc.balance)
            row.createCell(2).setCellValue(acc.colorHex)
            row.createCell(3).setCellValue(acc.icon)
            row.createCell(4).setCellValue(acc.currency)
            row.createCell(5).setCellValue(if (acc.includeInBalance) "True" else "False")
            row.createCell(6).setCellValue(acc.displayOrder.toDouble())
        }
        sheet1.setColumnWidth(0, 20 * 256)
        sheet1.setColumnWidth(1, 15 * 256)
        sheet1.setColumnWidth(2, 15 * 256)
        sheet1.setColumnWidth(3, 15 * 256)
        sheet1.setColumnWidth(4, 10 * 256)
        sheet1.setColumnWidth(5, 20 * 256)
        sheet1.setColumnWidth(6, 15 * 256)

        // Sheet 2: Expenses
        val sheet2 = workbook.createSheet("Expenses")
        val headerRow2 = sheet2.createRow(0)
        val headers2 = listOf("Date", "Category", "Description", "Amount", "Type", "Account", "ToAccount", "Tags")
        for (i in headers2.indices) {
            val cell = headerRow2.createCell(i)
            cell.setCellValue(headers2[i])
        }
        var rowIndex2 = 1
        for (expense in expenses) {
            val row = sheet2.createRow(rowIndex2++)
            row.createCell(0).setCellValue(sdf.format(Date(expense.date)))
            row.createCell(1).setCellValue(expense.category)
            row.createCell(2).setCellValue(expense.description)
            row.createCell(3).setCellValue(expense.amount)
            row.createCell(4).setCellValue(expense.type)
            row.createCell(5).setCellValue(accountMap[expense.accountId] ?: "Cash")
            row.createCell(6).setCellValue(expense.toAccountId?.let { accountMap[it] } ?: "")
            row.createCell(7).setCellValue(expense.tags)
        }
        sheet2.setColumnWidth(0, 20 * 256)
        sheet2.setColumnWidth(1, 15 * 256)
        sheet2.setColumnWidth(2, 30 * 256)
        sheet2.setColumnWidth(3, 15 * 256)
        sheet2.setColumnWidth(4, 15 * 256)
        sheet2.setColumnWidth(5, 15 * 256)
        sheet2.setColumnWidth(6, 15 * 256)
        sheet2.setColumnWidth(7, 20 * 256)

        // Sheet 3: Debts & Receivables
        val sheet3 = workbook.createSheet("Debts & Receivables")
        val headerRow3 = sheet3.createRow(0)
        val headers3 = listOf("Date", "Person", "Type", "Description", "Amount", "Due Date", "Status")
        for (i in headers3.indices) {
            val cell = headerRow3.createCell(i)
            cell.setCellValue(headers3[i])
        }
        var rowIndex3 = 1
        for (item in debtsDues) {
            val row = sheet3.createRow(rowIndex3++)
            row.createCell(0).setCellValue(sdf.format(Date(item.date)))
            row.createCell(1).setCellValue(item.personName)
            row.createCell(2).setCellValue(item.type)
            row.createCell(3).setCellValue(item.description)
            row.createCell(4).setCellValue(item.amount)
            val dueDateStr = item.dueDate?.let { sdf.format(Date(it)) } ?: "N/A"
            row.createCell(5).setCellValue(dueDateStr)
            row.createCell(6).setCellValue(if (item.isCleared) "Settled" else "Pending")
        }
        sheet3.setColumnWidth(0, 20 * 256)
        sheet3.setColumnWidth(1, 20 * 256)
        sheet3.setColumnWidth(2, 10 * 256)
        sheet3.setColumnWidth(3, 30 * 256)
        sheet3.setColumnWidth(4, 15 * 256)
        sheet3.setColumnWidth(5, 20 * 256)
        sheet3.setColumnWidth(6, 15 * 256)

        // Sheet 4: Planned Transactions
        val sheet4 = workbook.createSheet("Planned Transactions")
        val headerRow4 = sheet4.createRow(0)
        val headers4 = listOf("Title", "Amount", "Category", "Type", "Account", "Start Date", "Interval Type", "Interval N", "One Time", "Next Due Date", "Is Active", "Description")
        for (i in headers4.indices) {
            val cell = headerRow4.createCell(i)
            cell.setCellValue(headers4[i])
        }
        var rowIndex4 = 1
        for (planned in plannedTransactions) {
            val row = sheet4.createRow(rowIndex4++)
            row.createCell(0).setCellValue(planned.title)
            row.createCell(1).setCellValue(planned.amount)
            row.createCell(2).setCellValue(planned.category)
            row.createCell(3).setCellValue(planned.type)
            row.createCell(4).setCellValue(accountMap[planned.accountId] ?: "Cash")
            row.createCell(5).setCellValue(sdf.format(Date(planned.startDate)))
            row.createCell(6).setCellValue(planned.intervalType)
            row.createCell(7).setCellValue(planned.intervalN.toDouble())
            row.createCell(8).setCellValue(if (planned.oneTime) "Yes" else "No")
            row.createCell(9).setCellValue(sdf.format(Date(planned.nextDueDate)))
            row.createCell(10).setCellValue(if (planned.isActive) "Yes" else "No")
            row.createCell(11).setCellValue(planned.description)
        }
        sheet4.setColumnWidth(0, 20 * 256)
        sheet4.setColumnWidth(1, 15 * 256)
        sheet4.setColumnWidth(2, 15 * 256)
        sheet4.setColumnWidth(3, 10 * 256)
        sheet4.setColumnWidth(4, 15 * 256)
        sheet4.setColumnWidth(5, 20 * 256)
        sheet4.setColumnWidth(6, 15 * 256)
        sheet4.setColumnWidth(7, 12 * 256)
        sheet4.setColumnWidth(8, 12 * 256)
        sheet4.setColumnWidth(9, 20 * 256)
        sheet4.setColumnWidth(10, 12 * 256)
        sheet4.setColumnWidth(11, 25 * 256)

        workbook.write(outputStream)
        workbook.close()
    }

    private fun isRowEmpty(row: Row): Boolean {
        for (c in row.firstCellNum until row.lastCellNum) {
            val cell = row.getCell(c)
            if (cell != null && cell.cellType != CellType.BLANK) {
                return false
            }
        }
        return true
    }

    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    sdf.format(cell.dateCellValue)
                } else {
                    val d = cell.numericCellValue
                    if (d == d.toLong().toDouble()) {
                        d.toLong().toString()
                    } else {
                        d.toString()
                    }
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    cell.stringCellValue
                } catch (e: Exception) {
                    try {
                        cell.numericCellValue.toString()
                    } catch (ex: Exception) {
                        ""
                    }
                }
            }
            else -> ""
        }
    }

    private fun getCellValueAsDouble(cell: Cell?): Double? {
        if (cell == null) return null
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.toDoubleOrNull()
            CellType.FORMULA -> {
                try {
                    cell.numericCellValue
                } catch (e: Exception) {
                    cell.stringCellValue.toDoubleOrNull()
                }
            }
            else -> null
        }
    }

    private fun getCellValueAsDateMs(cell: Cell?): Long? {
        if (cell == null) return null
        when (cell.cellType) {
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.dateCellValue.time
                }

                val num = cell.numericCellValue
                if (num > 1000000000000L) {
                    return num.toLong()
                } else if (num > 1000000000L) {
                    return num.toLong() * 1000L
                } else {
                    try {
                        return DateUtil.getJavaDate(num).time
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            CellType.STRING -> {
                val str = cell.stringCellValue.trim()
                if (str.isEmpty()) return null

                str.toLongOrNull()?.let {
                    if (it > 1000000000000L) return it
                    if (it > 1000000000L) return it * 1000L
                }

                for (df in dateFormats) {
                    try {
                        return df.parse(str)?.time
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            else -> {}
        }
        return null
    }
}
