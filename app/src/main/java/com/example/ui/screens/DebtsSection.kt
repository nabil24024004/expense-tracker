package com.example.ui.screens

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DebtDue
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DebtsSection(
    type: String, // "DEBT" or "DUE"
    debtsDues: List<DebtDue>,
    onSettleDebtDue: (DebtDue, paidAmount: Double, logAsTransaction: Boolean) -> Unit,
    onDeleteDebtDue: (Int) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("pending") } // "all", "pending", "settled"
    
    var itemToSettle by remember { mutableStateOf<DebtDue?>(null) }
    var itemToDelete by remember { mutableStateOf<DebtDue?>(null) }

    val filteredList = remember(searchQuery, statusFilter, debtsDues, type) {
        debtsDues.filter { item ->
            item.type == type &&
            (item.personName.contains(searchQuery, ignoreCase = true) ||
             item.description.contains(searchQuery, ignoreCase = true)) &&
            when (statusFilter) {
                "pending" -> !item.isCleared
                "settled" -> item.isCleared
                else -> true
            }
        }
    }

    val totalAmount = remember(filteredList) {
        filteredList.sumOf { it.amount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Summary Banner Card
        val labelText = if (type == "DEBT") "Total Owed (Pending)" else "Total Collectible (Pending)"
        val activeTotal = remember(debtsDues, type) {
            debtsDues.filter { it.type == type && !it.isCleared }.sumOf { it.amount }
        }
        
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = labelText,
                    color = DarkCardTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "৳${String.format(Locale.US, "%,.2f", activeTotal)}",
                    color = DarkCardTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Search Outlined Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name or note...") },
            leadingIcon = { Icon(Icons.Rounded.Search, "Search", tint = TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryAccent,
                unfocusedBorderColor = CardSurface,
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                "pending" to "Active / Pending",
                "settled" to "Cleared / Settled",
                "all" to "All Logs"
            )
            filters.forEach { (filterKey, filterLabel) ->
                val isSelected = statusFilter == filterKey
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) PrimaryAccent else CardSurface,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { statusFilter = filterKey }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filterLabel,
                        color = if (isSelected) Color.White else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Debt / Due Items List
        val bottomPadding = 112.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = bottomPadding)
        ) {
            if (filteredList.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "No records found.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            textAlign = TextAlign.Center,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    DebtDueItem(
                        item = item,
                        onSettleClick = { itemToSettle = item },
                        onDeleteClick = { itemToDelete = item }
                    )
                }
            }
        }
    }

    // Confirmation Settle Dialog
    if (itemToSettle != null) {
        val target = itemToSettle!!
        val isDebt = target.type == "DEBT"
        var paymentTab by remember { mutableStateOf("full") } // "full" or "partial"
        var settleAmountText by remember { mutableStateOf("") }
        var logAsTransaction by remember { mutableStateOf(true) }

        // Set amount automatically when switching tabs
        LaunchedEffect(paymentTab, target.amount) {
            if (paymentTab == "full") {
                settleAmountText = String.format(Locale.US, "%.2f", target.amount)
            } else {
                settleAmountText = ""
            }
        }

        val parsedAmount = settleAmountText.toDoubleOrNull() ?: 0.0
        val isPartial = paymentTab == "partial" && parsedAmount > 0 && parsedAmount < target.amount
        val isFull = paymentTab == "full" || parsedAmount >= target.amount

        AlertDialog(
            onDismissRequest = { itemToSettle = null },
            title = {
                Text(
                    text = if (isDebt) "Settle Debt" else "Settle Receivable",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. User Profile summary with Avatar Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSurface, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val firstLetter = target.personName.firstOrNull()?.toString()?.uppercase(Locale.US) ?: "?"
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstLetter,
                                color = PrimaryAccent,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = target.personName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = if (isDebt) "You owe them" else "They owe you",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "৳${String.format(Locale.US, "%,.2f", target.amount)}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                                color = if (isDebt) Color(0xFFEA3B35) else Color(0xFF4CAF50)
                            )
                            Text(
                                text = if (isDebt) "Total Owed" else "Total Due",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // 2. Settlement Tabs (Full vs Partial)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSurface, RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "full" to "Full Settle",
                            "partial" to "Partial Pay"
                        ).forEach { (tabKey, tabLabel) ->
                            val isSelected = paymentTab == tabKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryAccent else Color.Transparent)
                                    .clickable { paymentTab = tabKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabLabel,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // 3. Amount Field
                    if (paymentTab == "partial") {
                        OutlinedTextField(
                            value = settleAmountText,
                            onValueChange = { settleAmountText = it },
                            label = { Text("Settlement Amount (৳)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = CardSurface,
                                focusedContainerColor = ThemeBackground,
                                unfocusedContainerColor = ThemeBackground,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    } else {
                        // Display full settle amount clearly
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ThemeBackground, RoundedCornerShape(12.dp))
                                .border(1.dp, CardSurface, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "SETTLEMENT AMOUNT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "৳${String.format(Locale.US, "%,.2f", target.amount)}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // 4. Logging Switch Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { logAsTransaction = !logAsTransaction }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isDebt) "Log repayment transaction" else "Log receipt transaction",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isDebt) "Add an expense entry automatically" else "Add an income entry automatically",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = logAsTransaction,
                                onCheckedChange = { logAsTransaction = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimaryAccent,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = ThemeBackground,
                                    uncheckedBorderColor = CardSurface
                                )
                            )
                        }
                    }

                    // 5. Dynamic Info Banner
                    val bannerText = when {
                        isFull -> if (isDebt) "Pending debt will be marked as fully settled." else "Pending receivable will be marked as fully settled."
                        isPartial && parsedAmount > 0.0 -> {
                            val remaining = target.amount - parsedAmount
                            "Partial payment recorded. Remaining ৳${String.format(Locale.US, "%,.2f", remaining)} will stay active."
                        }
                        else -> "Enter a valid payment amount."
                    }
                    val bannerBg = if (isFull) Color(0xFFE8F5E9) else if (isPartial && parsedAmount > 0.0) Color(0xFFFFECEB) else CardSurface
                    val bannerTextCol = if (isFull) Color(0xFF2E7D32) else if (isPartial && parsedAmount > 0.0) Color(0xFFC62828) else TextSecondary

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(bannerBg)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = bannerText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = bannerTextCol,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountToPay = if (paymentTab == "full") target.amount else parsedAmount
                        if (amountToPay > 0.0 && amountToPay <= target.amount) {
                            onSettleDebtDue(target, amountToPay, logAsTransaction)
                            val msg = if (amountToPay < target.amount) "Partial settlement recorded!" else "Record fully settled!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            itemToSettle = null
                        } else {
                            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToSettle = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ThemeBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Confirmation Delete Dialog
    if (itemToDelete != null) {
        val target = itemToDelete!!
        val isDebt = target.type == "DEBT"
        val typeLabel = if (isDebt) "debt" else "receivable"
        val titleLabel = if (isDebt) "Delete Debt" else "Delete Receivable"
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(titleLabel, color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this $typeLabel record with ${target.personName} for ৳${String.format(Locale.US, "%,.2f", target.amount)}? This action cannot be undone.", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDebtDue(target.id)
                        Toast.makeText(context, "Deleted record!", Toast.LENGTH_SHORT).show()
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ThemeBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun DebtDueItem(
    item: DebtDue,
    onSettleClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val dateStr = remember(item.date) { dateFormat.format(Date(item.date)) }
    val dueDateStr = remember(item.dueDate) { item.dueDate?.let { dateFormat.format(Date(it)) } }

    // Overdue logic
    val isOverdue = remember(item.dueDate, item.isCleared) {
        !item.isCleared && item.dueDate != null && item.dueDate < System.currentTimeMillis()
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored icon box based on Debt vs Due
                val isDebt = item.type == "DEBT"
                val tintColor = if (isDebt) Color(0xFFEA3B35) else Color(0xFF4CAF50) // Red vs Green
                val bgColor = if (isDebt) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                val icon = if (isDebt) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.type,
                        tint = tintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.personName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (isDebt) "Borrowed" else "Lent"}: $dateStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (dueDateStr != null) {
                        Text(
                            text = "Due: $dueDateStr",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isOverdue) Color(0xFFEA3B35) else TextSecondary
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "৳${String.format(Locale.US, "%,.2f", item.amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (item.isCleared) {
                        // Cleared badge
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Settled",
                                color = Color(0xFF2E7D32),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isOverdue) {
                        // Overdue badge
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Overdue",
                                color = Color(0xFFC62828),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Description and actions row (only for non-cleared, or expand note)
            if (item.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = ThemeBackground.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Action row if active
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!item.isCleared) {
                    // Settle Button
                    OutlinedButton(
                        onClick = onSettleClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAccent),
                        border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Settle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Delete Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
