package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Account
import com.example.ui.theme.*

@Composable
fun AccountsSection(
    accounts: List<Account>,
    hideBalance: Boolean,
    onAddAccountClick: () -> Unit,
    onAccountClick: (Account) -> Unit,
    onTransferClick: () -> Unit,
    onViewAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(24.dp))
            .border(1.dp, CardSurface.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Accounts",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Add or customize your bank, cash or other accounts",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onTransferClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = PrimaryAccent.copy(alpha = 0.1f),
                        contentColor = PrimaryAccent
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = "Transfer Funds",
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onViewAllClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = PrimaryAccent.copy(alpha = 0.1f),
                        contentColor = PrimaryAccent
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FormatListBulleted,
                        contentDescription = "View All Accounts",
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onAddAccountClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = PrimaryAccent.copy(alpha = 0.1f),
                        contentColor = PrimaryAccent
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Account",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(accounts) { account ->
                AccountCardItem(
                    account = account,
                    hideBalance = hideBalance,
                    onClick = { onAccountClick(account) }
                )
            }

            item {
                AddAccountCard(onClick = onAddAccountClick)
            }
        }
    }
}

@Composable
fun AccountCardItem(
    account: Account,
    hideBalance: Boolean,
    onClick: () -> Unit
) {
    val cardColor = remember(account.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(account.colorHex))
        } catch (e: Exception) {
            PrimaryAccent
        }
    }

    val iconVector = getAccountIcon(account.icon)

    Card(
        modifier = Modifier
            .width(150.dp)
            .height(115.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardSurface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(cardColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = account.name,
                            tint = cardColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    if (!account.includeInBalance) {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = "Excluded",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkCardTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                val sub = getSubtypeDisplayLabel(account.icon)
                if (sub.isNotEmpty()) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = DarkCardTextSecondary.copy(alpha = 0.6f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Dashed Divider
            val dividerLineColor = DarkCardTextSecondary.copy(alpha = 0.2f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            ) {
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                drawLine(
                    color = dividerLineColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    pathEffect = pathEffect,
                    strokeWidth = 1f
                )
            }

            // Bottom Section (Balance)
            Text(
                text = if (hideBalance) "••••" else String.format(java.util.Locale.US, "%s%,.0f", account.currency, account.balance),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = cardColor
                )
            )
        }
    }
}

@Composable
fun AddAccountCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(115.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardSurface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add Account",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add Account",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            )
        }
    }
}

fun getAccountIcon(iconName: String): ImageVector {
    val base = iconName.substringBefore(":")
    return when {
        base == "wallet" -> Icons.Rounded.AccountBalanceWallet
        base == "bank" -> Icons.Rounded.AccountBalance
        base.startsWith("mobile_") -> Icons.Rounded.Smartphone
        base.startsWith("card_") -> Icons.Rounded.CreditCard
        base.startsWith("savings_") -> Icons.Rounded.Savings
        else -> Icons.Rounded.AccountBalanceWallet
    }
}

fun getSubtypeDisplayLabel(icon: String): String {
    val base = icon.substringBefore(":")
    val bankName = icon.substringAfter(":", "")
    val label = when {
        base == "mobile_bkash" -> "bKash"
        base == "mobile_nagad" -> "Nagad"
        base == "mobile_rocket" -> "Rocket"
        base == "card_visa" -> "Visa"
        base == "card_mastercard" -> "Mastercard"
        base == "card_amex" -> "Amex"
        base == "savings_fd" -> "Fixed Deposit"
        base == "savings_pf" -> "Provident Fund"
        base == "savings_pension" -> "Pension"
        base == "savings_mf" -> "Mutual Fund"
        base.startsWith("mobile_") -> "Mobile Banking"
        base.startsWith("card_") -> "Card"
        base.startsWith("savings_") -> "Savings"
        base == "bank" -> "Bank"
        else -> ""
    }
    return if (bankName.isNotEmpty()) {
        if (label.isNotEmpty()) "$label ($bankName)" else bankName
    } else {
        label
    }
}
