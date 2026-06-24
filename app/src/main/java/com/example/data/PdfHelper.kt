package com.example.data

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
}
