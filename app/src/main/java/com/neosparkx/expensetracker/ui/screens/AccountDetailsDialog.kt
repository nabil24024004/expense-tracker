package com.neosparkx.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.neosparkx.expensetracker.data.Account
import com.neosparkx.expensetracker.data.Expense
import com.neosparkx.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsDialog(
    account: Account,
    allExpenses: List<Expense>,
    onDismiss: () -> Unit,
    onEditAccount: () -> Unit,
    onDeleteTransaction: (Int) -> Unit
) {
    // Filter transactions belonging to this account
    val accountExpenses = remember(account.id, allExpenses) {
        allExpenses.filter { 
            it.accountId == account.id || (it.type == "TRANSFER" && it.toAccountId == account.id)
        }.sortedByDescending { it.date }
    }

    // Calculate Inflow & Outflow relative to this account
    val totalInflow = remember(accountExpenses, account.id) {
        accountExpenses.sumOf { exp ->
            when {
                exp.type == "INCOME" && exp.accountId == account.id -> exp.amount
                exp.type == "TRANSFER" && exp.toAccountId == account.id -> exp.amount
                else -> 0.0
            }
        }
    }

    val totalOutflow = remember(accountExpenses, account.id) {
        accountExpenses.sumOf { exp ->
            when {
                exp.type == "EXPENSE" && exp.accountId == account.id -> exp.amount
                exp.type == "TRANSFER" && exp.accountId == account.id -> exp.amount
                else -> 0.0
            }
        }
    }

    val accountColor = remember(account.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(account.colorHex))
        } catch (e: Exception) {
            PrimaryAccent
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ThemeBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = CardSurface)
                    ) {
                        Icon(Icons.Rounded.ArrowBack, "Back", tint = TextPrimary)
                    }

                    Text(
                        text = "Account Details",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    IconButton(
                        onClick = onEditAccount,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = CardSurface)
                    ) {
                        Icon(Icons.Rounded.Edit, "Edit", tint = TextPrimary)
                    }
                }

                // Account card display
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(1.dp, CardSurface, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(accountColor.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getAccountIcon(account.icon),
                                        contentDescription = account.name,
                                        tint = accountColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = account.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = DarkCardTextPrimary
                                    )
                                    val sub = getSubtypeDisplayLabel(account.icon)
                                    if (sub.isNotEmpty()) {
                                        Text(
                                            text = sub,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DarkCardTextSecondary.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            if (!account.includeInBalance) {
                                Badge(
                                    containerColor = CardSurface,
                                    contentColor = TextSecondary
                                ) {
                                    Text("Excluded", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "CURRENT BALANCE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = DarkCardTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "৳%,.2f", account.balance),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = DarkCardTextPrimary
                                )
                            )
                        }
                    }
                }

                // Inflow/Outflow stats summary row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, "Inflow", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("Inflow", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(String.format(Locale.US, "৳%,.0f", totalInflow), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF4CAF50))
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFEA3B35).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, "Outflow", tint = Color(0xFFEA3B35), modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("Outflow", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(String.format(Locale.US, "৳%,.0f", totalOutflow), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFEA3B35))
                            }
                        }
                    }
                }

                // Transaction history title
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // List of filtered transactions
                if (accountExpenses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions recorded for this account.",
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(accountExpenses) { expense ->
                            AccountTransactionItem(
                                expense = expense,
                                currentAccountId = account.id,
                                onDelete = { onDeleteTransaction(expense.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountTransactionItem(
    expense: Expense,
    currentAccountId: Int,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.US) }
    val dateStr = remember(expense.date) { dateFormat.format(Date(expense.date)) }
    val style = getCategoryStyle(expense.category)

    // Determine type relative to the current account
    val isRelativeIncome = remember(expense, currentAccountId) {
        when {
            expense.type == "INCOME" -> true
            expense.type == "TRANSFER" && expense.toAccountId == currentAccountId -> true
            else -> false
        }
    }

    val amountColor = if (isRelativeIncome) Color(0xFF4CAF50) else Color(0xFFEA3B35)
    val amountPrefix = if (isRelativeIncome) "+" else "-"

    val displayTitle = remember(expense) {
        if (expense.type == "TRANSFER") {
            "Transfer"
        } else {
            expense.description.ifEmpty { expense.category }
        }
    }

    val displaySubtitle = remember(expense) {
        if (expense.type == "TRANSFER") {
            val toMe = expense.toAccountId == currentAccountId
            if (toMe) "Received from Transfer" else "Transferred out"
        } else {
            expense.category
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ThemeBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (expense.type == "TRANSFER") Icons.Rounded.SwapHoriz else style.first,
                    contentDescription = expense.category,
                    tint = if (expense.type == "TRANSFER") PrimaryAccent else style.third,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dateStr • $displaySubtitle",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Text(
                text = "$amountPrefix৳${String.format(Locale.US, "%,.2f", expense.amount)}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = amountColor
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = PrimaryAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

