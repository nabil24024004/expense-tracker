package com.neosparkx.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
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
import com.neosparkx.expensetracker.data.Account
import com.neosparkx.expensetracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    accountToEdit: Account? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, balance: Double, colorHex: String, icon: String, includeInBalance: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var balanceStr by remember { mutableStateOf(accountToEdit?.balance?.toString() ?: "") }
    
    // Parse existing icon into type and subtype
    val parsedType = remember(accountToEdit) {
        if (accountToEdit != null) {
            parseIconString(accountToEdit.icon)
        } else {
            "wallet" to "standard"
        }
    }

    var selectedType by remember { mutableStateOf(parsedType.first) }
    var selectedSubtype by remember { mutableStateOf(parsedType.second) }
    var bankName by remember { mutableStateOf(accountToEdit?.icon?.substringAfter(":", "") ?: "") }
    
    var selectedColorHex by remember { mutableStateOf(accountToEdit?.colorHex ?: "#EA3B35") }
    var includeInBalance by remember { mutableStateOf(accountToEdit?.includeInBalance ?: true) }

    val colorsPalette = listOf(
        "#EA3B35", // Red
        "#4CAF50", // Green
        "#2196F3", // Blue
        "#FF9800", // Orange
        "#9C27B0", // Purple
        "#009688", // Teal
        "#3F51B5", // Indigo
        "#767677"  // Grey
    )

    val mainTypes = listOf(
        "wallet" to "Cash",
        "bank" to "Bank",
        "card" to "Card",
        "savings" to "Savings"
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
                    text = if (accountToEdit == null) "Create Account" else "Edit Account",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
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
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Balance (৳)") },
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

                Text(
                    text = "Account Type",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )

                // Main Type Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mainTypes.forEach { (typeKey, label) ->
                        val isSelected = selectedType == typeKey
                        val chipBg = if (isSelected) PrimaryAccent else CardSurface
                        val chipText = if (isSelected) Color.White else TextPrimary
                        val iconVector = getAccountIcon(typeKey)
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBg)
                                .clickable { 
                                    selectedType = typeKey
                                    // Reset default subtype for selected type
                                    selectedSubtype = "standard"
                                }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = label,
                                tint = chipText,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = label,
                                color = chipText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Subtype Options Panel (Animate content or conditional spacing)
                if (selectedType == "bank") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Bank Type",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "standard" to "Traditional Bank",
                                "mobile" to "Mobile Banking"
                            ).forEach { (subTypeKey, subTypeLabel) ->
                                val isSubtypeGroupSelected = if (subTypeKey == "standard") selectedSubtype == "standard" else selectedSubtype != "standard"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSubtypeGroupSelected) PrimaryAccent else Color.Transparent)
                                        .clickable { 
                                            selectedSubtype = if (subTypeKey == "standard") "standard" else "bkash" 
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = subTypeLabel,
                                        color = if (isSubtypeGroupSelected) Color.White else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Mobile Banking Providers
                        if (selectedSubtype != "standard") {
                            Text(
                                text = "Mobile Provider",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("bkash" to "bKash", "nagad" to "Nagad", "rocket" to "Rocket").forEach { (providerKey, providerLabel) ->
                                    val isProviderSelected = selectedSubtype == providerKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isProviderSelected) PrimaryAccent.copy(alpha = 0.15f) else CardSurface)
                                            .border(
                                                width = if (isProviderSelected) 1.dp else 0.dp,
                                                color = if (isProviderSelected) PrimaryAccent else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedSubtype = providerKey }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = providerLabel,
                                            color = if (isProviderSelected) PrimaryAccent else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedType == "card") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Card Network",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("visa" to "Visa", "mastercard" to "Mastercard", "amex" to "Amex").forEach { (cardKey, cardLabel) ->
                                val isCardSelected = selectedSubtype == cardKey || (selectedSubtype == "standard" && cardKey == "visa")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCardSelected) PrimaryAccent.copy(alpha = 0.15f) else CardSurface)
                                        .border(
                                            width = if (isCardSelected) 1.dp else 0.dp,
                                            color = if (isCardSelected) PrimaryAccent else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedSubtype = cardKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cardLabel,
                                        color = if (isCardSelected) PrimaryAccent else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedType == "savings") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Savings Scheme",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        )
                        val savingsOptions = listOf(
                            "fd" to "Fixed Deposit",
                            "pf" to "Provident Fund",
                            "pension" to "Pension",
                            "mf" to "Mutual Fund"
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            savingsOptions.take(2).forEach { (saveKey, saveLabel) ->
                                val isSaveSelected = selectedSubtype == saveKey || (selectedSubtype == "standard" && saveKey == "fd")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSaveSelected) PrimaryAccent.copy(alpha = 0.15f) else CardSurface)
                                        .border(
                                            width = if (isSaveSelected) 1.dp else 0.dp,
                                            color = if (isSaveSelected) PrimaryAccent else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedSubtype = saveKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = saveLabel,
                                        color = if (isSaveSelected) PrimaryAccent else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            savingsOptions.drop(2).forEach { (saveKey, saveLabel) ->
                                val isSaveSelected = selectedSubtype == saveKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSaveSelected) PrimaryAccent.copy(alpha = 0.15f) else CardSurface)
                                        .border(
                                            width = if (isSaveSelected) 1.dp else 0.dp,
                                            color = if (isSaveSelected) PrimaryAccent else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedSubtype = saveKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = saveLabel,
                                        color = if (isSaveSelected) PrimaryAccent else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                val showBankNameField = (selectedType == "bank" && selectedSubtype == "standard") || (selectedType == "card")
                if (showBankNameField) {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name (e.g. City Bank, HSBC)") },
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
                }

                Text(
                    text = "Theme Color",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorsPalette.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColorHex = hex }
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) TextPrimary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = if (hex == "#FFFFFF") Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

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
                            text = "Include in Net Balance",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Show on dashboard total balance summary",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                    Switch(
                        checked = includeInBalance,
                        onCheckedChange = { includeInBalance = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryAccent,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CardSurface
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (accountToEdit != null && accountToEdit.id != 1 && onDelete != null) {
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
                                val bal = balanceStr.toDoubleOrNull() ?: 0.0
                                if (name.trim().isNotEmpty()) {
                                    val finalBankName = if (showBankNameField) bankName.trim() else ""
                                    val iconString = getIconString(selectedType, selectedSubtype, finalBankName)
                                    onConfirm(name.trim(), bal, selectedColorHex, iconString, includeInBalance)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryAccent,
                                contentColor = Color.White
                            ),
                            enabled = name.trim().isNotEmpty()
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Helpers for icon string serialization
fun getIconString(type: String, subType: String, bankName: String): String {
    val base = when (type) {
        "wallet" -> "wallet"
        "bank" -> {
            if (subType == "standard") "bank" else "mobile_$subType"
        }
        "card" -> {
            if (subType == "standard") "card_visa" else "card_$subType"
        }
        "savings" -> {
            if (subType == "standard") "savings_fd" else "savings_$subType"
        }
        else -> "wallet"
    }
    return if (bankName.isNotEmpty()) {
        "$base:$bankName"
    } else {
        base
    }
}

fun parseIconString(icon: String): Pair<String, String> {
    val baseIcon = icon.substringBefore(":")
    return when {
        baseIcon == "wallet" -> "wallet" to "standard"
        baseIcon == "bank" -> "bank" to "standard"
        baseIcon.startsWith("mobile_") -> "bank" to baseIcon.substringAfter("mobile_")
        baseIcon.startsWith("card_") -> "card" to baseIcon.substringAfter("card_")
        baseIcon.startsWith("savings_") -> "savings" to baseIcon.substringAfter("savings_")
        else -> "wallet" to "standard"
    }
}

