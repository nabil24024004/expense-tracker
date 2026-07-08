package com.example.ui.screens

import com.example.data.Account
import com.example.data.PlannedTransaction
import android.widget.Toast
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.MainViewModel
import com.example.data.Expense
import com.example.data.DebtDue
import com.example.data.BudgetPeriodHelper
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.SmoothLineChart
import com.example.ui.theme.*
import com.example.data.ExcelHelper
import com.example.data.PdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

fun copyUriToLocalFile(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, "profile_pic.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun rememberImagePainter(uriString: String?): Bitmap? {
    val context = LocalContext.current
    return remember(uriString) {
        if (uriString.isNullOrEmpty()) null else {
            try {
                val file = File(uriString)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    val uri = Uri.parse(uriString)
                    if (Build.VERSION.SDK_INT < 28) {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    } else {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, activity: FragmentActivity) {
    val expenses by viewModel.expenses.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val profileImageBitmap = rememberImagePainter(profileImageUri)
    val budgetLimit by viewModel.budgetLimit.collectAsState()
    val biometricsEnabled by viewModel.biometricsEnabled.collectAsState()
    val themeSelection by viewModel.themeSelection.collectAsState()
    val notificationsLastViewedTime by viewModel.notificationsLastViewedTime.collectAsState()
    val hasUnreadNotifications = remember(expenses, notificationsLastViewedTime) {
        notificationsLastViewedTime == 0L || expenses.any { it.date > notificationsLastViewedTime }
    }
    
    val budgetPeriodType by viewModel.budgetPeriodType.collectAsState()
    val budgetCustomStartDate by viewModel.budgetCustomStartDate.collectAsState()
    val budgetCustomEndDate by viewModel.budgetCustomEndDate.collectAsState()

    val activePeriodRange = remember(budgetPeriodType, budgetCustomStartDate, budgetCustomEndDate) {
        BudgetPeriodHelper.getPeriodRange(budgetPeriodType, budgetCustomStartDate, budgetCustomEndDate)
    }

    val activePeriodSpent = remember(expenses, activePeriodRange) {
        expenses.filter { it.type == "EXPENSE" && it.date in activePeriodRange.first..activePeriodRange.second }.sumOf { it.amount }
    }

    var currentTab by remember { mutableStateOf("home") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var addDialogPrefillCategory by remember { mutableStateOf("") }
    
    val debtsDues by viewModel.debtsDues.collectAsState()
    var activeHistorySection by remember { mutableStateOf("transactions") }
    var showAddDebtDueDialog by remember { mutableStateOf(false) }
    var showAddChoiceDialog by remember { mutableStateOf(false) }
    
    // Accounts and Planned transactions states
    val accounts by viewModel.accounts.collectAsState()
    val plannedTransactions by viewModel.plannedTransactions.collectAsState()
    val hideBalance by viewModel.hideBalance.collectAsState()
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showAllAccountsDialog by remember { mutableStateOf(false) }
    var showAddPlannedDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var plannedToEdit by remember { mutableStateOf<PlannedTransaction?>(null) }
    var viewedAccountForDetails by remember { mutableStateOf<Account?>(null) }

    var showNotificationsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = copyUriToLocalFile(context, uri)
            viewModel.updateProfileImageUri(localPath)
        }
    }

    val permissionToRequest = if (Build.VERSION.SDK_INT >= 33) {
        "android.permission.READ_MEDIA_IMAGES"
    } else {
        "android.permission.READ_EXTERNAL_STORAGE"
    }

    var pendingPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pendingPermissionAction?.invoke()
        } else {
            Toast.makeText(context, "Storage permission is required to access files", Toast.LENGTH_SHORT).show()
        }
        pendingPermissionAction = null
    }

    val requestStoragePermission = { actionOnSuccess: () -> Unit ->
        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            permissionToRequest
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            actionOnSuccess()
        } else {
            pendingPermissionAction = actionOnSuccess
            permissionLauncher.launch(permissionToRequest)
        }
    }

    val fabInteractionSource = remember { MutableInteractionSource() }
    val fabPressed by fabInteractionSource.collectIsPressedAsState()
    val fabScale by animateFloatAsState(
        targetValue = if (fabPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "fabScale"
    )

    val existingCategories = remember(expenses, plannedTransactions) {
        ((expenses.map { it.category.trim() } + plannedTransactions.map { it.category.trim() }).filter { it.isNotEmpty() } + listOf("Food", "Other")).distinct()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBackground)
    ) {


        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            if (showAddDialog) {
                AddExpenseDialog(
                    initialCategory = addDialogPrefillCategory,
                    existingCategories = existingCategories,
                    accounts = accounts,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { amt, desc, cat, type, accId, toAccId, tags ->
                        viewModel.addExpense(amt, desc, cat, type, accId, toAccId, tags)
                        showAddDialog = false
                    }
                )
            }

            if (showAddAccountDialog) {
                AddAccountDialog(
                    onDismiss = { showAddAccountDialog = false },
                    onConfirm = { name, bal, color, icon, include ->
                        viewModel.addAccount(name, bal, color, icon, includeInBalance = include)
                        showAddAccountDialog = false
                    }
                )
            }

            if (accountToEdit != null) {
                AddAccountDialog(
                    accountToEdit = accountToEdit,
                    onDismiss = { accountToEdit = null },
                    onConfirm = { name, bal, color, icon, include ->
                        accountToEdit?.let {
                            viewModel.updateAccount(it.copy(name = name, balance = bal, colorHex = color, icon = icon, includeInBalance = include))
                        }
                        accountToEdit = null
                    },
                    onDelete = {
                        accountToDelete = accountToEdit
                    }
                )
            }

            if (accountToDelete != null) {
                AlertDialog(
                    onDismissRequest = { accountToDelete = null },
                    title = { Text("Delete Account", color = TextPrimary, fontWeight = FontWeight.Bold) },
                    text = { Text("Are you sure you want to delete the account '${accountToDelete?.name}'? This action cannot be undone.", color = TextPrimary) },
                    confirmButton = {
                        Button(
                            onClick = {
                                accountToDelete?.let {
                                    viewModel.deleteAccount(it)
                                }
                                accountToDelete = null
                                accountToEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA3B35))
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { accountToDelete = null }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    },
                    containerColor = ThemeBackground
                )
            }

            val viewedAccount = viewedAccountForDetails
            if (viewedAccount != null) {
                AccountDetailsDialog(
                    account = viewedAccount,
                    allExpenses = expenses,
                    onDismiss = { viewedAccountForDetails = null },
                    onEditAccount = {
                        accountToEdit = viewedAccount
                        viewedAccountForDetails = null
                    },
                    onDeleteTransaction = { expenseId ->
                        viewModel.deleteExpense(expenseId)
                    }
                )
            }

            if (showAllAccountsDialog) {
                AllAccountsDialog(
                    accounts = accounts,
                    hideBalance = hideBalance,
                    onDismiss = { showAllAccountsDialog = false },
                    onAccountClick = { account ->
                        viewedAccountForDetails = account
                        showAllAccountsDialog = false
                    },
                    onAddAccountClick = {
                        showAddAccountDialog = true
                        showAllAccountsDialog = false
                    }
                )
            }

            if (showTransferDialog) {
                TransferDialog(
                    accounts = accounts,
                    onDismiss = { showTransferDialog = false },
                    onConfirm = { from, to, amt, desc ->
                        viewModel.addExpense(amt, desc, "Transfer", "TRANSFER", from, to, "")
                        showTransferDialog = false
                    }
                )
            }

            if (showAddPlannedDialog) {
                AddPlannedTransactionDialog(
                    accounts = accounts,
                    existingCategories = existingCategories,
                    onDismiss = { showAddPlannedDialog = false },
                    onConfirm = { title, amt, cat, type, accId, start, intervalType, intervalN, oneTime, desc ->
                        viewModel.addPlannedTransaction(title, amt, cat, type, accId, start, intervalType, intervalN, oneTime, desc)
                        showAddPlannedDialog = false
                    }
                )
            }

            if (plannedToEdit != null) {
                AddPlannedTransactionDialog(
                    plannedToEdit = plannedToEdit,
                    accounts = accounts,
                    existingCategories = existingCategories,
                    onDismiss = { plannedToEdit = null },
                    onConfirm = { title, amt, cat, type, accId, start, intervalType, intervalN, oneTime, desc ->
                        plannedToEdit?.let {
                            viewModel.updatePlannedTransaction(it.copy(
                                title = title,
                                amount = amt,
                                category = cat,
                                type = type,
                                accountId = accId,
                                startDate = start,
                                intervalType = intervalType,
                                intervalN = intervalN,
                                oneTime = oneTime,
                                description = desc
                            ))
                        }
                        plannedToEdit = null
                    },
                    onDelete = {
                        plannedToEdit?.let { viewModel.deletePlannedTransaction(it) }
                        plannedToEdit = null
                    }
                )
            }

            if (showAddDebtDueDialog) {
                AddDebtDueDialog(
                    onDismiss = { showAddDebtDueDialog = false },
                    accounts = accounts,
                    onConfirm = { name, amt, desc, type, dueDate, accountId ->
                        viewModel.addDebtDue(name, amt, desc, type, dueDate, accountId)
                        showAddDebtDueDialog = false
                    }
                )
            }

            if (showEditBudgetDialog) {
                var tempAmount by remember { mutableStateOf(budgetLimit.toString()) }
                val budgetPeriodType by viewModel.budgetPeriodType.collectAsState()
                val budgetCustomStartDate by viewModel.budgetCustomStartDate.collectAsState()
                val budgetCustomEndDate by viewModel.budgetCustomEndDate.collectAsState()

                AlertDialog(
                    onDismissRequest = { showEditBudgetDialog = false },
                    title = {
                        Text(
                            text = "Edit Budget & Cycle",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = tempAmount,
                                onValueChange = { tempAmount = it },
                                label = { Text("Budget Limit (৳)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                            Text(
                                text = "Budget Period",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "monthly" to "Monthly",
                                    "weekly" to "Weekly",
                                    "custom" to "Custom"
                                ).forEach { (typeKey, typeLabel) ->
                                    val isSelected = budgetPeriodType == typeKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) PrimaryAccent else CardSurface)
                                            .clickable {
                                                if (typeKey == "custom" && budgetCustomStartDate == 0L) {
                                                    val defaultStart = Calendar.getInstance().apply {
                                                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                                    }.timeInMillis
                                                    val defaultEnd = Calendar.getInstance().apply {
                                                        timeInMillis = defaultStart
                                                        add(Calendar.DAY_OF_YEAR, 30)
                                                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                                                    }.timeInMillis
                                                    viewModel.updateBudgetPeriod("custom", defaultStart, defaultEnd)
                                                } else {
                                                    viewModel.updateBudgetPeriod(typeKey, budgetCustomStartDate, budgetCustomEndDate)
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = typeLabel,
                                            color = if (isSelected) Color.White else TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            if (budgetPeriodType == "custom") {
                                val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.US)
                                val startLabel = if (budgetCustomStartDate > 0L) sdf.format(Date(budgetCustomStartDate)) else "Add Date"
                                val endLabel = if (budgetCustomEndDate > 0L) sdf.format(Date(budgetCustomEndDate)) else "Add Date"

                                val startCal = Calendar.getInstance().apply {
                                    if (budgetCustomStartDate > 0L) timeInMillis = budgetCustomStartDate
                                }
                                val startDatePickerDialog = remember(budgetCustomStartDate) {
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedCal = Calendar.getInstance()
                                            selectedCal.set(year, month, dayOfMonth, 0, 0, 0)
                                            selectedCal.set(Calendar.MILLISECOND, 0)
                                            val endVal = if (budgetCustomEndDate > selectedCal.timeInMillis) budgetCustomEndDate else selectedCal.timeInMillis + (24L * 60 * 60 * 1000 * 30)
                                            viewModel.updateBudgetPeriod("custom", selectedCal.timeInMillis, endVal)
                                        },
                                        startCal.get(Calendar.YEAR),
                                        startCal.get(Calendar.MONTH),
                                        startCal.get(Calendar.DAY_OF_MONTH)
                                    )
                                }

                                val endCal = Calendar.getInstance().apply {
                                    if (budgetCustomEndDate > 0L) timeInMillis = budgetCustomEndDate
                                }
                                val endDatePickerDialog = remember(budgetCustomEndDate) {
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedCal = Calendar.getInstance()
                                            selectedCal.set(year, month, dayOfMonth, 23, 59, 59)
                                            selectedCal.set(Calendar.MILLISECOND, 999)
                                            val startVal = if (budgetCustomStartDate > 0L && budgetCustomStartDate < selectedCal.timeInMillis) budgetCustomStartDate else selectedCal.timeInMillis - (24L * 60 * 60 * 1000 * 30)
                                            viewModel.updateBudgetPeriod("custom", startVal, selectedCal.timeInMillis)
                                        },
                                        endCal.get(Calendar.YEAR),
                                        endCal.get(Calendar.MONTH),
                                        endCal.get(Calendar.DAY_OF_MONTH)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CardSurface)
                                            .clickable { startDatePickerDialog.show() }
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("START DATE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = TextSecondary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(startLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (budgetCustomStartDate > 0L) TextPrimary else TextSecondary)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CardSurface)
                                            .clickable { endDatePickerDialog.show() }
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("END DATE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = TextSecondary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(endLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (budgetCustomEndDate > 0L) TextPrimary else TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amount = tempAmount.toDoubleOrNull()
                                if (amount != null) {
                                    viewModel.updateBudgetLimit(amount)
                                    showEditBudgetDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditBudgetDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    },
                    containerColor = CardSurface,
                    shape = RoundedCornerShape(24.dp)
                )
            }



            Box(modifier = Modifier.fillMaxSize()) {
                TabContent(visible = currentTab == "home") {
                    HomeTab(
                        viewModel = viewModel,
                        userName = userName,
                        profileImageBitmap = profileImageBitmap,
                        activePeriodSpent = activePeriodSpent,
                        budgetLimit = budgetLimit,
                        expenses = expenses,
                        hasUnreadNotifications = hasUnreadNotifications,
                        onAddExpenseClick = { category ->
                            addDialogPrefillCategory = category
                            showAddDialog = true
                        },
                        onNavigate = { tab ->
                            if (tab.startsWith("history:")) {
                                activeHistorySection = tab.substringAfter("history:")
                                currentTab = "history"
                            } else {
                                currentTab = tab
                            }
                        },
                        onShowNotifications = {
                            viewModel.markNotificationsAsRead()
                            showNotificationsDialog = true
                        },
                        onEditBudgetClick = {
                            showEditBudgetDialog = true
                        },
                        accounts = accounts,
                        plannedTransactions = plannedTransactions,
                        onAddAccountClick = { showAddAccountDialog = true },
                        onAccountClick = { viewedAccountForDetails = it },
                        onTransferClick = { showTransferDialog = true },
                        onViewAllAccountsClick = { showAllAccountsDialog = true },
                        onAddPlannedClick = { showAddPlannedDialog = true },
                        onPlannedClick = { plannedToEdit = it },
                        onPayPlanned = { viewModel.executePlannedTransaction(it) },
                        onSkipPlanned = { viewModel.skipPlannedTransaction(it) }
                    )
                }
                TabContent(visible = currentTab == "analytics") {
                    AnalyticsTab(
                        expenses = expenses,
                        debtsDues = debtsDues,
                        budgetLimit = budgetLimit,
                        viewModel = viewModel
                    )
                }
                TabContent(visible = currentTab == "history") {
                    val hideIncome by viewModel.hideIncome.collectAsState()
                    HistoryTab(
                        viewModel = viewModel,
                        expenses = expenses,
                        debtsDues = debtsDues,
                        activeHistorySection = activeHistorySection,
                        onSectionChange = { activeHistorySection = it },
                        onDeleteExpense = { viewModel.deleteExpense(it) },
                        onSettleDebtDue = { item, paidAmt, logAsExp, accountId ->
                            viewModel.settleDebtDuePartial(item, paidAmt, logAsExp, accountId)
                        },
                        onDeleteDebtDue = { id ->
                            viewModel.deleteDebtDue(id)
                        },
                        hideIncome = hideIncome
                    )
                }
                TabContent(visible = currentTab == "profile") {
                    ProfileTab(
                        viewModel = viewModel,
                        userName = userName,
                        profileImageBitmap = profileImageBitmap,
                        onUploadImageClick = {
                            requestStoragePermission {
                                imagePickerLauncher.launch("image/*")
                            }
                        },
                        onUpdateName = { name -> viewModel.updateUserName(name) },
                        budgetLimit = budgetLimit,
                        biometricsEnabled = biometricsEnabled,
                        themeSelection = themeSelection,
                        activity = activity,
                        expenses = expenses,
                        debtsDues = debtsDues,
                        onRequestStoragePermission = { action ->
                            requestStoragePermission(action)
                        }
                    )
                }
            }
        }


        // Dimming overlay when Floating Choices menu is shown
        AnimatedVisibility(
            visible = showAddChoiceDialog,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showAddChoiceDialog = false
                    }
            )
        }

        // Floating Choices speed dial menu
        AnimatedVisibility(
            visible = showAddChoiceDialog,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 88.dp, end = 16.dp)
                .width(220.dp)
        ) {
            val isDark = LocalAppColors.current == DarkAppColors
            val menuGlassBg = if (isDark) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1E22).copy(alpha = 0.88f),
                        Color(0xFF141416).copy(alpha = 0.94f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF).copy(alpha = 0.94f),
                        Color(0xFFEBEBE8).copy(alpha = 0.88f)
                    )
                )
            }
            
            val menuGlassBorder = if (isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.32f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.75f),
                        Color.Black.copy(alpha = 0.12f)
                    )
                )
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color.Black.copy(alpha = 0.25f)
                    )
                    .background(menuGlassBg, RoundedCornerShape(24.dp))
                    .border(1.dp, menuGlassBorder, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "LOG NEW ENTRY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    // Option 1: Log Transaction
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showAddChoiceDialog = false
                                showAddDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddCard,
                                contentDescription = "Log Transaction",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Log Transaction",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    // Option 2: Log Debt/Due
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showAddChoiceDialog = false
                                showAddDebtDueDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.People,
                                contentDescription = "Debt / Receivable",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Debt / Receivable",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    // Option 3: Transfer Funds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showAddChoiceDialog = false
                                showTransferDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = "Transfer Funds",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Transfer Funds",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Transparent)
                .navigationBarsPadding()
                .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                val isDark = LocalAppColors.current == DarkAppColors
                val navGlassBgBrush = remember(isDark) {
                    if (isDark) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E1E22).copy(alpha = 0.88f),
                                Color(0xFF141416).copy(alpha = 0.94f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF).copy(alpha = 0.94f),
                                Color(0xFFEBEBE8).copy(alpha = 0.88f)
                            )
                        )
                    }
                }
                
                val navGlassBorder = remember(isDark) {
                    if (isDark) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.32f), // Stronger reflection at top-left
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.12f)
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = Color.Black.copy(alpha = 0.15f)
                        )
                ) {
                    // 1. Frosted background layer (no blur modifier to avoid layout-invalidation lag)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(navGlassBgBrush, RoundedCornerShape(32.dp))
                    )

                    // 2. Specular border and content overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, navGlassBorder, RoundedCornerShape(32.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tabs = remember {
                                listOf(
                                    Triple("home", Icons.Rounded.Home, "Home"),
                                    Triple("history", Icons.AutoMirrored.Rounded.List, "Logs"),
                                    Triple("analytics", Icons.Rounded.BarChart, "Stats"),
                                    Triple("profile", Icons.Rounded.Person, "Profile")
                                )
                            }

                            tabs.forEach { (tabId, icon, label) ->
                                key(tabId) {
                                    val isSelected = currentTab == tabId
                                    
                                    val itemInteractionSource = remember { MutableInteractionSource() }
                                    val itemPressed by itemInteractionSource.collectIsPressedAsState()
                                    val itemScale by animateFloatAsState(if (itemPressed) 0.92f else 1f, label = "tabItemScale")

                                    val selectedTabBg = remember(isSelected, isDark) {
                                        if (isSelected) {
                                            if (isDark) {
                                                Color(0xFFFFFFFF).copy(alpha = 0.15f) // Frosted white glass overlay
                                            } else {
                                                Color(0xFF000000).copy(alpha = 0.10f) // Soft overlay
                                            }
                                        } else {
                                            Color.Transparent
                                        }
                                    }
                                     
                                    val selectedTabBorder = remember(isSelected, isDark) {
                                        if (isSelected) {
                                            if (isDark) {
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.25f),
                                                        Color.White.copy(alpha = 0.02f)
                                                    )
                                                )
                                            } else {
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.Black.copy(alpha = 0.15f),
                                                        Color.Black.copy(alpha = 0.02f)
                                                    )
                                                )
                                            }
                                        } else {
                                            Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .graphicsLayer {
                                                scaleX = itemScale
                                                scaleY = itemScale
                                            }
                                            .then(
                                                if (isSelected) {
                                                    Modifier
                                                        .shadow(
                                                            elevation = 4.dp,
                                                            shape = RoundedCornerShape(24.dp),
                                                            clip = false,
                                                            spotColor = Color.Black.copy(alpha = 0.25f)
                                                        )
                                                        .background(selectedTabBg, RoundedCornerShape(24.dp))
                                                        .border(0.5.dp, selectedTabBorder, RoundedCornerShape(24.dp))
                                                } else {
                                                    Modifier
                                                        .clip(RoundedCornerShape(24.dp))
                                                }
                                            )
                                            .clickable(
                                                interactionSource = itemInteractionSource,
                                                indication = LocalIndication.current
                                            ) { currentTab = tabId }
                                            .padding(horizontal = if (isSelected) 14.dp else 12.dp, vertical = 8.dp)
                                            .animateContentSize(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (isSelected) TextPrimary else TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            if (isSelected) {
                                                Text(
                                                    text = label,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                val fabRotation by animateFloatAsState(
                    targetValue = if (showAddChoiceDialog) 45f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "fabRotation"
                )

                val fabBorderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = fabScale
                            scaleY = fabScale
                        }
                        .size(56.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            spotColor = Color.Black.copy(alpha = 0.2f)
                        )
                        .background(PrimaryAccent.copy(alpha = 0.9f), CircleShape)
                        .clickable(
                            interactionSource = fabInteractionSource,
                            indication = LocalIndication.current
                        ) {
                            addDialogPrefillCategory = ""
                            showAddChoiceDialog = !showAddChoiceDialog
                        }
                        .border(1.dp, fabBorderBrush, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Transaction",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                rotationZ = fabRotation
                            }
                    )
                }
            }
        }


        if (showNotificationsDialog) {
            Dialog(onDismissRequest = { showNotificationsDialog = false }) {
                val notificationSections = remember(expenses, budgetLimit, activePeriodSpent) {
                    val sections = mutableListOf<Pair<String, List<NotificationItem>>>()


                    val budgetAlerts = mutableListOf<NotificationItem>()
                    val percentUsed = if (budgetLimit > 0) (activePeriodSpent / budgetLimit * 100).toInt() else 0

                    if (activePeriodSpent > budgetLimit) {
                        budgetAlerts.add(NotificationItem(
                            title = "Budget Exceeded",
                            text = "You've overspent by ৳${String.format(Locale.US, "%,.0f", activePeriodSpent - budgetLimit)}. Review your recent expenses.",
                            icon = Icons.Rounded.Error,
                            colorType = "accent",
                            severity = 2
                        ))
                    } else if (percentUsed >= 80) {
                        budgetAlerts.add(NotificationItem(
                            title = "High Spending Alert",
                            text = "$percentUsed% of budget used (৳${String.format(Locale.US, "%,.0f", budgetLimit - activePeriodSpent)} remaining).",
                            icon = Icons.Rounded.Warning,
                            colorType = "accent",
                            severity = 1
                        ))
                    } else if (expenses.isNotEmpty()) {
                        budgetAlerts.add(NotificationItem(
                            title = "Budget On Track",
                            text = "$percentUsed% used — ৳${String.format(Locale.US, "%,.0f", budgetLimit - activePeriodSpent)} remaining this month.",
                            icon = Icons.Rounded.CheckCircle,
                            colorType = "primary",
                            severity = 0
                        ))
                    }
                    if (budgetAlerts.isNotEmpty()) sections.add("Budget Alerts" to budgetAlerts)


                    val analysis = mutableListOf<NotificationItem>()
                    if (expenses.isNotEmpty()) {

                        val categoryGroups = expenses.groupBy { it.category }
                        val topCat = categoryGroups.maxByOrNull { it.value.sumOf { e -> e.amount } }
                        topCat?.let {
                            val topAmt = it.value.sumOf { e -> e.amount }
                            val topPct = if (activePeriodSpent > 0) (topAmt / activePeriodSpent * 100).toInt() else 0
                            analysis.add(NotificationItem(
                                title = "Top Category: ${it.key}",
                                text = "৳${String.format(Locale.US, "%,.0f", topAmt)} spent ($topPct% of total). ${if (topPct > 50) "Consider diversifying your spending." else ""}",
                                icon = Icons.Rounded.BarChart,
                                colorType = if (topPct > 50) "accent" else "secondary",
                                severity = if (topPct > 50) 1 else 0
                            ))
                        }


                        val avgPerTxn = activePeriodSpent / expenses.size
                        analysis.add(NotificationItem(
                            title = "Avg. Transaction: ৳${String.format(Locale.US, "%,.0f", avgPerTxn)}",
                            text = "Across ${expenses.size} total transactions.",
                            icon = Icons.Rounded.Calculate,
                            colorType = "primary",
                            severity = 0
                        ))


                        val catCount = categoryGroups.size
                        analysis.add(NotificationItem(
                            title = "$catCount Active ${if (catCount == 1) "Category" else "Categories"}",
                            text = categoryGroups.keys.joinToString(", "),
                            icon = Icons.Rounded.Category,
                            colorType = "secondary",
                            severity = 0
                        ))
                    }
                    if (analysis.isNotEmpty()) sections.add("Spending Analysis" to analysis)


                    val trends = mutableListOf<NotificationItem>()
                    if (expenses.isNotEmpty()) {
                        val cal = Calendar.getInstance()
                        val todayStart = (cal.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val todayExpenses = expenses.filter { it.date >= todayStart }
                        val todayTotal = todayExpenses.sumOf { it.amount }

                        if (todayExpenses.isNotEmpty()) {
                            trends.add(NotificationItem(
                                title = "Today's Spending: ৳${String.format(Locale.US, "%,.0f", todayTotal)}",
                                text = "${todayExpenses.size} transaction${if (todayExpenses.size > 1) "s" else ""} logged today.",
                                icon = Icons.Rounded.Today,
                                colorType = "primary",
                                severity = 0
                            ))
                        } else {
                            trends.add(NotificationItem(
                                title = "No Spending Today",
                                text = "Great discipline! Keep it going.",
                                icon = Icons.Rounded.Savings,
                                colorType = "secondary",
                                severity = 0
                            ))
                        }


                        val projection = BudgetPeriodHelper.getPeriodProjection(
                            expenses = expenses,
                            budgetLimit = budgetLimit,
                            periodType = budgetPeriodType,
                            customStart = budgetCustomStartDate,
                            customEnd = budgetCustomEndDate
                        )
                        val projected = projection.projected
                        val predictedSavings = projection.predictedSavings
                        val periodLabel = when (budgetPeriodType) {
                            "weekly" -> "this week"
                            "custom" -> "this period"
                            else -> "this month"
                        }

                        if (predictedSavings >= 0) {
                            trends.add(NotificationItem(
                                title = "Savings Forecast: +৳${String.format(Locale.US, "%,.0f", predictedSavings)}",
                                text = "At current pace, you'll save ৳${String.format(Locale.US, "%,.0f", predictedSavings)} $periodLabel.",
                                icon = Icons.Rounded.TrendingDown,
                                colorType = "primary",
                                severity = 0
                            ))
                        } else {
                            trends.add(NotificationItem(
                                title = "Over-Budget Warning",
                                text = "Projected to exceed budget by ৳${String.format(Locale.US, "%,.0f", -predictedSavings)} $periodLabel. Reduce daily spending.",
                                icon = Icons.Rounded.TrendingUp,
                                colorType = "accent",
                                severity = 2
                            ))
                        }
                    }
                    if (trends.isNotEmpty()) sections.add("Daily Trends" to trends)


                    val insights = mutableListOf<NotificationItem>()
                    if (expenses.isNotEmpty()) {

                        val latest = expenses.first()
                        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.US)
                        insights.add(NotificationItem(
                            title = "Last Transaction",
                            text = "৳${String.format(Locale.US, "%,.0f", latest.amount)} — ${latest.description.ifEmpty { latest.category }} (${sdf.format(Date(latest.date))})",
                            icon = Icons.Rounded.Receipt,
                            colorType = "secondary",
                            severity = 0
                        ))


                        val biggest = expenses.maxByOrNull { it.amount }
                        biggest?.let {
                            insights.add(NotificationItem(
                                title = "Biggest Expense: ৳${String.format(Locale.US, "%,.0f", it.amount)}",
                                text = "${it.description.ifEmpty { it.category }} in ${it.category}.",
                                icon = Icons.Rounded.Lightbulb,
                                colorType = "primary",
                                severity = 0
                            ))
                        }
                    } else {
                        insights.add(NotificationItem(
                            title = "Get Started",
                            text = "Tap the bolt button to log your first expense and unlock insights!",
                            icon = Icons.Rounded.Lightbulb,
                            colorType = "secondary",
                            severity = 0
                        ))
                    }
                    if (insights.isNotEmpty()) sections.add("Quick Insights" to insights)

                    sections
                }

                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(1.dp, CardSurface, RoundedCornerShape(28.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(CardSurface, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Notifications, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Insights & Alerts",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        "Live analytics updates",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showNotificationsDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))


                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            notificationSections.forEachIndexed { sectionIdx, (sectionTitle, items) ->
                                item {
                                    if (sectionIdx > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        HorizontalDivider(color = CardSurface, thickness = 1.dp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    Text(
                                        text = sectionTitle.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = TextSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                items(items, key = { it.title }) { item ->
                                    NotificationCard(item = item)
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}




@Composable
fun HomeTab(
    viewModel: MainViewModel,
    userName: String,
    profileImageBitmap: Bitmap?,
    activePeriodSpent: Double,
    budgetLimit: Double,
    expenses: List<Expense>,
    hasUnreadNotifications: Boolean,
    onAddExpenseClick: (category: String) -> Unit,
    onNavigate: (tab: String) -> Unit,
    onShowNotifications: () -> Unit,
    onEditBudgetClick: () -> Unit,
    accounts: List<Account> = emptyList(),
    plannedTransactions: List<PlannedTransaction> = emptyList(),
    onAddAccountClick: () -> Unit = {},
    onAccountClick: (Account) -> Unit = {},
    onTransferClick: () -> Unit = {},
    onViewAllAccountsClick: () -> Unit = {},
    onAddPlannedClick: () -> Unit = {},
    onPlannedClick: (PlannedTransaction) -> Unit = {},
    onPayPlanned: (PlannedTransaction) -> Unit = {},
    onSkipPlanned: (PlannedTransaction) -> Unit = {}
) {
    val context = LocalContext.current
    val debtsDues by viewModel.debtsDues.collectAsState()
    val budgetPeriodType by viewModel.budgetPeriodType.collectAsState()
    val budgetCustomStartDate by viewModel.budgetCustomStartDate.collectAsState()
    val budgetCustomEndDate by viewModel.budgetCustomEndDate.collectAsState()
    val hideBalance by viewModel.hideBalance.collectAsState()
    val hideIncome by viewModel.hideIncome.collectAsState()
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val displayExpenses by produceState(initialValue = emptyList<Expense>(), expenses, selectedFilter, hideIncome) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val weekStart = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.SUNDAY
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val baseList = if (hideIncome) expenses.filter { it.type != "INCOME" } else expenses
            val filtered = when (selectedFilter) {
                "Today" -> baseList.filter { it.date >= todayStart }
                "This Week" -> baseList.filter { it.date >= weekStart }
                "This Month" -> baseList.filter { it.date >= monthStart }
                else -> baseList
            }
            filtered.take(4)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bellScale"
    )


    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )


    val bellRotation by if (hasUnreadNotifications) {
        val rotationTransition = rememberInfiniteTransition(label = "bellRotation")
        rotationTransition.animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bellRotation"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val pendingDebts = remember(debtsDues) {
        debtsDues.filter { !it.isCleared && it.type == "DEBT" }.sumOf { it.amount }
    }
    val pendingDues = remember(debtsDues) {
        debtsDues.filter { !it.isCleared && it.type == "DUE" }.sumOf { it.amount }
    }

    val home7DaysData = remember(expenses) {
        val cal = Calendar.getInstance()
        val result = mutableListOf<Pair<String, Double>>()
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        for (i in 6 downTo 0) {
            val dayStart = (cal.clone() as Calendar).apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val dayEnd = (dayStart.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
            }
            val label = dayFormat.format(Date(dayStart.timeInMillis))
            val sum = expenses
                .filter { it.type == "EXPENSE" && it.date in dayStart.timeInMillis..dayEnd.timeInMillis }
                .sumOf { it.amount }
            result.add(label to sum)
        }
        result
    }

    val lastSpendText = remember(expenses) {
        val lastExpense = expenses.firstOrNull { it.type == "EXPENSE" }
        if (lastExpense != null) {
            "Latest: ৳${String.format(Locale.US, "%.0f", lastExpense.amount)} spent on ${lastExpense.category}"
        } else {
            "Goal target: ৳${String.format(Locale.US, "%.0f", budgetLimit)} set"
        }
    }

    val hubCategories = remember(expenses, plannedTransactions) {
        (listOf("Food", "Other") + expenses.map { it.category } + plannedTransactions.map { it.category }).distinct()
    }

    val bottomPadding = 112.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = bottomPadding, top = 24.dp)
    ) {
        item {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "WELCOME BACK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontSize = 9.sp
                        ),
                        color = TextSecondary
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Month/Period Pill at Top Right
                    Surface(
                        onClick = onEditBudgetClick,
                        shape = RoundedCornerShape(20.dp),
                        color = CardSurface,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = PrimaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            val sdf = SimpleDateFormat("dd MMM", Locale.US)
                            val rangeLabel = when (budgetPeriodType) {
                                "weekly" -> {
                                    val start = BudgetPeriodHelper.getPeriodRange("weekly", 0, 0).first
                                    val end = BudgetPeriodHelper.getPeriodRange("weekly", 0, 0).second
                                    "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
                                }
                                "custom" -> {
                                    if (budgetCustomStartDate > 0L && budgetCustomEndDate > 0L) {
                                        "${sdf.format(Date(budgetCustomStartDate))} - ${sdf.format(Date(budgetCustomEndDate))}"
                                    } else {
                                        "Select Custom Dates"
                                    }
                                }
                                else -> {
                                    val sdfMonth = SimpleDateFormat("MMM yyyy", Locale.US)
                                    sdfMonth.format(Date())
                                }
                            }
                            
                            Text(
                                text = rangeLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit Budget",
                                tint = TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CardSurface)
                            .clickable { onNavigate("profile") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImageBitmap != null) {
                            Image(
                                bitmap = profileImageBitmap.asImageBitmap(),
                                contentDescription = "Settings Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CardSurface)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = LocalIndication.current
                                ) { onShowNotifications() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Notifications",
                                tint = if (hasUnreadNotifications) PrimaryAccent else TextPrimary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = bellRotation }
                            )
                        }

                        if (hasUnreadNotifications) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-1).dp, y = 1.dp)
                                    .size(8.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                        alpha = pulseAlpha
                                    }
                                    .background(PrimaryAccent, CircleShape)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-1).dp, y = 1.dp)
                                    .size(8.dp)
                                    .border(1.dp, ThemeBackground, CircleShape)
                                    .background(PrimaryAccent, CircleShape)
                            )
                        }
                    }
                }
            }


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val totalAccountsBalance = remember(accounts) {
                    accounts.filter { it.includeInBalance }.sumOf { it.balance }
                }
                val totalBalanceText = if (hideBalance) "••••" else String.format(Locale.US, "%,.2f", totalAccountsBalance)

                val remainingBudget = (budgetLimit - activePeriodSpent).coerceAtLeast(0.0)
                val remainingText = if (hideBalance) "••••" else String.format(Locale.US, "%,.2f", remainingBudget)

                // 1. Total Accounts Balance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, CardSurface.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ThemeBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Wallet,
                                contentDescription = "Total Balance",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "TOTAL ACCOUNTS BALANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 10.sp
                                ),
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "৳$totalBalanceText",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    letterSpacing = (-1).sp
                                )
                            )
                        }
                    }
                }

                // 2. Remaining Budget Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CardSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PieChart,
                            contentDescription = "Remaining Budget",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "REMAINING TARGETED BUDGET",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            ),
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "৳$remainingText",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                }

                // Dashed Divider
                val dividerColor = TextSecondary.copy(alpha = 0.3f)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                ) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        pathEffect = pathEffect,
                        strokeWidth = 2f
                    )
                }

                // 3. Spent of period target Row
                val spentText = if (hideBalance) "••••" else String.format(Locale.US, "%,.2f", activePeriodSpent)
                val periodLabel = when (budgetPeriodType) {
                    "weekly" -> "weekly"
                    "custom" -> {
                        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.US)
                        "target period (${sdf.format(Date(budgetCustomStartDate))} - ${sdf.format(Date(budgetCustomEndDate))})"
                    }
                    else -> "monthly"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(TextSecondary.copy(alpha = 0.6f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                                append("৳$spentText")
                            }
                            append(" spent of $periodLabel target")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // 3. Accounts/Wallets
            AccountsSection(
                accounts = accounts,
                hideBalance = hideBalance,
                onAddAccountClick = onAddAccountClick,
                onAccountClick = onAccountClick,
                onTransferClick = onTransferClick,
                onViewAllClick = onViewAllAccountsClick,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 4. Quick Spend Hub (moved higher for immediate access)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Category,
                        contentDescription = "Quick Spend Hub",
                        tint = PrimaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "QUICK SPEND HUB",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 13.sp
                        ),
                        color = TextPrimary
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hubCategories, key = { it }) { catName ->
                        val style = getCategoryStyle(catName)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            modifier = Modifier
                                .width(96.dp)
                                .clickable { onAddExpenseClick(catName) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(ThemeBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = style.first,
                                        contentDescription = catName,
                                        tint = style.third,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = catName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 5. Recent Activity (Line Chart)
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Recent Activity",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DarkCardTextPrimary
                            )
                            Text(
                                text = "Last 7 days spend trend",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkCardTextSecondary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .background(Color(0xFF1E1E22), RoundedCornerShape(12.dp))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF2E2E33), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "7D",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SmoothLineChart(
                        data = home7DaysData,
                        lineColor = Color.White,
                        gridColor = Color.White.copy(alpha = 0.1f),
                        dotColor = PrimaryAccent,
                        drawDot = true,
                        drawTooltip = true
                    )
                }
            }

            // 6. Latest Spend Card ("UPDATE")
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkCardSurface)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(5.dp).background(PrimaryAccent, CircleShape))
                                Text(
                                    text = "UPDATE",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = lastSpendText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ThemeBackground)
                            .clickable { onNavigate("history") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = "Arrow Go",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 7. Debts & Receivables Overview
            if (pendingDebts > 0 || pendingDues > 0) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .clickable { onNavigate("history:debts") }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Handshake,
                                contentDescription = "Debts & Receivables Overview",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "DEBTS & RECEIVABLES OVERVIEW",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    fontSize = 13.sp
                                ),
                                color = TextPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "You Owe",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "৳${String.format(Locale.US, "%,.0f", pendingDebts)}",
                                    color = if (pendingDebts > 0) Color(0xFFEA3B35) else TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(ThemeBackground)
                            )
                            
                            Column(
                                modifier = Modifier.weight(1f).padding(start = 16.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "You Are Owed",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "৳${String.format(Locale.US, "%,.0f", pendingDues)}",
                                    color = if (pendingDues > 0) Color(0xFF4CAF50) else TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "Go",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 8. Planned Expenses/Payments
            PlannedPaymentsSection(
                plannedTransactions = plannedTransactions,
                accounts = accounts,
                onPayClick = onPayPlanned,
                onSkipClick = onSkipPlanned,
                onAddPlannedClick = onAddPlannedClick,
                onPlannedClick = onPlannedClick,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 9. Recent Transactions Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { dropdownExpanded = true }
                            .background(CardSurface)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = selectedFilter,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(CardSurface)
                    ) {
                        listOf("All", "Today", "This Week", "This Month").forEach { filterOption ->
                            DropdownMenuItem(
                                text = { Text(filterOption, color = TextPrimary) },
                                onClick = {
                                    selectedFilter = filterOption
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (displayExpenses.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = if (expenses.isEmpty()) {
                            "No transactions added yet. Click the central Floating Add Button to start tracking!"
                        } else {
                            "No transactions match the selected filter."
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        textAlign = TextAlign.Center,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(displayExpenses, key = { it.id }) { expense ->
                ExpenseItem(
                    expense = expense,
                    accounts = accounts,
                    onDelete = { expenseToDelete = expense }
                )
            }
        }
    }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Transaction", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this transaction for '${expenseToDelete?.description}'?", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        expenseToDelete?.let {
                            viewModel.deleteExpense(it.id)
                        }
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ThemeBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}


@Composable
fun AnalyticsTab(
    expenses: List<Expense>,
    debtsDues: List<DebtDue>,
    budgetLimit: Double,
    viewModel: MainViewModel
) {
    var selectedStatsTab by remember { mutableStateOf("spending") }
    var isWeekSelected by remember { mutableStateOf(true) }

    val budgetPeriodType by viewModel.budgetPeriodType.collectAsState()
    val budgetCustomStartDate by viewModel.budgetCustomStartDate.collectAsState()
    val budgetCustomEndDate by viewModel.budgetCustomEndDate.collectAsState()

    val snapshot by viewModel.analyticsSnapshot.collectAsState()

    val last7DaysData = snapshot?.last7DaysData ?: emptyList()
    val last4WeeksData = snapshot?.last4WeeksData ?: emptyList()
    val totalSpent = snapshot?.totalSpent ?: 0.0
    val categoryData = snapshot?.categoryData ?: emptyList()
    val topCategory = categoryData.firstOrNull()
    val dailyAvg = snapshot?.dailyAvg ?: 0.0

    val chartData = remember(isWeekSelected, last7DaysData, last4WeeksData) {
        if (isWeekSelected) last7DaysData else last4WeeksData
    }


    val periodProjection = remember(expenses, budgetLimit, budgetPeriodType, budgetCustomStartDate, budgetCustomEndDate) {
        BudgetPeriodHelper.getPeriodProjection(
            expenses = expenses,
            budgetLimit = budgetLimit,
            periodType = budgetPeriodType,
            customStart = budgetCustomStartDate,
            customEnd = budgetCustomEndDate
        )
    }


    val biggestExpense = remember(expenses) { expenses.filter { it.type == "EXPENSE" }.maxByOrNull { it.amount } }


    val mostActiveDay = remember(expenses) {
        val expenseList = expenses.filter { it.type == "EXPENSE" }
        if (expenseList.isEmpty()) null
        else {
            val dayFormat = SimpleDateFormat("EEEE", Locale.US)
            expenseList.groupBy { dayFormat.format(Date(it.date)) }
                .maxByOrNull { it.value.size }
                ?.let { (day, list) -> day to list.size }
        }
    }


    val avgPerTransaction = remember(expenses, totalSpent) {
        val expenseList = expenses.filter { it.type == "EXPENSE" }
        if (expenseList.isEmpty()) 0.0 else totalSpent / expenseList.size
    }


    val chartColors = listOf(
        Color(0xFFEA3B35), // Accent Red
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFFF59E0B), // Orange/Amber
        Color(0xFF10B981), // Green
        Color(0xFF64748B), // Slate Grey
        Color(0xFFEAB308)  // Yellow
    )

    // Debts & Dues Calculations
    val activeDebts = remember(debtsDues) {
        debtsDues.filter { !it.isCleared && it.type == "DEBT" }
    }
    val activeDues = remember(debtsDues) {
        debtsDues.filter { !it.isCleared && it.type == "DUE" }
    }
    val totalDebtsVal = remember(activeDebts) {
        activeDebts.sumOf { it.amount }
    }
    val totalDuesVal = remember(activeDues) {
        activeDues.sumOf { it.amount }
    }
    val netPositionVal = remember(totalDebtsVal, totalDuesVal) {
        totalDuesVal - totalDebtsVal
    }

    val overdueItems = remember(debtsDues) {
        debtsDues.filter { !it.isCleared && it.dueDate != null && it.dueDate < System.currentTimeMillis() }
    }

    val topCreditors = remember(activeDebts) {
        activeDebts.groupBy { it.personName }
            .map { (name, list) -> name to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(3)
    }
    val topDebtors = remember(activeDues) {
        activeDues.groupBy { it.personName }
            .map { (name, list) -> name to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(3)
    }

    val totalDebtsCount = remember(debtsDues) { debtsDues.count { it.type == "DEBT" } }
    val settledDebtsCount = remember(debtsDues) { debtsDues.count { it.type == "DEBT" && it.isCleared } }
    val debtSettlementPct = remember(totalDebtsCount, settledDebtsCount) {
        if (totalDebtsCount > 0) (settledDebtsCount.toDouble() / totalDebtsCount) else 0.0
    }

    val totalDuesCount = remember(debtsDues) { debtsDues.count { it.type == "DUE" } }
    val settledDuesCount = remember(debtsDues) { debtsDues.count { it.type == "DUE" && it.isCleared } }
    val dueSettlementPct = remember(totalDuesCount, settledDuesCount) {
        if (totalDuesCount > 0) (settledDuesCount.toDouble() / totalDuesCount) else 0.0
    }

    val bottomPadding = 112.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = bottomPadding, top = 24.dp)
    ) {

        item {
            val titleText = if (selectedStatsTab == "spending") "Spending Analytics" else "Liability Analytics"
            val subtitleText = if (selectedStatsTab == "spending") {
                "Deep insights into your spending patterns."
            } else {
                "Insights into your outstanding debts and receivables."
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Segmented switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .background(CardSurface, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("spending" to "Spending", "debts_dues" to "Debts & Receivables").forEach { (tabKey, tabLabel) ->
                    val active = (selectedStatsTab == tabKey)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) ThemeBackground else Color.Transparent)
                            .clickable { selectedStatsTab = tabKey }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabLabel,
                            color = if (active) TextPrimary else TextSecondary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        if (selectedStatsTab == "spending") {
            if (expenses.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No spending data to analyze yet.",
                                textAlign = TextAlign.Center,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Add transactions on the dashboard to unlock insights.",
                                textAlign = TextAlign.Center,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {

                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ThemeBackground, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Assessment, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Overview", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "৳${String.format(Locale.US, "%,.0f", totalSpent)}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                        color = PrimaryAccent
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }

                                Box(modifier = Modifier.width(1.dp).height(48.dp).background(ThemeBackground))

                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "৳${String.format(Locale.US, "%,.0f", dailyAvg)}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Daily Avg", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }

                                Box(modifier = Modifier.width(1.dp).height(48.dp).background(ThemeBackground))

                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${expenses.size}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Transactions", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                }


                item {
                    topCategory?.let { (catName, catAmount) ->
                        val style = getCategoryStyle(catName)
                        val percentage = if (totalSpent > 0) (catAmount / totalSpent * 100) else 0.0
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(ThemeBackground, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.TrendingUp, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Top Spending Category", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(ThemeBackground, RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(style.first, contentDescription = null, tint = style.third, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(catName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                        Text(
                                            "${String.format(Locale.US, "%.1f", percentage)}% of total spending",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Text(
                                        "৳${String.format(Locale.US, "%,.0f", catAmount)}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                        color = style.third
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(ThemeBackground, RoundedCornerShape(4.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = (percentage / 100.0).toFloat().coerceIn(0f, 1f))
                                            .height(8.dp)
                                            .background(
                                                color = style.third,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }


                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ThemeBackground, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.PieChart, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Category Breakdown", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            SimpleBarChart(data = categoryData)

                            Spacer(modifier = Modifier.height(16.dp))

                            categoryData.forEach { (cat, amt) ->
                                val pct = if (totalSpent > 0) amt / totalSpent * 100 else 0.0
                                val catStyle = getCategoryStyle(cat)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(ThemeBackground, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(catStyle.first, contentDescription = null, tint = catStyle.third, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(cat, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                    Text(
                                        "${String.format(Locale.US, "%.0f", pct)}%",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextSecondary,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        "৳${String.format(Locale.US, "%,.0f", amt)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }


                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp, top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Spend Trend",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isWeekSelected) "Last 7 days spend activity" else "Last 4 weeks spend activity",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }


                            Row(
                                modifier = Modifier
                                    .background(CardSurface, RoundedCornerShape(12.dp))
                                    .padding(2.dp)
                            ) {
                                listOf("Week" to true, "Month" to false).forEach { (label, isWeek) ->
                                    val active = (isWeekSelected == isWeek)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (active) DarkCardSurface else Color.Transparent)
                                            .clickable { isWeekSelected = isWeek }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (active) Color.White else TextSecondary,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))


                        SmoothLineChart(
                            data = chartData,
                            lineColor = TextPrimary,
                            gridColor = CardSurface,
                            dotColor = PrimaryAccent,
                            drawDot = true,
                            drawTooltip = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))


                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            chartData.forEach { (day, _) ->
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextSecondary,
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))


                        val highDay = chartData.maxByOrNull { it.second }
                        val lowDay = chartData.filter { it.second > 0.0 }.minByOrNull { it.second }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            highDay?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Peak: ${it.first} (৳${String.format(Locale.US, "%,.0f", it.second)})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PrimaryAccent
                                    )
                                }
                            }
                            lowDay?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Low: ${it.first} (৳${String.format(Locale.US, "%,.0f", it.second)})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }


                item {
                    val activeSpent = periodProjection.spent
                    val projected = periodProjection.projected
                    val predictedSavings = periodProjection.predictedSavings
                    val isOnTrack = predictedSavings >= 0
                    val dailyRate = periodProjection.dailyRate

                    val totalScale = maxOf(budgetLimit, projected)
                    val actualFraction = if (totalScale > 0) (activeSpent / totalScale).toFloat().coerceIn(0f, 1f) else 0f
                    val projectedFraction = if (totalScale > 0) (projected / totalScale).toFloat().coerceIn(0f, 1f) else 0f
                    val budgetFraction = if (totalScale > 0) (budgetLimit / totalScale).toFloat().coerceIn(0f, 1f) else 0f

                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ThemeBackground, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isOnTrack) Icons.Rounded.Savings else Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = if (isOnTrack) TextPrimary else PrimaryAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    val periodTitle = when (budgetPeriodType) {
                                        "weekly" -> "Weekly"
                                        "custom" -> "Period"
                                        else -> "Monthly"
                                    }
                                    Text(
                                        text = "$periodTitle Spending Forecast", 
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Est. period-end spend based on your daily speed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))


                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(), 
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = "Actual Spend (So Far)", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "৳${String.format(Locale.US, "%,.0f", activeSpent)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isOnTrack) TextPrimary else PrimaryAccent
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))


                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .background(ThemeBackground, RoundedCornerShape(4.dp))
                                    )

                                    if (projectedFraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = projectedFraction)
                                                .height(8.dp)
                                                .background(
                                                    color = if (isOnTrack) TextPrimary.copy(alpha = 0.15f) else PrimaryAccent.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                        )
                                    }

                                    if (actualFraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = actualFraction)
                                                .height(8.dp)
                                                .background(
                                                    color = if (isOnTrack) TextPrimary else PrimaryAccent,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                        )
                                    }

                                    if (budgetFraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = budgetFraction)
                                                .height(20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(16.dp)
                                                    .background(if (isOnTrack) TextSecondary else PrimaryAccent)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))


                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "0",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    val budgetLimitLabel = when (budgetPeriodType) {
                                        "weekly" -> "Weekly"
                                        "custom" -> "Period"
                                        else -> "Monthly"
                                    }
                                    Text(
                                        text = "$budgetLimitLabel Budget Limit: ৳${String.format(Locale.US, "%,.0f", budgetLimit)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))


                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 10.dp, height = 6.dp)
                                                .background(if (isOnTrack) TextPrimary else PrimaryAccent, RoundedCornerShape(1.dp))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Spent", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 10.dp, height = 6.dp)
                                                .background(if (isOnTrack) TextPrimary.copy(alpha = 0.2f) else PrimaryAccent.copy(alpha = 0.25f), RoundedCornerShape(1.dp))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Forecasted", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 2.dp, height = 8.dp)
                                                .background(if (isOnTrack) TextSecondary else PrimaryAccent)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Budget Wall", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))


                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ThemeBackground, RoundedCornerShape(16.dp))
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Speed,
                                        contentDescription = "Pace",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "৳${String.format(Locale.US, "%,.0f", dailyRate)} / day",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Daily Pace",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }

                                Box(modifier = Modifier.width(1.dp).height(32.dp).background(CardSurface))


                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = "Projected Spend",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "৳${String.format(Locale.US, "%,.0f", projected)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isOnTrack) TextPrimary else PrimaryAccent
                                    )
                                    Text(
                                        text = when (budgetPeriodType) {
                                            "weekly" -> "Est. Week End"
                                            "custom" -> "Est. Period End"
                                            else -> "Est. Month End"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }

                                Box(modifier = Modifier.width(1.dp).height(32.dp).background(CardSurface))


                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isOnTrack) Icons.Rounded.Savings else Icons.Rounded.Warning,
                                        contentDescription = "Outcome",
                                        tint = if (isOnTrack) TextSecondary else PrimaryAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${if (isOnTrack) "+" else "-"}৳${String.format(Locale.US, "%,.0f", Math.abs(predictedSavings))}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isOnTrack) TextPrimary else PrimaryAccent
                                    )
                                    Text(
                                        text = if (isOnTrack) "Est. Savings" else "Est. Overrun",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))


                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isOnTrack) TextPrimary.copy(alpha = 0.05f) else PrimaryAccent.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isOnTrack) TextPrimary.copy(alpha = 0.1f) else PrimaryAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isOnTrack) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                        contentDescription = null,
                                        tint = if (isOnTrack) TextPrimary else PrimaryAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isOnTrack)
                                            "At this daily pace, you'll stay under budget and save ৳${String.format(Locale.US, "%,.0f", predictedSavings)} this month."
                                        else
                                            "Warning: You're spending too fast! At this pace, you'll exceed your budget by ৳${String.format(Locale.US, "%,.0f", -predictedSavings)}.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = if (isOnTrack) TextPrimary else PrimaryAccent
                                    )
                                }
                            }
                        }
                    }
                }


                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ThemeBackground, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Spending Insights", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(16.dp))


                            biggestExpense?.let { exp ->
                                InsightRow(
                                    icon = Icons.Rounded.Receipt,
                                    iconBg = ThemeBackground,
                                    iconTint = PrimaryAccent,
                                    label = "Biggest Expense",
                                    value = "৳${String.format(Locale.US, "%,.0f", exp.amount)}",
                                    subtitle = "${exp.description} (${exp.category})"
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))


                            mostActiveDay?.let { (day, count) ->
                                InsightRow(
                                    icon = Icons.Rounded.CalendarMonth,
                                    iconBg = ThemeBackground,
                                    iconTint = TextPrimary,
                                    label = "Most Active Day",
                                    value = day,
                                    subtitle = "$count transactions"
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))


                            InsightRow(
                                icon = Icons.Rounded.Calculate,
                                iconBg = ThemeBackground,
                                iconTint = TextPrimary,
                                label = "Avg. per Transaction",
                                value = "৳${String.format(Locale.US, "%,.0f", avgPerTransaction)}",
                                subtitle = "across ${expenses.size} entries"
                            )

                            Spacer(modifier = Modifier.height(12.dp))


                            InsightRow(
                                icon = Icons.Rounded.Category,
                                iconBg = ThemeBackground,
                                iconTint = TextSecondary,
                                label = "Categories Used",
                                value = "${categoryData.size}",
                                subtitle = "unique spending categories"
                            )
                        }
                    }
                }
            }
        } else {
            // Debts & Dues Stats Dashboard
            if (debtsDues.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Handshake,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No liability data to analyze yet.",
                                textAlign = TextAlign.Center,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Add debts or receivables on the logs page to unlock insights.",
                                textAlign = TextAlign.Center,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                // Card 1: Net Position Overview
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ThemeBackground, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (netPositionVal >= 0) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                        contentDescription = null,
                                        tint = if (netPositionVal >= 0) Color(0xFF4CAF50) else Color(0xFFEA3B35),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Net Position",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            val statusColor = if (netPositionVal >= 0) Color(0xFF4CAF50) else Color(0xFFEA3B35)
                            val sign = if (netPositionVal > 0) "+" else ""
                            Text(
                                text = "${sign}৳${String.format(Locale.US, "%,.0f", netPositionVal)}",
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                                color = statusColor
                            )
                            Text(
                                text = if (netPositionVal >= 0) {
                                    "Others owe you more than you owe."
                                } else {
                                    "You owe more than others owe you."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )
                            
                            HorizontalDivider(color = ThemeBackground, modifier = Modifier.padding(vertical = 4.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "You Owe",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "৳${String.format(Locale.US, "%,.0f", totalDebtsVal)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = Color(0xFFEA3B35)
                                    )
                                }
                                
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(ThemeBackground))
                                
                                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                                    Text(
                                        text = "You Are Owed",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "৳${String.format(Locale.US, "%,.0f", totalDuesVal)}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 2: Composition Ratio Chart
                item {
                    val totalSum = totalDebtsVal + totalDuesVal
                    val debtFraction = if (totalSum > 0) (totalDebtsVal / totalSum).toFloat() else 0.5f
                    val dueFraction = if (totalSum > 0) (totalDuesVal / totalSum).toFloat() else 0.5f
                    
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ThemeBackground, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PieChart,
                                        contentDescription = null,
                                        tint = PrimaryAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Composition",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Horizontal progress bar split
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ThemeBackground)
                            ) {
                                if (totalSum == 0.0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(TextSecondary.copy(alpha = 0.3f))
                                    )
                                } else {
                                    if (debtFraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction = debtFraction)
                                                .background(Color(0xFFEA3B35))
                                        )
                                    }
                                    if (dueFraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth()
                                                .background(Color(0xFF4CAF50))
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFFEA3B35), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Debts: ${String.format(Locale.US, "%.0f", debtFraction * 100)}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextSecondary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF4CAF50), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Receivables: ${String.format(Locale.US, "%.0f", dueFraction * 100)}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 3: Settlement Progress Indicators
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ThemeBackground, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = PrimaryAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Settlement Progress",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Debt payback progress
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Debts Repaid",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${settledDebtsCount}/${totalDebtsCount} cleared (${String.format(Locale.US, "%.0f", debtSettlementPct * 100)}%)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { debtSettlementPct.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFFEA3B35),
                                    trackColor = ThemeBackground,
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Due collection progress
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Receivables Collected",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${settledDuesCount}/${totalDuesCount} cleared (${String.format(Locale.US, "%.0f", dueSettlementPct * 100)}%)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { dueSettlementPct.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFF4CAF50),
                                    trackColor = ThemeBackground,
                                )
                            }
                        }
                    }
                }

                // Card 4: Overdue Warning Panel (Only visible if there are overdue items)
                if (overdueItems.isNotEmpty()) {
                    item {
                        val cardBgColor = Color(0xFFEA3B35).copy(alpha = 0.1f)
                        val cardBorderColor = Color(0xFFEA3B35).copy(alpha = 0.2f)
                        
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = BorderStroke(1.dp, cardBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(ThemeBackground, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFEA3B35),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Overdue Actions (${overdueItems.size})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFEA3B35)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                overdueItems.take(3).forEach { overdue ->
                                    val isDebt = overdue.type == "DEBT"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = if (isDebt) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                                contentDescription = null,
                                                tint = if (isDebt) Color(0xFFEA3B35) else Color(0xFF4CAF50),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isDebt) "Pay ${overdue.personName}" else "Collect from ${overdue.personName}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = TextPrimary
                                            )
                                        }
                                        Text(
                                            text = "৳${String.format(Locale.US, "%,.0f", overdue.amount)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Card 5: Top Entities Breakdown
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ThemeBackground, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.People,
                                        contentDescription = null,
                                        tint = PrimaryAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Top Partners Breakdown",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Top Creditors (Who you owe)
                            Text(
                                text = "TOP PEOPLE YOU OWE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (topCreditors.isEmpty()) {
                                Text(
                                    text = "No pending debts.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            } else {
                                topCreditors.forEach { (name, amount) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            "৳${String.format(Locale.US, "%,.0f", amount)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                            color = Color(0xFFEA3B35)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            HorizontalDivider(color = ThemeBackground, modifier = Modifier.padding(vertical = 4.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Top Debtors (Who owes you)
                            Text(
                                text = "TOP PEOPLE WHO OWE YOU",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (topDebtors.isEmpty()) {
                                Text(
                                    text = "No pending receivables.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            } else {
                                topDebtors.forEach { (name, amount) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            "৳${String.format(Locale.US, "%,.0f", amount)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun InsightRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = 0.7f))
        }
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
            color = TextPrimary
        )
    }
}


data class HistorySnapshot(
    val filteredExpenses: List<Expense>,
    val groupedExpenses: Map<String, List<Expense>>
)

@Composable
fun HistoryTab(
    viewModel: MainViewModel,
    expenses: List<Expense>,
    debtsDues: List<DebtDue>,
    activeHistorySection: String,
    onSectionChange: (String) -> Unit,
    onDeleteExpense: (Int) -> Unit,
    onSettleDebtDue: (DebtDue, Double, Boolean, Int) -> Unit,
    onDeleteDebtDue: (Int) -> Unit,
    hideIncome: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    var activeSection by remember(activeHistorySection) { mutableStateOf(activeHistorySection) }

    val accounts by viewModel.accounts.collectAsState()
    val plannedTransactions by viewModel.plannedTransactions.collectAsState()
    var showAddPlannedDialog by remember { mutableStateOf(false) }
    var plannedToEdit by remember { mutableStateOf<PlannedTransaction?>(null) }

    val handleSectionChange = { sec: String ->
        activeSection = sec
        onSectionChange(sec)
    }



    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("date_desc") }
    var dateFilter by remember { mutableStateOf("all") }
    var amountFilter by remember { mutableStateOf("all") }
    var historyCustomStartDate by remember { mutableStateOf(0L) }
    var historyCustomEndDate by remember { mutableStateOf(0L) }
    var groupByOption by remember { mutableStateOf("none") }

    val baseExpenses = remember(expenses, hideIncome) {
        if (hideIncome) expenses.filter { it.type != "INCOME" } else expenses
    }

    val categories = remember(baseExpenses) {
        listOf("All") + baseExpenses.map { it.category }.distinct()
    }

    val historySnapshot by produceState(initialValue = HistorySnapshot(emptyList(), emptyMap()), searchQuery, selectedCategory, dateFilter, amountFilter, sortBy, baseExpenses, historyCustomStartDate, historyCustomEndDate, groupByOption) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val weekStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val filtered = baseExpenses.filter { expense ->
                val matchesSearch = expense.description.contains(searchQuery, ignoreCase = true) ||
                        expense.category.contains(searchQuery, ignoreCase = true)
                val matchesCategory = selectedCategory == "All" || expense.category == selectedCategory
                val matchesDate = when (dateFilter) {
                    "today" -> expense.date >= todayStart
                    "week" -> expense.date >= weekStart
                    "month" -> expense.date >= monthStart
                    "custom" -> {
                        val start = if (historyCustomStartDate > 0L) historyCustomStartDate else 0L
                        val end = if (historyCustomEndDate > 0L) historyCustomEndDate else Long.MAX_VALUE
                        expense.date in start..end
                    }
                    else -> true
                }
                val matchesAmount = when (amountFilter) {
                    "low" -> expense.amount < 500.0
                    "medium" -> expense.amount in 500.0..2000.0
                    "high" -> expense.amount > 2000.0
                    else -> true
                }
                matchesSearch && matchesCategory && matchesDate && matchesAmount
            }.sortedWith { a, b ->
                when (sortBy) {
                    "date_asc" -> a.date.compareTo(b.date)
                    "amount_desc" -> b.amount.compareTo(a.amount)
                    "amount_asc" -> a.amount.compareTo(b.amount)
                    else -> b.date.compareTo(a.date)
                }
            }

            val grouped = if (groupByOption == "none") {
                emptyMap()
            } else {
                val displaySdf = if (groupByOption == "weekly") {
                    SimpleDateFormat("'Week of' MMM dd, yyyy", Locale.US)
                } else if (groupByOption == "yearly") {
                    SimpleDateFormat("yyyy", Locale.US)
                } else {
                    SimpleDateFormat("MMMM yyyy", Locale.US)
                }

                filtered.groupBy { expense ->
                    if (groupByOption == "weekly") {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = expense.date
                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        }
                        displaySdf.format(cal.time)
                    } else {
                        displaySdf.format(Date(expense.date))
                    }
                }
            }
            HistorySnapshot(filtered, grouped)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Redesigned Top Header: Title + Icon Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ledger & Logs",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )
            }
            
        }

        // Sub-Navigation Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(CardSurface, RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val sections = listOf(
                "transactions" to "Transactions",
                "debts" to "Debts",
                "dues" to "Receivables",
                "planned" to "Planned"
            )
            sections.forEach { (secKey, secLabel) ->
                val isSelected = activeSection == secKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) PrimaryAccent else Color.Transparent
                        )
                        .clickable { handleSectionChange(secKey) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = secLabel,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (activeSection == "transactions") {
            // Search Bar with Trailing Filter Icon
            var showFilterPanel by remember { mutableStateOf(false) }
            val isFilterActive = dateFilter != "all" || amountFilter != "all" || sortBy != "date_desc" || selectedCategory != "All" || groupByOption != "none"

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search logs...") },
                leadingIcon = { Icon(Icons.Rounded.Search, "Search", tint = TextSecondary) },
                trailingIcon = {
                    IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = "Toggle Filters",
                            tint = if (isFilterActive) PrimaryAccent else TextPrimary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
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

            // Collapsible Filter Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                if (showFilterPanel) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Filters & Grouping", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                Text(
                                    text = "Clear All",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryAccent,
                                    modifier = Modifier.clickable {
                                        sortBy = "date_desc"
                                        dateFilter = "all"
                                        amountFilter = "all"
                                        selectedCategory = "All"
                                        groupByOption = "none"
                                        historyCustomStartDate = 0L
                                        historyCustomEndDate = 0L
                                    }
                                )
                            }

                            // 1. Sort & Filters Dropdown Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Sort By Dropdown
                                var sortMenuExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { sortMenuExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                        border = BorderStroke(1.dp, ThemeBackground)
                                    ) {
                                        val label = when (sortBy) {
                                            "date_asc" -> "Oldest"
                                            "amount_desc" -> "Price: High"
                                            "amount_asc" -> "Price: Low"
                                            else -> "Newest"
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.Sort, null, modifier = Modifier.size(14.dp), tint = PrimaryAccent)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Icon(Icons.Rounded.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = sortMenuExpanded,
                                        onDismissRequest = { sortMenuExpanded = false },
                                        modifier = Modifier.background(ThemeBackground)
                                    ) {
                                        listOf(
                                            "date_desc" to "Newest First",
                                            "date_asc" to "Oldest First",
                                            "amount_desc" to "Price: High to Low",
                                            "amount_asc" to "Price: Low to High"
                                        ).forEach { (valKey, valLabel) ->
                                            DropdownMenuItem(
                                                text = { Text(valLabel, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                                onClick = {
                                                    sortBy = valKey
                                                    sortMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Date Filter Dropdown
                                var dateMenuExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { dateMenuExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                        border = BorderStroke(1.dp, ThemeBackground)
                                    ) {
                                        val label = when (dateFilter) {
                                            "today" -> "Today"
                                            "week" -> "This Week"
                                            "month" -> "This Month"
                                            "custom" -> "Custom"
                                            else -> "All Time"
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.DateRange, null, modifier = Modifier.size(14.dp), tint = PrimaryAccent)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Icon(Icons.Rounded.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = dateMenuExpanded,
                                        onDismissRequest = { dateMenuExpanded = false },
                                        modifier = Modifier.background(ThemeBackground)
                                    ) {
                                        listOf(
                                            "all" to "All Time",
                                            "today" to "Today",
                                            "week" to "This Week",
                                            "month" to "This Month",
                                            "custom" to "Custom Range"
                                        ).forEach { (valKey, valLabel) ->
                                            DropdownMenuItem(
                                                text = { Text(valLabel, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                                onClick = {
                                                    dateFilter = valKey
                                                    dateMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Amount Filter Dropdown
                                var amountMenuExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { amountMenuExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                        border = BorderStroke(1.dp, ThemeBackground)
                                    ) {
                                        val label = when (amountFilter) {
                                            "low" -> "< ৳500"
                                            "medium" -> "৳500-2k"
                                            "high" -> "> ৳2k"
                                            else -> "All Prices"
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.Payments, null, modifier = Modifier.size(14.dp), tint = PrimaryAccent)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Icon(Icons.Rounded.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = amountMenuExpanded,
                                        onDismissRequest = { amountMenuExpanded = false },
                                        modifier = Modifier.background(ThemeBackground)
                                    ) {
                                        listOf(
                                            "all" to "All Prices",
                                            "low" to "Under ৳500",
                                            "medium" to "৳500 - ৳2,000",
                                            "high" to "Over ৳2,000"
                                        ).forEach { (valKey, valLabel) ->
                                            DropdownMenuItem(
                                                text = { Text(valLabel, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                                onClick = {
                                                    amountFilter = valKey
                                                    amountMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Custom Date Range Pickers (if dateFilter == "custom")
                            if (dateFilter == "custom") {
                                val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.US)
                                val startLabel = if (historyCustomStartDate > 0L) sdf.format(Date(historyCustomStartDate)) else "Add Date"
                                val endLabel = if (historyCustomEndDate > 0L) sdf.format(Date(historyCustomEndDate)) else "Add Date"

                                val startCal = Calendar.getInstance().apply {
                                    if (historyCustomStartDate > 0L) timeInMillis = historyCustomStartDate
                                }
                                val startDatePickerDialog = remember(historyCustomStartDate) {
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedCal = Calendar.getInstance()
                                            selectedCal.set(year, month, dayOfMonth, 0, 0, 0)
                                            selectedCal.set(Calendar.MILLISECOND, 0)
                                            val endVal = if (historyCustomEndDate > selectedCal.timeInMillis) historyCustomEndDate else 0L
                                            historyCustomStartDate = selectedCal.timeInMillis
                                            historyCustomEndDate = endVal
                                        },
                                        startCal.get(Calendar.YEAR),
                                        startCal.get(Calendar.MONTH),
                                        startCal.get(Calendar.DAY_OF_MONTH)
                                    )
                                }

                                val endCal = Calendar.getInstance().apply {
                                    if (historyCustomEndDate > 0L) timeInMillis = historyCustomEndDate
                                }
                                val endDatePickerDialog = remember(historyCustomEndDate) {
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedCal = Calendar.getInstance()
                                            selectedCal.set(year, month, dayOfMonth, 23, 59, 59)
                                            selectedCal.set(Calendar.MILLISECOND, 999)
                                            val startVal = if (historyCustomStartDate > 0L && historyCustomStartDate < selectedCal.timeInMillis) historyCustomStartDate else 0L
                                            historyCustomStartDate = startVal
                                            historyCustomEndDate = selectedCal.timeInMillis
                                        },
                                        endCal.get(Calendar.YEAR),
                                        endCal.get(Calendar.MONTH),
                                        endCal.get(Calendar.DAY_OF_MONTH)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ThemeBackground)
                                            .clickable { startDatePickerDialog.show() }
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("START DATE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = TextSecondary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(startLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (historyCustomStartDate > 0L) TextPrimary else TextSecondary)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ThemeBackground)
                                            .clickable { endDatePickerDialog.show() }
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("END DATE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = TextSecondary)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(endLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (historyCustomEndDate > 0L) TextPrimary else TextSecondary)
                                        }
                                    }
                                }
                            }

                            // 3. Group By Selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Group:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextSecondary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "none" to "None",
                                        "weekly" to "Weekly",
                                        "monthly" to "Monthly",
                                        "yearly" to "Yearly"
                                    ).forEach { (optKey, optLabel) ->
                                        item {
                                            val isSelected = groupByOption == optKey
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) PrimaryAccent else ThemeBackground)
                                                    .clickable { groupByOption = optKey }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = optLabel,
                                                    color = if (isSelected) Color.White else TextPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Category Filter Chips
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(categories, key = { it }) { cat ->
                                    val isSelected = selectedCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) PrimaryAccent else ThemeBackground)
                                            .clickable { selectedCategory = cat }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = cat,
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val bottomPadding = 112.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = bottomPadding)
            ) {
                if (historySnapshot.filteredExpenses.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "No matching transactions found.",
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                textAlign = TextAlign.Center,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    if (groupByOption == "none") {
                        items(historySnapshot.filteredExpenses, key = { it.id }) { expense ->
                            ExpenseItem(
                                expense = expense,
                                accounts = accounts,
                                onDelete = { expenseToDelete = expense }
                            )
                        }
                    } else {
                        for (entry in historySnapshot.groupedExpenses.entries) {
                            val groupLabel = entry.key
                            val groupList = entry.value
                            val netSubtotal = groupList.sumOf { exp ->
                                if (exp.type == "INCOME") exp.amount else -exp.amount 
                            }
                            val formattedSubtotal = String.format(Locale.US, "%,.2f", Math.abs(netSubtotal))
                            val netPrefix = if (netSubtotal >= 0) "+" else "-"
                            val netColor = if (netSubtotal >= 0) Color(0xFF4CAF50) else Color(0xFFEA3B35)

                            item(key = groupLabel) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = groupLabel.uppercase(),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        ),
                                        color = PrimaryAccent
                                    )
                                    Text(
                                        text = "Net: $netPrefix৳$formattedSubtotal",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = netColor
                                    )
                                }
                            }

                            items(groupList, key = { it.id }) { expense ->
                                ExpenseItem(
                                    expense = expense,
                                    accounts = accounts,
                                    onDelete = { expenseToDelete = expense }
                                )
                            }
                        }
                    }
                }
            }
        } else if (activeSection == "debts") {
            DebtsSection(
                type = "DEBT",
                debtsDues = debtsDues,
                accounts = accounts,
                onSettleDebtDue = onSettleDebtDue,
                onDeleteDebtDue = onDeleteDebtDue
            )
        } else if (activeSection == "dues") {
            DebtsSection(
                type = "DUE",
                debtsDues = debtsDues,
                accounts = accounts,
                onSettleDebtDue = onSettleDebtDue,
                onDeleteDebtDue = onDeleteDebtDue
            )
        } else if (activeSection == "planned") {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scheduled Cycles",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Button(
                            onClick = { showAddPlannedDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Rounded.Add, "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Cycle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (plannedTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No planned cycles scheduled yet.", color = TextSecondary)
                        }
                    }
                } else {
                    items(plannedTransactions, key = { it.id }) { planned ->
                        val now = System.currentTimeMillis()
                        val isOverdue = planned.nextDueDate < now && planned.isActive
                        val accountName = accounts.find { it.id == planned.accountId }?.name ?: "Unassigned"
                        
                        PlannedTransactionItem(
                            planned = planned,
                            accountName = accountName,
                            isOverdue = isOverdue,
                            onPay = { viewModel.executePlannedTransaction(planned) },
                            onSkip = { viewModel.skipPlannedTransaction(planned) },
                            onClick = { plannedToEdit = planned }
                        )
                    }
                }
            }
        }
    }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Transaction", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this transaction for '${expenseToDelete?.description}'?", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        expenseToDelete?.let {
                            onDeleteExpense(it.id)
                        }
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ThemeBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showAddPlannedDialog) {
        val existingCategories = remember(expenses, plannedTransactions) {
            (expenses.map { it.category } + plannedTransactions.map { it.category }).distinct()
        }
        AddPlannedTransactionDialog(
            accounts = accounts,
            existingCategories = existingCategories,
            onDismiss = { showAddPlannedDialog = false },
            onConfirm = { title, amt, cat, type, accId, start, intervalType, intervalN, oneTime, desc ->
                viewModel.addPlannedTransaction(title, amt, cat, type, accId, start, intervalType, intervalN, oneTime, desc)
                showAddPlannedDialog = false
            }
        )
    }

    if (plannedToEdit != null) {
        val existingCategories = remember(expenses, plannedTransactions) {
            (expenses.map { it.category } + plannedTransactions.map { it.category }).distinct()
        }
        AddPlannedTransactionDialog(
            plannedToEdit = plannedToEdit,
            accounts = accounts,
            existingCategories = existingCategories,
            onDismiss = { plannedToEdit = null },
            onConfirm = { title, amt, cat, type, accId, start, intervalType, intervalN, oneTime, desc ->
                plannedToEdit?.let {
                    viewModel.updatePlannedTransaction(it.copy(
                        title = title,
                        amount = amt,
                        category = cat,
                        type = type,
                        accountId = accId,
                        startDate = start,
                        intervalType = intervalType,
                        intervalN = intervalN,
                        oneTime = oneTime,
                        description = desc
                    ))
                }
                plannedToEdit = null
            },
            onDelete = {
                plannedToEdit?.let { viewModel.deletePlannedTransaction(it) }
                plannedToEdit = null
            }
        )
    }
}

@Composable
fun SheetInfoRow(name: String, cols: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .border(1.dp, CardSurface, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(PrimaryAccent, CircleShape)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = cols,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = TextSecondary,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun ExcelImportGuideDialog(
    onDismiss: () -> Unit,
    onChooseFile: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = ThemeBackground
            ),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(8.dp)
                .border(1.dp, CardSurface, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(PrimaryAccent.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = null,
                                tint = PrimaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Excel Import Guide",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(CardSurface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Tab Selection
                var selectedTab by remember { mutableStateOf(0) } // 0 = Full Restore, 1 = Legacy Transactions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSurface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) PrimaryAccent else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Full Restore",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) Color.White else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) PrimaryAccent else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Legacy (TX Only)",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) Color.White else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (selectedTab == 0) {
                    Text(
                        text = "Import a full backup spreadsheet containing multiple sheets to restore your entire wallet database context:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SheetInfoRow(name = "Accounts", cols = "Name, Balance, ColorHex, Icon, Currency, IncludeInBalance, DisplayOrder")
                        SheetInfoRow(name = "Expenses", cols = "Date, Category, Description, Amount, Type, Account, ToAccount, Tags")
                        SheetInfoRow(name = "Debts & Receivables", cols = "Date, Person, Type, Description, Amount, Due Date, Status")
                        SheetInfoRow(name = "Planned Transactions", cols = "Title, Amount, Category, Type, Account, Start Date, Interval Type, Interval N, One Time, Next Due Date, Is Active, Description")
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        BulletPoint("Auto-link Wallets:", "Transactions and planned schedules automatically map to accounts by matching Name (case-insensitive).")
                        BulletPoint("Balance Restoration:", "Wallet balances are restored exactly as specified in the spreadsheet, without double-adjusting from transactions.")
                        BulletPoint("New Account Insertion:", "If a transaction references a wallet that doesn't exist, it will be automatically created.")
                    }
                } else {
                    Text(
                        text = "To import transactions only, ensure your Excel (.xls or .xlsx) file has a single sheet with this column structure:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    // Table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardSurface, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        val weights = listOf(1.0f, 0.85f, 1.45f, 0.7f)

                        // Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PrimaryAccent.copy(alpha = 0.08f))
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val headers = listOf(
                                "Date" to Icons.Rounded.DateRange,
                                "Category" to Icons.Rounded.Category,
                                "Description" to Icons.Rounded.Description,
                                "Amount" to Icons.Rounded.Payments
                            )
                            headers.forEachIndexed { index, (text, icon) ->
                                Row(
                                    modifier = Modifier.weight(weights[index]),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = PrimaryAccent,
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = text,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryAccent,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        
                        // Divider
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardSurface))

                        // Row 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val rowData = listOf("2026-06-22", "Food", "Lunch\nwith team", "1250.0")
                            rowData.forEachIndexed { index, valStr ->
                                Text(
                                    text = valStr,
                                    modifier = Modifier.weight(weights[index]),
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    color = TextPrimary,
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        // Divider
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardSurface))

                        // Row 2
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val rowData = listOf("2026-06-21", "Bills", "Electricity\nBill", "4500.0")
                            rowData.forEachIndexed { index, valStr ->
                                Text(
                                    text = valStr,
                                    modifier = Modifier.weight(weights[index]),
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    color = TextPrimary,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }

                    // Guidelines List
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        BulletPoint("Columns must include:", "Date, Category, Description, and Amount (headers are case-insensitive).")
                        BulletPoint("Supported Date Formats:", "YYYY-MM-DD, YYYY-MM-DD HH:mm:ss, DD/MM/YYYY, or Excel date cell format.")
                        BulletPoint("Amount", "must be a positive number greater than 0.")
                        BulletPoint("Empty rows", "or rows with invalid values are skipped automatically.")
                    }
                }

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardSurface),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            onDismiss()
                            onChooseFile()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryAccent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload File", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BulletPoint(boldPrefix: String, normalSuffix: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = PrimaryAccent,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                    append(boldPrefix)
                }
                append(" ")
                withStyle(SpanStyle(color = TextSecondary)) {
                    append(normalSuffix)
                }
            },
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 16.sp
        )
    }
}


@Composable
fun ProfileTab(
    viewModel: MainViewModel,
    userName: String,
    profileImageBitmap: Bitmap?,
    onUploadImageClick: () -> Unit,
    onUpdateName: (String) -> Unit,
    budgetLimit: Double,
    biometricsEnabled: Boolean,
    themeSelection: String,
    activity: FragmentActivity,
    expenses: List<Expense>,
    debtsDues: List<DebtDue>,
    onRequestStoragePermission: (() -> Unit) -> Unit
) {
    val context = LocalContext.current
    val budgetPeriodType by viewModel.budgetPeriodType.collectAsState()
    val budgetCustomStartDate by viewModel.budgetCustomStartDate.collectAsState()
    val budgetCustomEndDate by viewModel.budgetCustomEndDate.collectAsState()
    val hideBalance by viewModel.hideBalance.collectAsState()
    val hideIncome by viewModel.hideIncome.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val plannedTransactions by viewModel.plannedTransactions.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    var showImportGuide by remember { mutableStateOf(false) }
    var showExportFormatDialog by remember { mutableStateOf(false) }
    
    var inputBudget by remember { mutableStateOf(budgetLimit.toString()) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember(userName) { mutableStateOf(userName) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isImporting = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val importedData = ExcelHelper.importAllData(inputStream)
                        withContext(Dispatchers.Main) {
                            viewModel.importAllData(importedData) { success, count ->
                                isImporting = false
                                if (success) {
                                    Toast.makeText(context, "Successfully imported $count records!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to save imported data.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        isImporting = false
                        Toast.makeText(context, "Import failed: ${e.javaClass.simpleName} - ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        ExcelHelper.exportAllData(outputStream, accounts, expenses, debtsDues, plannedTransactions)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Exported successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Export failed: ${e.javaClass.simpleName} - ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        PdfHelper.exportAllData(outputStream, expenses, debtsDues)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Exported successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Export failed: ${e.javaClass.simpleName} - ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Username", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editNameInput,
                    onValueChange = { editNameInput = it },
                    label = { Text("Username") },
                    shape = RoundedCornerShape(16.dp),
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameInput.trim().isNotEmpty()) {
                            onUpdateName(editNameInput.trim())
                            showEditNameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ThemeBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    val bottomPadding = 112.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = bottomPadding, top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Personal Profile",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Customize settings, security, and developer credits.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(ThemeBackground, CircleShape)
                            .clickable { onUploadImageClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImageBitmap != null) {
                            Image(
                                bitmap = profileImageBitmap.asImageBitmap(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Profile Avatar",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = "Edit photo",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp).padding(bottom = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit Name",
                                tint = PrimaryAccent,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { showEditNameDialog = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .background(ThemeBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, CardSurface, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = "Saver Level badge",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Smart Saver Elite",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Savings & Budget Target",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = inputBudget,
                        onValueChange = {
                            inputBudget = it
                            val limit = it.toDoubleOrNull()
                            if (limit != null) {
                                viewModel.updateBudgetLimit(limit)
                            }
                        },
                        label = { Text("Budget Target (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("budget_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = ThemeBackground,
                            focusedContainerColor = ThemeBackground,
                            unfocusedContainerColor = ThemeBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Budget Cycle Period",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "monthly" to "Monthly",
                            "weekly" to "Weekly",
                            "custom" to "Custom"
                        ).forEach { (typeKey, typeLabel) ->
                            val isSelected = budgetPeriodType == typeKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryAccent else ThemeBackground)
                                    .clickable {
                                        if (typeKey == "custom" && budgetCustomStartDate == 0L) {
                                            val defaultStart = Calendar.getInstance().apply {
                                                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                            }.timeInMillis
                                            val defaultEnd = Calendar.getInstance().apply {
                                                timeInMillis = defaultStart
                                                add(Calendar.DAY_OF_YEAR, 30)
                                                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                                            }.timeInMillis
                                            viewModel.updateBudgetPeriod("custom", defaultStart, defaultEnd)
                                        } else {
                                            viewModel.updateBudgetPeriod(typeKey, budgetCustomStartDate, budgetCustomEndDate)
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typeLabel,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (budgetPeriodType == "custom") {
                        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.US)
                        val startLabel = if (budgetCustomStartDate > 0L) sdf.format(Date(budgetCustomStartDate)) else "Add Date"
                        val endLabel = if (budgetCustomEndDate > 0L) sdf.format(Date(budgetCustomEndDate)) else "Add Date"

                        val startCal = Calendar.getInstance().apply {
                            if (budgetCustomStartDate > 0L) timeInMillis = budgetCustomStartDate
                        }
                        val startDatePickerDialog = remember(budgetCustomStartDate) {
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selectedCal = Calendar.getInstance()
                                    selectedCal.set(year, month, dayOfMonth, 0, 0, 0)
                                    selectedCal.set(Calendar.MILLISECOND, 0)
                                    val endVal = if (budgetCustomEndDate > selectedCal.timeInMillis) budgetCustomEndDate else selectedCal.timeInMillis + (24L * 60 * 60 * 1000 * 30)
                                    viewModel.updateBudgetPeriod("custom", selectedCal.timeInMillis, endVal)
                                },
                                startCal.get(Calendar.YEAR),
                                startCal.get(Calendar.MONTH),
                                startCal.get(Calendar.DAY_OF_MONTH)
                            )
                        }

                        val endCal = Calendar.getInstance().apply {
                            if (budgetCustomEndDate > 0L) timeInMillis = budgetCustomEndDate
                        }
                        val endDatePickerDialog = remember(budgetCustomEndDate) {
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selectedCal = Calendar.getInstance()
                                    selectedCal.set(year, month, dayOfMonth, 23, 59, 59)
                                    selectedCal.set(Calendar.MILLISECOND, 999)
                                    val startVal = if (budgetCustomStartDate > 0L && budgetCustomStartDate < selectedCal.timeInMillis) budgetCustomStartDate else selectedCal.timeInMillis - (24L * 60 * 60 * 1000 * 30)
                                    viewModel.updateBudgetPeriod("custom", startVal, selectedCal.timeInMillis)
                                },
                                endCal.get(Calendar.YEAR),
                                endCal.get(Calendar.MONTH),
                                endCal.get(Calendar.DAY_OF_MONTH)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Custom Date Range",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ThemeBackground)
                                    .border(1.dp, CardSurface, RoundedCornerShape(12.dp))
                                    .clickable { startDatePickerDialog.show() }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("START DATE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(startLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (budgetCustomStartDate > 0L) TextPrimary else TextSecondary)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ThemeBackground)
                                    .border(1.dp, CardSurface, RoundedCornerShape(12.dp))
                                    .clickable { endDatePickerDialog.show() }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("END DATE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(endLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (budgetCustomEndDate > 0L) TextPrimary else TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Backup & Export",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showImportGuide = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.UploadFile,
                            contentDescription = "Import",
                            tint = PrimaryAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Import Logs",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Import transactions from an Excel file",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PrimaryAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = ThemeBackground.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (expenses.isEmpty()) {
                                    Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                                } else {
                                    showExportFormatDialog = true
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Export",
                            tint = PrimaryAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Export Logs",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Export your data to Excel or PDF format",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "App Security & Privacy",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Fingerprint, "Biometric", tint = PrimaryAccent, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Biometric Privacy", 
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), 
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Require scan on app start", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = biometricsEnabled,
                            onCheckedChange = { viewModel.updateBiometricsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryAccent,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = ThemeBackground,
                                uncheckedBorderColor = CardSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = ThemeBackground.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.VisibilityOff, "Hide Balance", tint = PrimaryAccent, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Hide Balance",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Mask totals on Home page",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = hideBalance,
                            onCheckedChange = { viewModel.updateHideBalance(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryAccent,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = ThemeBackground,
                                uncheckedBorderColor = CardSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = ThemeBackground.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.RemoveRedEye, "Hide Income", tint = PrimaryAccent, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Hide Income Logs",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Filter out all income from lists",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = hideIncome,
                            onCheckedChange = { viewModel.updateHideIncome(it) },
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
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "App Theme",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (themeSelection == "Dark") Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                contentDescription = "Theme Icon",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Dark Mode",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (themeSelection == "Dark") "Dark theme active" else "Light theme active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = themeSelection == "Dark",
                            onCheckedChange = { isDark ->
                                viewModel.updateTheme(if (isDark) "Dark" else "Light")
                            },
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
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Account Management",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Button(
                        onClick = {
                            showResetConfirmDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryAccent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, "Reset")
                            Text("Wipe & Reset App Data", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }


        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Azwad Abrar Nabil",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = TextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Developed & Created by Azwad Abrar Nabil",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nabil24024004"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open GitHub link", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Code,
                                    contentDescription = "GitHub",
                                    modifier = Modifier.size(16.dp),
                                    tint = PrimaryAccent
                                )
                                Text(
                                    text = "GitHub",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryAccent
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(width = 1.dp, height = 12.dp)
                                .background(ThemeBackground)
                        )

                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://abrarnabil.vercel.app/"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open portfolio link", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = "Portfolio",
                                    modifier = Modifier.size(16.dp),
                                    tint = PrimaryAccent
                                )
                                Text(
                                    text = "Portfolio",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryAccent
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(width = 1.dp, height = 12.dp)
                                .background(ThemeBackground)
                        )

                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:azwadabrar109@gmail.com")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not launch email app", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Email,
                                    contentDescription = "Contact",
                                    modifier = Modifier.size(16.dp),
                                    tint = PrimaryAccent
                                )
                                Text(
                                    text = "Contact",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryAccent
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Copyright,
                        contentDescription = "Copyright",
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val copyrightText = buildAnnotatedString {
                        append("Copyright ")
                        pushStringAnnotation(tag = "URL", annotation = "https://neosparkx.com")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = PrimaryAccent)) {
                            append("NeoSparkX")
                        }
                        pop()
                        append(". All rights reserved.")
                    }
                    ClickableText(
                        text = copyrightText,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                        onClick = { offset ->
                            copyrightText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                        }
                    )
                }
                Text(
                    text = "v${com.example.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.8f)
                )
            }
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Wipe & Reset App Data", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all transaction logs and reset the app? This action is permanent and cannot be undone.", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        Toast.makeText(context, "All App Data Cleared", Toast.LENGTH_LONG).show()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = ThemeBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showImportGuide) {
        ExcelImportGuideDialog(
            onDismiss = { showImportGuide = false },
            onChooseFile = {
                onRequestStoragePermission {
                    importLauncher.launch("*/*")
                }
            }
        )
    }

    if (showExportFormatDialog) {
        AlertDialog(
            onDismissRequest = { showExportFormatDialog = false },
            title = { Text("Export Records", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select your preferred format for exporting your transactions and ledger:", color = TextSecondary)
                    
                    Button(
                        onClick = {
                            showExportFormatDialog = false
                            onRequestStoragePermission {
                                pdfExportLauncher.launch("expenses_and_debts_export.pdf")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = Color.White)
                    ) {
                        Text("PDF Document (.pdf)", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            showExportFormatDialog = false
                            onRequestStoragePermission {
                                exportLauncher.launch("expenses_and_debts_export.xls")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurface, contentColor = TextPrimary)
                    ) {
                        Text("Excel Spreadsheet (.xls)", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportFormatDialog = false }) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = ThemeBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}


@Composable
fun ExpenseItem(
    expense: Expense,
    accounts: List<Account> = emptyList(),
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.US) }
    val dateStr = remember(expense.date) { dateFormat.format(Date(expense.date)) }
    val style = getCategoryStyle(expense.category)

    val account = remember(expense.accountId, accounts) {
        accounts.firstOrNull { it.id == expense.accountId }
    }

    val toAccount = remember(expense.toAccountId, accounts) {
        accounts.firstOrNull { it.id == expense.toAccountId }
    }

    val isIncome = remember(expense.type) { expense.type == "INCOME" }
    val isTransfer = remember(expense.type) { expense.type == "TRANSFER" }

    val accountColor = remember(account) {
        if (account != null) {
            try {
                Color(android.graphics.Color.parseColor(account.colorHex))
            } catch (e: Exception) {
                PrimaryAccent
            }
        } else {
            PrimaryAccent
        }
    }

    val tagsList = remember(expense.tags) {
        expense.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        imageVector = style.first,
                        contentDescription = expense.category,
                        tint = style.third,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.description.ifEmpty { expense.category },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$dateStr • ${expense.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                val amountColor = when {
                    isIncome -> Color(0xFF4CAF50)
                    isTransfer -> TextPrimary
                    else -> Color(0xFFEA3B35)
                }
                val amountPrefix = when {
                    isIncome -> "+"
                    isTransfer -> ""
                    else -> "-"
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
                    modifier = Modifier.size(36.dp).testTag("delete_expense_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = PrimaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (account != null || tagsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (account != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accountColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isTransfer && toAccount != null) {
                                    "${account.name} ➔ ${toAccount.name}"
                                } else {
                                    account.name
                                },
                                color = accountColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    tagsList.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CardSurface.copy(alpha = 0.8f))
                                .border(BorderStroke(0.5.dp, TextSecondary.copy(alpha = 0.3f)), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TechStackBadge(text: String) {
    Box(
        modifier = Modifier
            .background(CardSurface, RoundedCornerShape(8.dp))
            .border(0.5.dp, CardSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = TextSecondary
        )
    }
}

@Composable
fun DeveloperActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(CardSurface)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .width(76.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextPrimary,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
    }
}


@Composable
fun getCategoryStyle(category: String): Triple<androidx.compose.ui.graphics.vector.ImageVector, Color, Color> {
    val cleanCategory = category.lowercase(Locale.US).trim()
    val icon = when (cleanCategory) {
        "food" -> Icons.Rounded.Restaurant
        "shopping" -> Icons.Rounded.ShoppingBag
        "bills" -> Icons.Rounded.ReceiptLong
        "other" -> Icons.Rounded.AddCircleOutline
        "travel", "transport", "car" -> Icons.Rounded.DirectionsCar
        "entertainment", "games", "movies" -> Icons.Rounded.SportsEsports
        "medical", "health", "doctor" -> Icons.Rounded.MedicalServices
        "education", "school", "books" -> Icons.Rounded.School
        "salary", "income", "freelance", "tuition" -> Icons.Rounded.Payments
        "investment" -> Icons.Rounded.TrendingUp
        else -> {
            val hashCode = category.hashCode()
            val icons = listOf(
                Icons.Rounded.Category,
                Icons.Rounded.LocalMall,
                Icons.Rounded.LocalActivity,
                Icons.Rounded.Bookmark,
                Icons.Rounded.Work,
                Icons.Rounded.Celebration,
                Icons.Rounded.CardMembership
            )
            icons[Math.abs(hashCode) % icons.size]
        }
    }
    
    val tints = listOf(PrimaryAccent, TextPrimary, TextSecondary)
    val isIncomeCategory = cleanCategory in listOf("salary", "income", "freelance", "tuition", "investment", "gift", "debt received")
    val tint = if (isIncomeCategory) Color(0xFF4CAF50) else tints[Math.abs(category.hashCode()) % tints.size]
    
    return Triple(icon, CardSurface, tint)
}


data class NotificationItem(
    val title: String,
    val text: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val colorType: String,
    val severity: Int = 0
)


@Composable
private fun NotificationCard(item: NotificationItem) {
    val iconTint = when (item.colorType) {
        "accent" -> PrimaryAccent
        "primary" -> TextPrimary
        else -> TextSecondary
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, CardSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(ThemeBackground, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun TabContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val hasBeenVisible = remember { mutableStateOf(false) }
    if (visible) {
        hasBeenVisible.value = true
    }
    if (hasBeenVisible.value) {
        Box(
            modifier = modifier
                .then(if (visible) Modifier.fillMaxSize() else Modifier.size(0.dp))
        ) {
            content()
        }
    }
}

