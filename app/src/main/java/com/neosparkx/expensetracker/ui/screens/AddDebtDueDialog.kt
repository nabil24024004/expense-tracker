package com.neosparkx.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.clip
import com.neosparkx.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

import com.neosparkx.expensetracker.data.Account
import androidx.compose.material.icons.rounded.ArrowDropDown
import com.neosparkx.expensetracker.ui.components.AutoScaleText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtDueDialog(
    onDismiss: () -> Unit,
    accounts: List<Account> = emptyList(),
    onConfirm: (personName: String, amount: Double, description: String, type: String, dueDate: Long?, accountId: Int?, addToAccountNow: Boolean) -> Unit
) {
    var personName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("DEBT") } // "DEBT" (Owed to them) or "DUE" (Owed to me)
    
    var selectedAccountId by remember { mutableStateOf(accounts.find { it.id == 1 }?.id ?: accounts.firstOrNull()?.id) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    val selectedAccount = remember(selectedAccountId, accounts) {
        accounts.find { it.id == selectedAccountId }
    }
    var addToAccountNow by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var dueDateMs by remember { mutableStateOf<Long?>(null) }
    var dueDateStr by remember { mutableStateOf("") }

    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth, 23, 59, 59)
                dueDateMs = selectedCal.timeInMillis
                val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.US)
                dueDateStr = sdf.format(selectedCal.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

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
                    text = "Add Debt / Receivable",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                )

                // Type Segmented Selection Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(CardSurface, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val types = listOf("DEBT" to "I Owe (Debt)", "DUE" to "Owed to Me (Due)")
                    types.forEach { (typeKey, typeLabel) ->
                        val isSelected = type == typeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    if (isSelected) PrimaryAccent else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { type = typeKey },
                            contentAlignment = Alignment.Center
                        ) {
                            AutoScaleText(
                                text = typeLabel,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(fontSize = 13.sp),
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                // Person Name Field
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Person's Name") },
                    modifier = Modifier.fillMaxWidth().testTag("person_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = CardSurface,
                        focusedLabelColor = PrimaryAccent,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = PrimaryAccent
                    )
                )

                // Amount Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("debt_amount_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = CardSurface,
                        focusedLabelColor = PrimaryAccent,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = PrimaryAccent
                    )
                )
                
                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Reason / Note") },
                    modifier = Modifier.fillMaxWidth().testTag("debt_description_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = CardSurface,
                        focusedLabelColor = PrimaryAccent,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = PrimaryAccent
                    )
                )

                // Account Dropdown Selector
                if (accounts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Account",
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

                // Add-to-account toggle (only visible when an account is selected)
                if (accounts.isNotEmpty() && selectedAccountId != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { addToAccountNow = !addToAccountNow }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (type == "DEBT") "Add amount to account now?" else "Deduct amount from account now?",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (type == "DEBT") "Record as income (you already received the money)" else "Record as expense (you already paid the money)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = addToAccountNow,
                                onCheckedChange = { addToAccountNow = it },
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
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardSurface, RoundedCornerShape(12.dp))
                        .clickable { datePickerDialog.show() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Due Date (Optional)",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (dueDateStr.isEmpty()) "Select Due Date" else dueDateStr,
                                color = if (dueDateStr.isEmpty()) TextSecondary else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (dueDateStr.isEmpty()) FontWeight.Normal else FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Select Date",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Dialog Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && personName.trim().isNotEmpty()) {
                                val finalDescription = if (description.trim().isEmpty()) "Debt/Receivable log" else description.trim()
                                onConfirm(personName.trim(), amt, finalDescription, type, dueDateMs, selectedAccountId, addToAccountNow)
                            }
                        },
                        modifier = Modifier.testTag("save_debt_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

