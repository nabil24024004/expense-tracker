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

    fun importExpenses(inputStream: InputStream): List<Expense> {
        val expenses = mutableListOf<Expense>()
        val bytes = inputStream.readBytes()
        val workbook = try {
            HSSFWorkbook(ByteArrayInputStream(bytes))
        } catch (e: Exception) {
            WorkbookFactory.create(ByteArrayInputStream(bytes))
        }
        val sheet = workbook.getSheetAt(0) ?: throw IllegalArgumentException("Excel sheet is empty")

        val headerRow = sheet.getRow(0) ?: throw IllegalArgumentException("Missing header row in Excel sheet")

        var dateColIndex = -1
        var categoryColIndex = -1
        var descriptionColIndex = -1
        var amountColIndex = -1

        for (cell in headerRow) {
            val headerText = getCellValueAsString(cell).trim().lowercase(Locale.US)
            when (headerText) {
                "date" -> dateColIndex = cell.columnIndex
                "category" -> categoryColIndex = cell.columnIndex
                "description" -> descriptionColIndex = cell.columnIndex
                "amount" -> amountColIndex = cell.columnIndex
            }
        }

        if (dateColIndex == -1 || categoryColIndex == -1 || descriptionColIndex == -1 || amountColIndex == -1) {
            val missing = mutableListOf<String>()
            if (dateColIndex == -1) missing.add("Date")
            if (categoryColIndex == -1) missing.add("Category")
            if (descriptionColIndex == -1) missing.add("Description")
            if (amountColIndex == -1) missing.add("Amount")
            throw IllegalArgumentException("Missing headers: ${missing.joinToString(", ")}. File must have: Date, Category, Description, Amount.")
        }

        val rowIterator = sheet.iterator()
        if (rowIterator.hasNext()) {
            rowIterator.next()
        }

        while (rowIterator.hasNext()) {
            val row = rowIterator.next()
            

            if (isRowEmpty(row)) continue

            val dateCell = row.getCell(dateColIndex)
            val categoryCell = row.getCell(categoryColIndex)
            val descriptionCell = row.getCell(descriptionColIndex)
            val amountCell = row.getCell(amountColIndex)


            val amount = getCellValueAsDouble(amountCell) ?: continue
            if (amount <= 0.0) continue


            val dateMs = getCellValueAsDateMs(dateCell) ?: System.currentTimeMillis()


            val category = getCellValueAsString(categoryCell).trim()
            val description = getCellValueAsString(descriptionCell).trim()

            if (category.isEmpty() && description.isEmpty() && amount == 0.0) {
                continue
            }

            val finalCategory = if (category.isEmpty()) "Other" else category
            val finalDescription = if (description.isEmpty()) "Imported Transaction" else description

            expenses.add(
                Expense(
                    amount = amount,
                    description = finalDescription,
                    category = finalCategory,
                    date = dateMs
                )
            )
        }

        workbook.close()
        return expenses
    }

    fun exportExpenses(outputStream: OutputStream, expenses: List<Expense>) {
        val workbook = HSSFWorkbook()
        val sheet = workbook.createSheet("Expenses")


        val headerRow = sheet.createRow(0)
        val headers = listOf("Date", "Category", "Description", "Amount")
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
        }


        sheet.setColumnWidth(0, 20 * 256)
        sheet.setColumnWidth(1, 15 * 256)
        sheet.setColumnWidth(2, 30 * 256)
        sheet.setColumnWidth(3, 15 * 256)

        workbook.write(outputStream)
        workbook.close()
    }

    fun exportAllData(outputStream: OutputStream, expenses: List<Expense>, debtsDues: List<DebtDue>) {
        val workbook = HSSFWorkbook()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        
        // Sheet 1: Expenses
        val sheet1 = workbook.createSheet("Expenses")
        val headerRow1 = sheet1.createRow(0)
        val headers1 = listOf("Date", "Category", "Description", "Amount")
        for (i in headers1.indices) {
            val cell = headerRow1.createCell(i)
            cell.setCellValue(headers1[i])
        }

        var rowIndex1 = 1
        for (expense in expenses) {
            val row = sheet1.createRow(rowIndex1++)
            row.createCell(0).setCellValue(sdf.format(Date(expense.date)))
            row.createCell(1).setCellValue(expense.category)
            row.createCell(2).setCellValue(expense.description)
            row.createCell(3).setCellValue(expense.amount)
        }

        sheet1.setColumnWidth(0, 20 * 256)
        sheet1.setColumnWidth(1, 15 * 256)
        sheet1.setColumnWidth(2, 30 * 256)
        sheet1.setColumnWidth(3, 15 * 256)
        // Sheet 2: Debts & Receivables
        val sheet2 = workbook.createSheet("Debts & Receivables")
        val headerRow2 = sheet2.createRow(0)
        val headers2 = listOf("Date", "Person", "Type", "Description", "Amount", "Due Date", "Status")
        for (i in headers2.indices) {
            val cell = headerRow2.createCell(i)
            cell.setCellValue(headers2[i])
        }

        var rowIndex2 = 1
        for (item in debtsDues) {
            val row = sheet2.createRow(rowIndex2++)
            row.createCell(0).setCellValue(sdf.format(Date(item.date)))
            row.createCell(1).setCellValue(item.personName)
            row.createCell(2).setCellValue(item.type)
            row.createCell(3).setCellValue(item.description)
            row.createCell(4).setCellValue(item.amount)
            val dueDateStr = item.dueDate?.let { sdf.format(Date(it)) } ?: "N/A"
            row.createCell(5).setCellValue(dueDateStr)
            row.createCell(6).setCellValue(if (item.isCleared) "Settled" else "Pending")
        }

        sheet2.setColumnWidth(0, 20 * 256)
        sheet2.setColumnWidth(1, 20 * 256)
        sheet2.setColumnWidth(2, 10 * 256)
        sheet2.setColumnWidth(3, 30 * 256)
        sheet2.setColumnWidth(4, 15 * 256)
        sheet2.setColumnWidth(5, 20 * 256)
        sheet2.setColumnWidth(6, 15 * 256)

        workbook.write(outputStream)
        workbook.close()
    }

    fun exportGroupExpenses(
        outputStream: OutputStream,
        group: com.example.group.data.entity.GroupEntity,
        expenses: List<com.example.group.data.entity.GroupExpenseEntity>,
        members: List<com.example.group.data.entity.MemberEntity>
    ) {
        val workbook = HSSFWorkbook()
        val sheet = workbook.createSheet("Group Expenses")

        val headerRow = sheet.createRow(0)
        val headers = listOf("Date", "Title", "Category", "Amount", "Currency", "Notes")
        for (i in headers.indices) {
            val cell = headerRow.createCell(i)
            cell.setCellValue(headers[i])
        }

        var rowIndex = 1
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (expense in expenses) {
            val row = sheet.createRow(rowIndex++)
            row.createCell(0).setCellValue(sdf.format(Date(expense.date)))
            row.createCell(1).setCellValue(expense.title)
            row.createCell(2).setCellValue(expense.category)
            row.createCell(3).setCellValue(expense.amount)
            row.createCell(4).setCellValue(expense.currency)
            row.createCell(5).setCellValue(expense.notes ?: "")
        }

        sheet.setColumnWidth(0, 20 * 256)
        sheet.setColumnWidth(1, 25 * 256)
        sheet.setColumnWidth(2, 15 * 256)
        sheet.setColumnWidth(3, 15 * 256)
        sheet.setColumnWidth(4, 10 * 256)
        sheet.setColumnWidth(5, 30 * 256)

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
                    cell.numericCellValue.toString()
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

                    }
                }
            }
            else -> {}
        }
        return null
    }
}
