package com.neosparkx.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.neosparkx.expensetracker.data.Account
import com.neosparkx.expensetracker.data.PlannedTransaction
import com.neosparkx.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlannedTransactionDialog(
    plannedToEdit: PlannedTransaction? = null,
    accounts: List<Account>,
    existingCategories: List<String> = listOf("Food", "Other"),
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        amount: Double,
        category: String,
        type: String,
        accountId: Int,
        startDate: Long,
        intervalType: String,
        intervalN: Int,
        oneTime: Boolean,
        description: String
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(plannedToEdit?.title ?: "") }
    var amountStr by remember { mutableStateOf(plannedToEdit?.amount?.toString() ?: "") }
    var transactionType by remember { mutableStateOf(plannedToEdit?.type ?: "EXPENSE") }
    var category by remember { mutableStateOf(plannedToEdit?.category ?: "") }
    var selectedAccountId by remember { mutableStateOf(plannedToEdit?.accountId ?: accounts.find { it.id == 1 }?.id ?: accounts.firstOrNull()?.id ?: 1) }
    var description by remember { mutableStateOf(plannedToEdit?.description ?: "") }

    // Recurrence settings
    var oneTime by remember { mutableStateOf(plannedToEdit?.oneTime ?: false) }
    var intervalType by remember { mutableStateOf(plannedToEdit?.intervalType ?: "MONTH") }
    var intervalNStr by remember { mutableStateOf(plannedToEdit?.intervalN?.toString() ?: "1") }

    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var intervalDropdownExpanded by remember { mutableStateOf(false) }

    val selectedAccount = remember(selectedAccountId, accounts) {
        accounts.find { it.id == selectedAccountId }
    }

    val quickCategories = remember(transactionType, existingCategories) {
        if (transactionType == "INCOME") {
            listOf("Salary", "Freelance", "Investment", "Tuition", "Gift", "Other")
        } else {
            (existingCategories.filter { it != "Salary" && it != "Freelance" && it != "Investment" && it != "Tuition" } + listOf("Food", "Shopping", "Bills", "Travel", "Other")).distinct()
        }
    }

    val intervalTypes = listOf(
        "DAY" to "Day(s)",
        "WEEK" to "Week(s)",
        "MONTH" to "Month(s)",
        "YEAR" to "Year(s)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = ThemeBackground
            ),
            modifier = Modifier
                .padding(16.dp)
                .border(1.dp, CardSurface, RoundedCornerShape(24.dp))
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (plannedToEdit == null) "Schedule Expenses" else "Edit Scheduled Expense",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                )

                // Transaction Type Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSurface, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "EXPENSE" to "Expense",
                        "INCOME" to "Income"
                    ).forEach { (typeKey, typeLabel) ->
                        val isSelected = transactionType == typeKey
                        val activeBg = if (typeKey == "EXPENSE") Color(0xFFEA3B35) else Color(0xFF4CAF50)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) activeBg else Color.Transparent)
                                .clickable { 
                                    transactionType = typeKey
                                    category = ""
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = typeLabel,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Rent, Spotify)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = CardSurface,
                        focusedLabelColor = PrimaryAccent,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = PrimaryAccent
                    )
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = CardSurface,
                        focusedLabelColor = PrimaryAccent,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = PrimaryAccent
                    )
                )

                // Account Selection
                if (accounts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Account to charge/deposit",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardSurface)
                                .clickable { accountDropdownExpanded = true }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedAccount?.name ?: "Select Account",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = "Expand",
                                    tint = TextSecondary
                                )
                            }
                            DropdownMenu(
                                expanded = accountDropdownExpanded,
                                onDismissRequest = { accountDropdownExpanded = false },
                                modifier = Modifier.background(ThemeBackground)
                            ) {
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc.name, color = TextPrimary) },
                                        onClick = {
                                            selectedAccountId = acc.id
                                            accountDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Schedule Type Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSurface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "One-Time Scheduled",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Log once and complete",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                    Switch(
                        checked = oneTime,
                        onCheckedChange = { oneTime = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryAccent,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CardSurface
                        )
                    )
                }

                // Recurrence details (Interval Type and N)
                if (!oneTime) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = intervalNStr,
                            onValueChange = { intervalNStr = it },
                            label = { Text("Every") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(80.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = CardSurface,
                                focusedLabelColor = PrimaryAccent,
                                unfocusedLabelColor = TextSecondary,
                                cursorColor = PrimaryAccent
                            )
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Interval Type",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardSurface)
                                    .clickable { intervalDropdownExpanded = true }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = intervalTypes.find { it.first == intervalType }?.second ?: "Month(s)",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = "Expand",
                                        tint = TextSecondary
                                    )
                                }
                                DropdownMenu(
                                    expanded = intervalDropdownExpanded,
                                    onDismissRequest = { intervalDropdownExpanded = false },
                                    modifier = Modifier.background(ThemeBackground)
                                ) {
                                    intervalTypes.forEach { (typeKey, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label, color = TextPrimary) },
                                            onClick = {
                                                intervalType = typeKey
                                                intervalDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("Type category (e.g. Rent, Food)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = CardSurface,
                        focusedLabelColor = PrimaryAccent,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = PrimaryAccent
                    )
                )

                if (quickCategories.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickCategories, key = { it }) { catName ->
                            val isSelected = category.trim().lowercase(Locale.US) == catName.trim().lowercase(Locale.US)
                            val chipSelectedColor = if (transactionType == "EXPENSE") Color(0xFFEA3B35) else Color(0xFF4CAF50)
                            FilterChip(
                                selected = isSelected,
                                onClick = { category = catName },
                                label = { Text(catName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipSelectedColor,
                                    selectedLabelColor = Color.White,
                                    containerColor = CardSurface,
                                    labelColor = TextPrimary
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = CardSurface,
                        focusedLabelColor = PrimaryAccent,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = PrimaryAccent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (plannedToEdit != null && onDelete != null) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEA3B35))
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = amountStr.toDoubleOrNull() ?: 0.0
                                val n = intervalNStr.toIntOrNull() ?: 1
                                if (title.trim().isNotEmpty() && amt > 0) {
                                    val finalCat = if (category.trim().isEmpty()) "Other" else category.trim()
                                    val start = plannedToEdit?.startDate ?: System.currentTimeMillis()
                                    onConfirm(title.trim(), amt, finalCat, transactionType, selectedAccountId, start, intervalType, n, oneTime, description.trim())
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryAccent,
                                contentColor = Color.White
                            ),
                            enabled = title.trim().isNotEmpty() && amountStr.toDoubleOrNull() != null && amountStr.toDoubleOrNull()!! > 0
                        ) {
                            Text("Schedule", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

