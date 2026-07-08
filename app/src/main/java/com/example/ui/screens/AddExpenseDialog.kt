package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.data.Account
import com.example.data.MathEvaluator
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    initialCategory: String = "",
    existingCategories: List<String> = listOf("Food", "Other"),
    accounts: List<Account> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, description: String, category: String, type: String, accountId: Int?, toAccountId: Int?, tags: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("EXPENSE") }
    var category by remember { mutableStateOf(initialCategory) }
    
    // Account Selector
    var selectedAccountId by remember { mutableStateOf(accounts.find { it.id == 1 }?.id ?: accounts.firstOrNull()?.id) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val selectedAccount = remember(selectedAccountId, accounts) {
        accounts.find { it.id == selectedAccountId }
    }

    val evaluatedAmount = remember(amount) {
        MathEvaluator.evaluate(amount)
    }

    val hasOperators = remember(amount) {
        amount.any { it == '+' || it == '-' || it == '*' || it == '/' }
    }

    val quickCategories = remember(transactionType, existingCategories) {
        if (transactionType == "INCOME") {
            listOf("Salary", "Freelance", "Investment", "Tuition", "Gift", "Other")
        } else {
            (existingCategories.filter { it != "Salary" && it != "Freelance" && it != "Investment" && it != "Tuition" } + listOf("Food", "Shopping", "Bills", "Travel", "Other")).distinct()
        }
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
                    text = "Add Transaction",
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
                
                // Amount Input (with live math calculator preview)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth().testTag("amount_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = CardSurface,
                            focusedLabelColor = PrimaryAccent,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = PrimaryAccent
                        )
                    )
                    if (evaluatedAmount != null && hasOperators) {
                        Text(
                            text = String.format(java.util.Locale.US, "= ৳%,.2f", evaluatedAmount),
                            color = if (transactionType == "EXPENSE") Color(0xFFEA3B35) else Color(0xFF4CAF50),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().testTag("description_input"),
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
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Enter Category") },
                    placeholder = { Text("e.g. Food, Salary, Bills") },
                    modifier = Modifier.fillMaxWidth().testTag("category_input"),
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
                    Text(
                        text = "Quick Select Category",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )

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

                // Account Dropdown Selector (Moved to bottom, replacing Tags!)
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
                            val finalAmt = evaluatedAmount ?: amount.toDoubleOrNull() ?: 0.0
                            if (finalAmt > 0) {
                                val finalCategory = if (category.trim().isEmpty()) "Other" else category.trim()
                                onConfirm(finalAmt, description, finalCategory, transactionType, selectedAccountId, null, "")
                            }
                        },
                        modifier = Modifier.testTag("save_expense_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (transactionType == "EXPENSE") Color(0xFFEA3B35) else Color(0xFF4CAF50),
                            contentColor = Color.White
                        ),
                        enabled = evaluatedAmount != null && evaluatedAmount > 0
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
