package com.neosparkx.expensetracker.data

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfHelper {
    fun exportExpenses(outputStream: OutputStream, expenses: List<Expense>) {
        val pdfDocument = PdfDocument()
        
        // A4 page sizes in points (1/72 inch): 595 x 842
        val pageWidth = 595
        val pageHeight = 842
        
        val paintTitle = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val paintHeader = Paint().apply {
            textSize = 11f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val paintText = Paint().apply {
            textSize = 9f
            color = Color.BLACK
        }
        val paintLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        
        val marginStart = 40f
        val marginEnd = 40f
        val marginTop = 50f
        val marginBottom = 50f
        
        val contentWidth = pageWidth - marginStart - marginEnd
        val colWidths = floatArrayOf(
            contentWidth * 0.35f, // Date
            contentWidth * 0.20f, // Category
            contentWidth * 0.30f, // Description
            contentWidth * 0.15f  // Amount
        )
        
        val colStarts = FloatArray(4)
        var currentOffset = marginStart
        for (i in colWidths.indices) {
            colStarts[i] = currentOffset
            currentOffset += colWidths[i]
        }
        
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        var yPosition = marginTop
        
        // Draw Document Header
        canvas.drawText("Expense Tracker - Transaction Logs", marginStart, yPosition, paintTitle)
        yPosition += 30f
        
        // Draw Summary Stats
        val totalSpent = expenses.sumOf { it.amount }
        canvas.drawText("Total Transactions: ${expenses.size}   |   Total Spent: ৳${String.format(Locale.US, "%,.2f", totalSpent)}", marginStart, yPosition, paintText)
        yPosition += 25f
        
        // Draw Table Header
        canvas.drawLine(marginStart, yPosition, pageWidth - marginEnd, yPosition, paintLine)
        yPosition += 15f
        
        val headers = listOf("Date", "Category", "Description", "Amount")
        for (i in headers.indices) {
            canvas.drawText(headers[i], colStarts[i], yPosition, paintHeader)
        }
        yPosition += 8f
        canvas.drawLine(marginStart, yPosition, pageWidth - marginEnd, yPosition, paintLine)
        yPosition += 15f
        
        for (expense in expenses) {
            // Check if page needs splitting
            if (yPosition + 25f > pageHeight - marginBottom) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = marginTop
                
                // Draw Table Header on new page
                for (i in headers.indices) {
                    canvas.drawText(headers[i], colStarts[i], yPosition, paintHeader)
                }
                yPosition += 8f
                canvas.drawLine(marginStart, yPosition, pageWidth - marginEnd, yPosition, paintLine)
                yPosition += 15f
            }
            
            val dateStr = sdf.format(Date(expense.date))
            val amountStr = "৳${String.format(Locale.US, "%,.2f", expense.amount)}"
            
            canvas.drawText(dateStr, colStarts[0], yPosition, paintText)
            canvas.drawText(expense.category, colStarts[1], yPosition, paintText)
            
            // Description truncation if too long
            var desc = expense.description
            val maxDescWidth = colWidths[2] - 10f
            val measuredWidth = paintText.measureText(desc)
            if (measuredWidth > maxDescWidth) {
                val charCount = paintText.breakText(desc, true, maxDescWidth, null)
                desc = if (charCount > 3) desc.substring(0, charCount - 3) + "..." else desc
            }
            canvas.drawText(desc, colStarts[2], yPosition, paintText)
            
            canvas.drawText(amountStr, colStarts[3], yPosition, paintText)
            
            yPosition += 20f
        }
        
        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    fun exportAllData(outputStream: OutputStream, expenses: List<Expense>, debtsDues: List<DebtDue>) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        
        val paintTitle = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val paintSection = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val paintHeader = Paint().apply {
            textSize = 11f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val paintText = Paint().apply {
            textSize = 9f
            color = Color.BLACK
        }
        val paintTextBold = Paint().apply {
            textSize = 9f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val paintLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        
        val marginStart = 40f
        val marginEnd = 40f
        val marginTop = 50f
        val marginBottom = 50f
        
        val contentWidth = pageWidth - marginStart - marginEnd
        
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        var yPosition = marginTop
        
        // 1. Document Title
        canvas.drawText("Expense Tracker - Financial Report", marginStart, yPosition, paintTitle)
        yPosition += 30f
        
        // 2. Summary stats
        val totalSpent = expenses.sumOf { it.amount }
        val activeDebts = debtsDues.filter { !it.isCleared && it.type == "DEBT" }.sumOf { it.amount }
        val activeDues = debtsDues.filter { !it.isCleared && it.type == "DUE" }.sumOf { it.amount }
        val netDebtDue = activeDues - activeDebts
        val netStatus = if (netDebtDue >= 0) "Net Receivable: ৳${String.format(Locale.US, "%,.2f", netDebtDue)}" else "Net Payable: ৳${String.format(Locale.US, "%,.2f", -netDebtDue)}"
        
        canvas.drawText("Transactions Spent: ৳${String.format(Locale.US, "%,.2f", totalSpent)}", marginStart, yPosition, paintTextBold)
        yPosition += 15f
        canvas.drawText("Total Active Debts (You Owe): ৳${String.format(Locale.US, "%,.2f", activeDebts)}", marginStart, yPosition, paintText)
        yPosition += 15f
        canvas.drawText("Total Active Receivables (Owed to You): ৳${String.format(Locale.US, "%,.2f", activeDues)}", marginStart, yPosition, paintText)
        yPosition += 15f
        canvas.drawText(netStatus, marginStart, yPosition, paintTextBold)
        yPosition += 30f
        
        // 3. Section: Expenses
        canvas.drawText("Transaction Logs", marginStart, yPosition, paintSection)
        yPosition += 15f
        
        val colWidthsExp = floatArrayOf(
            contentWidth * 0.35f,
            contentWidth * 0.20f,
            contentWidth * 0.30f,
            contentWidth * 0.15f
        )
        val colStartsExp = FloatArray(4)
        var offsetExp = marginStart
        for (i in colWidthsExp.indices) {
            colStartsExp[i] = offsetExp
            offsetExp += colWidthsExp[i]
        }
        
        canvas.drawLine(marginStart, yPosition, pageWidth - marginEnd, yPosition, paintLine)
        yPosition += 15f
        val headersExp = listOf("Date", "Category", "Description", "Amount")
        for (i in headersExp.indices) {
            canvas.drawText(headersExp[i], colStartsExp[i], yPosition, paintHeader)
        }
        yPosition += 8f
        canvas.drawLine(marginStart, yPosition, pageWidth - marginEnd, yPosition, paintLine)
        yPosition += 15f
        
        // Draw expenses
        for (expense in expenses) {
            if (yPosition + 25f > pageHeight - marginBottom) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = marginTop
                
                // Draw Table Header on new page
                for (i in headersExp.indices) {
                    canvas.drawText(headersExp[i], colStartsExp[i], yPosition, paintHeader)
                }
                yPosition += 8f
                canvas.drawLine(marginStart, yPosition, pageWidth - marginEnd, yPosition, paintLine)
                yPosition += 15f
            }
            
            val dateStr = sdf.format(Date(expense.date))
            val amountStr = "৳${String.format(Locale.US, "%,.2f", expense.amount)}"
            
            canvas.drawText(dateStr, colStartsExp[0], yPosition, paintText)
            canvas.drawText(expense.category, colStartsExp[1], yPosition, paintText)
            
            var desc = expense.description
            val maxDescWidth = colWidthsExp[2] - 10f
            val measuredWidth = paintText.measureText(desc)
            if (measuredWidth > maxDescWidth) {
                val charCount = paintText.breakText(desc, true, maxDescWidth, null)
                desc = if (charCount > 3) desc.substring(0, charCount - 3) + "..." else desc
            }
            canvas.drawText(desc, colStartsExp[2], yPosition, paintText)
            canvas.drawText(amountStr, colStartsExp[3], yPosition, paintText)
            yPosition += 20f
        }
        
        // 4. Section: Debts & Dues
        pdfDocument.finishPage(page)
        pageNumber++
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas
        yPosition = marginTop
        
        canvas.drawText("Debts & Receivables Logs", marginStart, yPosition, paintSection)
        yPosition += 20f
        
        val colWidthsDebt = floatArrayOf(
            contentWidth * 0.20f, // Date
            contentWidth * 0.20f, // Person
            contentWidth * 0.12f, // Type
            contentWidth * 0.23f, // Description
            contentWidth * 0.13f, // Amount
            contentWidth * 0.12f  // Status
        )
        val colStartsDebt = FloatArray(6)
        var offsetDebt = marginStart
        for (i in colWidthsDebt.indices) {
            colStartsDebt[i] = offsetDebt
            offsetDebt += colWidthsDebt[i]
        }
        
        canvas.drawLine(marginStart, yPosition, pageWidth - marginEnd, yPosition, paintLine)
        yPosition += 15f
        val headersDebt = listOf("Date", "Person", "Type", "Note", "Amount", "Status")
        for (i in headersDebt.indices) {
            canvas.drawText(headersDebt[i], colStartsDebt[i], yPosition, paintHeader)
        }
        yPosition += 8f
        canvas.drawLine(marginStart, yPosition, pageWidth - marginEnd, yPosition, paintLine)
        yPosition += 15f
        
        for (item in debtsDues) {
            if (yPosition + 25f > pageHeight - marginBottom) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = marginTop
                
                // Draw Table Header on new page
                for (i in headersDebt.indices) {
                    canvas.drawText(headersDebt[i], colStartsDebt[i], yPosition, paintHeader)
                }
                yPosition += 8f
                canvas.drawLine(marginStart, yPosition, pageWidth - marginEnd, yPosition, paintLine)
                yPosition += 15f
            }
            
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(item.date))
            val amountStr = "৳${String.format(Locale.US, "%,.2f", item.amount)}"
            val statusStr = if (item.isCleared) "Settled" else "Pending"
            
            canvas.drawText(dateStr, colStartsDebt[0], yPosition, paintText)
            canvas.drawText(item.personName, colStartsDebt[1], yPosition, paintText)
            canvas.drawText(item.type, colStartsDebt[2], yPosition, paintText)
            
            var desc = item.description
            val maxDescWidth = colWidthsDebt[3] - 10f
            val measuredWidth = paintText.measureText(desc)
            if (measuredWidth > maxDescWidth) {
                val charCount = paintText.breakText(desc, true, maxDescWidth, null)
                desc = if (charCount > 3) desc.substring(0, charCount - 3) + "..." else desc
            }
            canvas.drawText(desc, colStartsDebt[3], yPosition, paintText)
            canvas.drawText(amountStr, colStartsDebt[4], yPosition, paintText)
            canvas.drawText(statusStr, colStartsDebt[5], yPosition, paintText)
            yPosition += 20f
        }
        
        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
}

