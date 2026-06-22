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
            rowIterator.next() // Skip header row
        }

        while (rowIterator.hasNext()) {
            val row = rowIterator.next()
            
            // Check if row is entirely empty
            if (isRowEmpty(row)) continue

            val dateCell = row.getCell(dateColIndex)
            val categoryCell = row.getCell(categoryColIndex)
            val descriptionCell = row.getCell(descriptionColIndex)
            val amountCell = row.getCell(amountColIndex)

            // Parse Amount
            val amount = getCellValueAsDouble(amountCell) ?: continue // Skip rows with invalid or empty amount
            if (amount <= 0.0) continue // Skip invalid amounts

            // Parse Date
            val dateMs = getCellValueAsDateMs(dateCell) ?: System.currentTimeMillis()

            // Parse Category & Description
            val category = getCellValueAsString(categoryCell).trim()
            val description = getCellValueAsString(descriptionCell).trim()

            if (category.isEmpty() && description.isEmpty() && amount == 0.0) {
                continue // Skip empty-looking row
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

        // Header Row
        val headerRow = sheet.createRow(0)
        val headers = listOf("Date", "Category", "Description", "Amount")
        for (i in headers.indices) {
            val cell = headerRow.createCell(i)
            cell.setCellValue(headers[i])
        }

        // Data Rows
        var rowIndex = 1
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (expense in expenses) {
            val row = sheet.createRow(rowIndex++)

            // Date (write formatted string directly)
            row.createCell(0).setCellValue(sdf.format(Date(expense.date)))

            // Category
            row.createCell(1).setCellValue(expense.category)

            // Description
            row.createCell(2).setCellValue(expense.description)

            // Amount
            row.createCell(3).setCellValue(expense.amount)
        }

        // Set manual column widths (in characters * 256) to avoid AWT autoSizeColumn crashes on Android
        sheet.setColumnWidth(0, 20 * 256) // Date (approx. 19 chars)
        sheet.setColumnWidth(1, 15 * 256) // Category
        sheet.setColumnWidth(2, 30 * 256) // Description
        sheet.setColumnWidth(3, 15 * 256) // Amount (approx. 12 chars + extra padding)

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
                // Try treating numeric value as Excel serial date or raw millis timestamp
                val num = cell.numericCellValue
                if (num > 1000000000000L) { // Looks like Unix timestamp in ms
                    return num.toLong()
                } else if (num > 1000000000L) { // Looks like Unix timestamp in seconds
                    return num.toLong() * 1000L
                } else { // Excel Serial Date
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
                // Try parsing Unix timestamp
                str.toLongOrNull()?.let {
                    if (it > 1000000000000L) return it
                    if (it > 1000000000L) return it * 1000L
                }
                // Try simple date format parsers
                for (df in dateFormats) {
                    try {
                        return df.parse(str)?.time
                    } catch (e: Exception) {
                        // Continue
                    }
                }
            }
            else -> {}
        }
        return null
    }
}
