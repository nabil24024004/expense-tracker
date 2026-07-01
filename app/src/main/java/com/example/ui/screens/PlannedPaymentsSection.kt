package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Account
import com.example.data.PlannedTransaction
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlannedPaymentsSection(
    plannedTransactions: List<PlannedTransaction>,
    accounts: List<Account>,
    onPayClick: (PlannedTransaction) -> Unit,
    onSkipClick: (PlannedTransaction) -> Unit,
    onAddPlannedClick: () -> Unit,
    onPlannedClick: (PlannedTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePlanned = remember(plannedTransactions) {
        plannedTransactions.filter { it.isActive }
    }

    val now = System.currentTimeMillis()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(24.dp))
            .border(1.dp, CardSurface.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.EventRepeat,
                    contentDescription = "Planned Expenses",
                    tint = PrimaryAccent,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Planned Expenses",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                )
            }

            IconButton(
                onClick = onAddPlannedClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = PrimaryAccent.copy(alpha = 0.1f),
                    contentColor = PrimaryAccent
                ),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Planned Expense",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (activePlanned.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No upcoming expenses scheduled.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activePlanned.take(3).forEach { planned ->
                    val isOverdue = planned.nextDueDate < now
                    val accountName = accounts.find { it.id == planned.accountId }?.name ?: "Unassigned"
                    
                    PlannedTransactionItem(
                        planned = planned,
                        accountName = accountName,
                        isOverdue = isOverdue,
                        onPay = { onPayClick(planned) },
                        onSkip = { onSkipClick(planned) },
                        onClick = { onPlannedClick(planned) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlannedTransactionItem(
    planned: PlannedTransaction,
    accountName: String,
    isOverdue: Boolean,
    onPay: () -> Unit,
    onSkip: () -> Unit,
    onClick: () -> Unit
) {
    val dateString = remember(planned.nextDueDate) {
        val formatter = SimpleDateFormat("dd MMM, yyyy", Locale.US)
        formatter.format(Date(planned.nextDueDate))
    }

    val ruleString = remember(planned.intervalType, planned.intervalN, planned.oneTime) {
        if (planned.oneTime) {
            "One-time"
        } else {
            val typeStr = when (planned.intervalType) {
                "DAY" -> "day"
                "WEEK" -> "week"
                "MONTH" -> "month"
                "YEAR" -> "year"
                else -> "month"
            }
            if (planned.intervalN == 1) "Every $typeStr" else "Every ${planned.intervalN} ${typeStr}s"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardSurface
        ),
        border = BorderStroke(1.dp, CardSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = planned.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkCardTextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isOverdue) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEA3B35).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Overdue",
                                    color = Color(0xFFEA3B35),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Due $dateString • $ruleString",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DarkCardTextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = String.format(java.util.Locale.US, "৳%,.2f", planned.amount),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = if (planned.type == "EXPENSE") Color(0xFFEA3B35) else Color(0xFF4CAF50)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Account tag badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardSurface)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = accountName,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (planned.type == "EXPENSE") {
                    if (isOverdue) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onSkip,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                            ) {
                                Text("Skip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onPay,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEA3B35),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Upcoming",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Auto-deposit",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}
