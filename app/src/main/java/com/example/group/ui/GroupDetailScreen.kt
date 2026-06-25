package com.example.group.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.group.data.entity.*
import com.example.group.viewmodel.GroupViewModel
import com.example.group.calculator.SettlementGenerator
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Int,
    viewModel: GroupViewModel,
    onBack: () -> Unit
) {
    val group by viewModel.selectedGroup.collectAsState()
    val members by viewModel.selectedGroupMembers.collectAsState()
    val expenses by viewModel.selectedGroupExpenses.collectAsState()
    val settlements by viewModel.selectedGroupSettlements.collectAsState()
    val balances by viewModel.selectedGroupBalances.collectAsState()
    val suggestions by viewModel.selectedGroupSuggestedSettlements.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Expenses, 1 = Balances, 2 = Members, 3 = Analytics/Settings
    
    var showAddExpense by remember { mutableStateOf(false) }
    var editExpenseTarget by remember { mutableStateOf<GroupExpenseEntity?>(null) }
    var showSettleDialog by remember { mutableStateOf(false) }
    var settlePayerId by remember { mutableStateOf<Int?>(null) }
    var settlePayeeId by remember { mutableStateOf<Int?>(null) }
    var settleAmount by remember { mutableStateOf(0.0) }

    val showAddExpenseTrigger by viewModel.showAddExpenseDialog.collectAsState()
    LaunchedEffect(showAddExpenseTrigger) {
        if (showAddExpenseTrigger) {
            showAddExpense = true
            viewModel.setShowAddExpenseDialog(false)
        }
    }

    LaunchedEffect(groupId) {
        viewModel.selectGroup(groupId)
    }

    if (group == null) {
        Box(modifier = Modifier.fillMaxSize().background(ThemeBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryAccent)
        }
        return
    }

    val currentGroup = group!!

    if (showAddExpense) {
        AddGroupExpenseScreen(
            groupId = groupId,
            expenseToEdit = null,
            viewModel = viewModel,
            onDismiss = { showAddExpense = false }
        )
        return
    }

    if (editExpenseTarget != null) {
        AddGroupExpenseScreen(
            groupId = groupId,
            expenseToEdit = editExpenseTarget,
            viewModel = viewModel,
            onDismiss = { editExpenseTarget = null }
        )
        return
    }

    Scaffold(
        containerColor = ThemeBackground,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeBackground, titleContentColor = TextPrimary),
                actions = {
                    IconButton(onClick = { showSettleDialog = true }) {
                        Icon(imageVector = Icons.Rounded.Handshake, contentDescription = "Settle Up", tint = PrimaryAccent)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentGroup.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text("Transactions", color = TextSecondary, fontSize = 12.sp)
                        Text("${expenses.size}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column {
                        Text("Members", color = TextSecondary, fontSize = 12.sp)
                        Text("${members.size}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column {
                        Text("Total Spent", color = TextSecondary, fontSize = 12.sp)
                        val totalSpent = expenses.sumOf { it.amount }
                        val formattedSpent = if (totalSpent % 1.0 == 0.0) {
                            String.format(Locale.getDefault(), "%,.0f", totalSpent)
                        } else {
                            String.format(Locale.getDefault(), "%,.2f", totalSpent)
                        }
                        Text("${currentGroup.currency} $formattedSpent", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Highlight: Who should pay (largest negative balance)
                    val debtor = members.map { it to (balances[it.id] ?: 0.0) }
                        .filter { it.second < 0.0 }
                        .minByOrNull { it.second } // lowest balance is the one owing the most

                    if (debtor != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = debtor.first.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = " owes the most",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Text(
                            text = "Everyone is settled up",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { activeTab = 3 }, // Go to More/Settings tab where spend analytics is located
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CardSurface,
                                contentColor = TextPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ShowChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Show charts", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        var showMoreMenu by remember { mutableStateOf(false) }
                        Box {
                            Button(
                                onClick = { showMoreMenu = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CardSurface,
                                    contentColor = TextPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.width(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "More options",
                                    modifier = Modifier.size(16.dp),
                                    tint = TextPrimary
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                modifier = Modifier.background(CardSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Record Settlement", color = TextPrimary) },
                                    onClick = {
                                        showMoreMenu = false
                                        showSettleDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export to Excel", color = TextPrimary) },
                                    onClick = {
                                        showMoreMenu = false
                                        activeTab = 3 // Go to Settings tab to export
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Row selectors
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = ThemeBackground,
                contentColor = PrimaryAccent
            ) {
                listOf("Expenses", "Balances", "Members", "More").forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        selectedContentColor = PrimaryAccent,
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut()
                        }
                    },
                    label = "tabChange",
                    modifier = Modifier.fillMaxSize()
                ) { targetTab ->
                    when (targetTab) {
                        0 -> ExpensesTab(
                            expenses = expenses,
                            members = members,
                            balances = balances,
                            onAddExpenseClick = { showAddExpense = true },
                            onExpenseClick = { editExpenseTarget = it },
                            onDeleteExpense = { viewModel.deleteExpense(it) },
                            groupCurrency = currentGroup.currency,
                            onBubbleClick = { activeTab = 1 }
                        )
                        1 -> BalancesTab(
                            members = members,
                            balances = balances,
                            suggestions = suggestions,
                            groupCurrency = currentGroup.currency,
                            onSettleClick = { debtor, creditor, amt ->
                                settlePayerId = debtor
                                settlePayeeId = creditor
                                settleAmount = amt
                                showSettleDialog = true
                            }
                        )
                        2 -> MembersTab(
                            members = members,
                            groupId = groupId,
                            viewModel = viewModel
                        )
                        3 -> SettingsTab(
                            group = currentGroup,
                            expenses = expenses,
                            settlements = settlements,
                            members = members,
                            viewModel = viewModel,
                            onBack = onBack
                        )
                    }
                }
            }
        }
    }

    if (showSettleDialog) {
        RecordSettlementDialog(
            members = members,
            preFilledPayerId = settlePayerId,
            preFilledPayeeId = settlePayeeId,
            preFilledAmount = settleAmount,
            groupCurrency = currentGroup.currency,
            onDismiss = {
                showSettleDialog = false
                settlePayerId = null
                settlePayeeId = null
                settleAmount = 0.0
            },
            onConfirm = { payerId, payeeId, amount, date, notes ->
                viewModel.addSettlement(groupId, payerId, payeeId, amount, date, notes)
                showSettleDialog = false
            }
        )
    }
}

@Composable
fun ExpensesTab(
    expenses: List<GroupExpenseEntity>,
    members: List<MemberEntity>,
    balances: Map<Int, Double>,
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (GroupExpenseEntity) -> Unit,
    onDeleteExpense: (GroupExpenseEntity) -> Unit,
    groupCurrency: String,
    onBubbleClick: () -> Unit
) {
    if (expenses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Rounded.ReceiptLong, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                Text("No group expenses logged yet", color = TextSecondary, fontSize = 15.sp)
                Button(onClick = onAddExpenseClick, colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)) {
                    Text("Add Group Expense", color = DarkCardTextPrimary)
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            GroupBalanceBubbles(
                members = members,
                balances = balances,
                groupCurrency = groupCurrency,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                isCompact = false,
                onBubbleClick = onBubbleClick
            )

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${expenses.size} Expenses", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                TextButton(onClick = onAddExpenseClick) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrimaryAccent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Expense", color = PrimaryAccent)
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(expenses, key = { it.id }) { expense ->
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onExpenseClick(expense) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(ThemeBackground), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (expense.category.lowercase()) {
                                            "food" -> Icons.Rounded.Fastfood
                                            "transport" -> Icons.Rounded.DirectionsCar
                                            "accommodation" -> Icons.Rounded.Hotel
                                            "entertainment" -> Icons.Rounded.LocalPlay
                                            else -> Icons.Rounded.Category
                                        },
                                        contentDescription = null,
                                        tint = PrimaryAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = expense.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(expense.date)),
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${groupCurrency} ${String.format("%.2f", expense.amount)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Expense?") },
                            text = { Text("Are you sure you want to delete '${expense.title}'? This cannot be undone.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    onDeleteExpense(expense)
                                    showDeleteDialog = false
                                }) { Text("Delete", color = Color.Red) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = TextSecondary) }
                            },
                            containerColor = ThemeBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupBalanceBubbles(
    members: List<MemberEntity>,
    balances: Map<Int, Double>,
    groupCurrency: String,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onBubbleClick: () -> Unit = {}
) {
    // Filter members with non-zero balances
    val activeBalances = members.map { it to (balances[it.id] ?: 0.0) }
        .filter { it.second != 0.0 }
        .sortedByDescending { Math.abs(it.second) }

    if (activeBalances.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(if (isCompact) 140.dp else 180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardSurface)
                .clickable { onBubbleClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("Everyone is settled up", color = TextSecondary, fontSize = 14.sp)
        }
        return
    }

    val maxAbs = activeBalances.maxOf { Math.abs(it.second) }
    val displayLimit = if (isCompact) 4 else 5
    val visibleBubbles = activeBalances.take(displayLimit)
    val remainingCount = activeBalances.size - displayLimit

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isCompact) 200.dp else 280.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        val bubblePositions = if (isCompact) {
            listOf(
                Pair(0, 0),        // Center
                Pair(-45, 45),     // Bottom-Left
                Pair(-35, -45),    // Top-Left
                Pair(45, -35),     // Top-Right
                Pair(50, 15),      // Middle-Right
                Pair(30, 50)       // Bottom-Right
            )
        } else {
            listOf(
                Pair(0, 0),        // Center
                Pair(-75, 65),     // Bottom-Left
                Pair(-60, -65),    // Top-Left
                Pair(80, -55),     // Top-Right
                Pair(95, 25),      // Middle-Right
                Pair(50, 80)       // Bottom-Right
            )
        }

        visibleBubbles.forEachIndexed { index, (member, bal) ->
            val absBal = Math.abs(bal)
            val pct = if (maxAbs > 0.0) absBal / maxAbs else 1.0
            val minSize = if (isCompact) 60 else 80
            val maxSizeDiff = if (isCompact) 40 else 60
            val targetSize = (minSize + (pct * maxSizeDiff)).toInt().dp
            val pos = bubblePositions.getOrElse(index) { Pair(0, 0) }

            // Spring animated size and positions
            val sizeDp by animateDpAsState(
                targetValue = targetSize,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "size"
            )
            val animX by animateDpAsState(
                targetValue = pos.first.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "x"
            )
            val animY by animateDpAsState(
                targetValue = pos.second.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "y"
            )

            val bubbleBg = if (bal < 0.0) {
                Color(0xFFE28C38).copy(alpha = 0.85f)
            } else {
                Color(0xFF2E7D32).copy(alpha = 0.65f)
            }

            Box(
                modifier = Modifier
                    .offset(x = animX, y = animY)
                    .size(sizeDp)
                    .clip(CircleShape)
                    .background(bubbleBg)
                    .clickable { onBubbleClick() }
                    .padding(if (isCompact) 4.dp else 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                        text = member.name,
                        color = Color.White,
                        fontSize = if (isCompact) (8 + (pct * 2)).toInt().sp else (11 + (pct * 3)).toInt().sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val formatted = if (absBal % 1.0 == 0.0) {
                        String.format(Locale.getDefault(), "%,.0f", absBal)
                    } else {
                        String.format(Locale.getDefault(), "%,.2f", absBal)
                    }
                    val balText = if (bal < 0.0) "-$groupCurrency$formatted" else "+$groupCurrency$formatted"

                    Text(
                        text = balText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = if (isCompact) (7 + (pct * 2)).toInt().sp else (10 + (pct * 3)).toInt().sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (bal < 0.0 && pct > 0.4 && !isCompact) {
                        Text(
                            text = "should pay",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }

        if (remainingCount > 0) {
            val pos = bubblePositions.getOrElse(visibleBubbles.size) { Pair(0, 0) }
            val targetSize = if (isCompact) 36.dp else 50.dp

            val sizeDp by animateDpAsState(
                targetValue = targetSize,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "remSize"
            )
            val animX by animateDpAsState(
                targetValue = pos.first.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "remX"
            )
            val animY by animateDpAsState(
                targetValue = pos.second.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "remY"
            )

            Box(
                modifier = Modifier
                    .offset(x = animX, y = animY)
                    .size(sizeDp)
                    .clip(CircleShape)
                    .background(Color(0xFFE28C38).copy(alpha = 0.6f))
                    .clickable { onBubbleClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$remainingCount",
                    color = Color.White,
                    fontSize = if (isCompact) 11.sp else 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BalancesTab(
    members: List<MemberEntity>,
    balances: Map<Int, Double>,
    suggestions: List<SettlementGenerator.SuggestedSettlement>,
    groupCurrency: String,
    onSettleClick: (debtor: Int, creditor: Int, amount: Double) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Members Balances Card list
        item {
            Text("Balances", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        items(members) { member ->
            val bal = balances[member.id] ?: 0.0
            val formattedBal = String.format("%.2f", bal)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardSurface)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(ThemeBackground), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Rounded.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    Text(text = member.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (bal > 0.0) {
                        Text(text = "is owed", fontSize = 11.sp, color = TextSecondary)
                        Text(text = "+${groupCurrency} $formattedBal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    } else if (bal < 0.0) {
                        Text(text = "owes", fontSize = 11.sp, color = TextSecondary)
                        Text(text = "-${groupCurrency} ${formattedBal.substring(1)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA3B35))
                    } else {
                        Text(text = "settled up", fontSize = 11.sp, color = TextSecondary)
                        Text(text = "${groupCurrency} 0.00", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    }
                }
            }
        }

        // Suggested Settlements Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Suggested Settlements", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        if (suggestions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Rounded.DoneAll, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(36.dp))
                        Text("Everything settled up!", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    }
                }
            }
        } else {
            items(suggestions) { sug ->
                val debtor = members.find { it.id == sug.debtorId }?.name ?: "Unknown"
                val creditor = members.find { it.id == sug.creditorId }?.name ?: "Unknown"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = debtor, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.padding(horizontal = 6.dp).size(14.dp))
                                Text(text = creditor, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Pay ${groupCurrency} ${String.format("%.2f", sug.amount)}", fontSize = 13.sp, color = TextSecondary)
                        }

                        Button(
                            onClick = { onSettleClick(sug.debtorId, sug.creditorId, sug.amount) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Settle", color = DarkCardTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MembersTab(
    members: List<MemberEntity>,
    groupId: Int,
    viewModel: GroupViewModel
) {
    var showAddMember by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Group Members (${members.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            TextButton(onClick = { showAddMember = true }) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrimaryAccent)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Member", color = PrimaryAccent)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(members) { member ->
                var showMbrDetailDialog by remember { mutableStateOf(false) }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .clickable { showMbrDetailDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(ThemeBackground), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Rounded.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(text = member.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            if (!member.notes.isNullOrEmpty()) {
                                Text(text = member.notes, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                if (showMbrDetailDialog) {
                    var editNotes by remember { mutableStateOf(member.notes ?: "") }
                    
                    AlertDialog(
                        onDismissRequest = { showMbrDetailDialog = false },
                        title = { Text("Edit ${member.name}") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = editNotes,
                                    onValueChange = { editNotes = it },
                                    label = { Text("Notes (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = CardSurface)
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.updateMember(member.copy(notes = editNotes.trim().ifEmpty { null }))
                                showMbrDetailDialog = false
                            }) { Text("Save", color = PrimaryAccent) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showMbrDetailDialog = false }) { Text("Cancel", color = TextSecondary) }
                        },
                        containerColor = ThemeBackground
                    )
                }
            }
        }
    }

    if (showAddMember) {
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text("Add Group Member") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = CardSurface)
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = CardSurface)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            viewModel.addMember(groupId, nameInput.trim(), notesInput.trim().ifEmpty { null }, 1.0)
                            nameInput = ""
                            notesInput = ""
                            showAddMember = false
                        }
                    },
                    enabled = nameInput.isNotBlank()
                ) { Text("Add", color = PrimaryAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showAddMember = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = ThemeBackground
        )
    }
}

@Composable
fun SettingsTab(
    group: GroupEntity,
    expenses: List<GroupExpenseEntity>,
    settlements: List<SettlementEntity>,
    members: List<MemberEntity>,
    viewModel: GroupViewModel,
    onBack: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    com.example.data.ExcelHelper.exportGroupExpenses(
                        outputStream,
                        group,
                        expenses,
                        members
                    )
                }
                Toast.makeText(context, "Group expenses exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Analytics Box (Simple Category Spends column)
        item {
            Text("Spend Analytics", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        val totalSpend = expenses.sumOf { it.amount }
        if (totalSpend > 0.0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Total Group Spending", fontSize = 13.sp, color = TextSecondary)
                        Text(text = "${group.currency} ${String.format("%.2f", totalSpend)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Category breakout
                        val catGroups = expenses.groupBy { it.category }
                        catGroups.forEach { (cat, list) ->
                            val amt = list.sumOf { it.amount }
                            val pct = (amt / totalSpend * 100).toInt()
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PrimaryAccent))
                                    Text(text = cat, fontSize = 14.sp, color = TextPrimary)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(text = "${group.currency} ${String.format("%.2f", amt)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(text = "$pct%", fontSize = 14.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardSurface).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No expenses registered to analyze", color = TextSecondary, fontSize = 14.sp)
                }
            }
        }

        // Settlement History
        item {
            Text("Settlement History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        if (settlements.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardSurface).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No settlements logged yet", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            items(settlements, key = { it.id }) { setl ->
                val payer = members.find { it.id == setl.payerId }?.name ?: "Unknown"
                val payee = members.find { it.id == setl.payeeId }?.name ?: "Unknown"
                var showDelSetConfirm by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "$payer paid $payee", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "${group.currency} ${String.format("%.2f", setl.amount)} • ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(setl.date))}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = { showDelSetConfirm = true }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                }

                if (showDelSetConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDelSetConfirm = false },
                        title = { Text("Delete Settlement?") },
                        text = { Text("Are you sure you want to delete this settlement record?") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deleteSettlement(setl)
                                showDelSetConfirm = false
                            }) { Text("Delete", color = Color.Red) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDelSetConfirm = false }) { Text("Cancel", color = TextSecondary) }
                        },
                        containerColor = ThemeBackground
                    )
                }
            }
        }

        // Export Group Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Export Options", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Save all group expenses as an Excel sheet for external viewing or backup.", fontSize = 13.sp, color = TextSecondary)
                    Button(
                        onClick = {
                            val fileName = "${group.name.replace(" ", "_")}_expenses.xls"
                            exportLauncher.launch(fileName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export to Excel (.xls)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Archive / Delete group settings
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Danger Zone", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = if (group.isArchived) "Unarchive Group" else "Archive Group", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(text = "Temporarily hide this group from active view", fontSize = 12.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = group.isArchived,
                            onCheckedChange = { viewModel.toggleGroupArchive(group) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryAccent)
                        )
                    }

                    HorizontalDivider(color = ThemeBackground)

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showDeleteConfirm = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Delete Group Permanently", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Red)
                            Text(text = "Erase all expenses, participants, and settlements forever", fontSize = 12.sp, color = TextSecondary)
                        }
                        Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = TextSecondary)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Group?") },
            text = { Text("Are you sure you want to permanently delete '${group.name}'? This action is destructive and cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup(group)
                    showDeleteConfirm = false
                    onBack()
                }) { Text("Delete Group", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = ThemeBackground
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSettlementDialog(
    members: List<MemberEntity>,
    preFilledPayerId: Int?,
    preFilledPayeeId: Int?,
    preFilledAmount: Double,
    groupCurrency: String,
    onDismiss: () -> Unit,
    onConfirm: (payerId: Int, payeeId: Int, amount: Double, date: Long, notes: String?) -> Unit
) {
    var payerId by remember { mutableStateOf(preFilledPayerId ?: members.firstOrNull()?.id) }
    var payeeId by remember { mutableStateOf(preFilledPayeeId ?: members.getOrNull(1)?.id ?: members.firstOrNull()?.id) }
    var amount by remember { mutableStateOf(if (preFilledAmount > 0.0) preFilledAmount.toString() else "") }
    var notes by remember { mutableStateOf("") }

    var expandedPayer by remember { mutableStateOf(false) }
    var expandedPayee by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeBackground)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Record Settlement", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                // Payer Dropdown selector
                Column {
                    Text("Who Sent Money", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardSurface)
                                .clickable { expandedPayer = true }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = members.find { it.id == payerId }?.name ?: "Select Sender",
                                color = TextPrimary
                            )
                        }
                        DropdownMenu(expanded = expandedPayer, onDismissRequest = { expandedPayer = false }, modifier = Modifier.background(CardSurface)) {
                            members.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.name, color = TextPrimary) },
                                    onClick = {
                                        payerId = m.id
                                        expandedPayer = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Payee Dropdown selector
                Column {
                    Text("Who Received Money", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardSurface)
                                .clickable { expandedPayee = true }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = members.find { it.id == payeeId }?.name ?: "Select Receiver",
                                color = TextPrimary
                            )
                        }
                        DropdownMenu(expanded = expandedPayee, onDismissRequest = { expandedPayee = false }, modifier = Modifier.background(CardSurface)) {
                            members.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.name, color = TextPrimary) },
                                    onClick = {
                                        payeeId = m.id
                                        expandedPayee = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount Text Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount ($groupCurrency)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = CardSurface),
                    singleLine = true
                )

                // Notes Text Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = CardSurface),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (payerId != null && payeeId != null && payerId != payeeId && amt > 0.0) {
                                onConfirm(payerId!!, payeeId!!, amt, System.currentTimeMillis(), notes.trim().ifEmpty { null })
                            }
                        },
                        enabled = payerId != null && payeeId != null && payerId != payeeId && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Text("Record", color = DarkCardTextPrimary)
                    }
                }
            }
        }
    }
}
