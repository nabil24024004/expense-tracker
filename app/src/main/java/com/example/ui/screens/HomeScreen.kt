package com.example.ui.screens

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
import com.example.ui.screens.DebtsSection
import com.example.ui.components.GlassBox
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
    val groupViewModel: com.example.group.viewmodel.GroupViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val selectedGroupId by groupViewModel.selectedGroupId.collectAsState()
    
    var currentTab by remember { mutableStateOf("home") }
    var homeMode by remember { mutableStateOf("personal") } // "personal" or "group"
    var showAddEntryDialog by remember { mutableStateOf(false) }
    var addDialogPrefillCategory by remember { mutableStateOf("") }
    
    val debtsDues by viewModel.debtsDues.collectAsState()
    var activeHistorySection by remember { mutableStateOf("transactions") }
    var insightsSubTab by remember { mutableStateOf("ledger") }
    

    var showChatAssistant by remember { mutableStateOf(false) }
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
    val totalAmount = expenses.sumOf { it.amount }
    val existingCategories = remember(expenses) {
        (expenses.map { it.category.trim() }.filter { it.isNotEmpty() } + listOf("Food", "Other")).distinct()
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
            if (showAddEntryDialog) {
                AddEntryDialog(
                    initialCategory = addDialogPrefillCategory,
                    existingCategories = existingCategories,
                    onDismiss = { showAddEntryDialog = false },
                    onExpenseConfirm = { amt, desc, cat, date, imgBytes, foodDetails ->
                        viewModel.addExpense(amt, desc, cat, date, imgBytes, foodDetails)
                        showAddEntryDialog = false
                    },
                    onDebtConfirm = { name, amt, desc, type, dueDate ->
                        viewModel.addDebtDue(name, amt, desc, type, dueDate)
                        showAddEntryDialog = false
                    }
                )
            }

            when (currentTab) {
                "home" -> {
                    val selectedGroupId by groupViewModel.selectedGroupId.collectAsState()
                    if (selectedGroupId != null) {
                        com.example.group.ui.GroupsHomeScreen(
                            viewModel = groupViewModel,
                            showTopHeader = true
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ThemeBackground)
                        ) {
                            HomeHeaderRow(
                                userName = userName,
                                profileImageBitmap = profileImageBitmap,
                                hasUnreadNotifications = hasUnreadNotifications,
                                onNavigate = { tab ->
                                    if (tab.startsWith("history:")) {
                                        activeHistorySection = tab.substringAfter("history:")
                                        insightsSubTab = "ledger"
                                        currentTab = "insights"
                                    } else if (tab == "history") {
                                        insightsSubTab = "ledger"
                                        currentTab = "insights"
                                    } else if (tab == "analytics") {
                                        insightsSubTab = "analytics"
                                        currentTab = "insights"
                                    } else {
                                        currentTab = tab
                                    }
                                },
                                onShowNotifications = {
                                    viewModel.markNotificationsAsRead()
                                    showNotificationsDialog = true
                                }
                            )

                            HomeModeSwitcher(
                                selectedMode = homeMode,
                                onModeSelected = { homeMode = it }
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                if (homeMode == "personal") {
                                    HomeTab(
                                        viewModel = viewModel,
                                        userName = userName,
                                        profileImageBitmap = profileImageBitmap,
                                        totalAmount = totalAmount,
                                        budgetLimit = budgetLimit,
                                        expenses = expenses,
                                        hasUnreadNotifications = hasUnreadNotifications,
                                        onAddExpenseClick = { category ->
                                            addDialogPrefillCategory = category
                                            showAddEntryDialog = true
                                        },
                                        onNavigate = { tab ->
                                            if (tab.startsWith("history:")) {
                                                activeHistorySection = tab.substringAfter("history:")
                                                insightsSubTab = "ledger"
                                                currentTab = "insights"
                                            } else if (tab == "history") {
                                                insightsSubTab = "ledger"
                                                currentTab = "insights"
                                            } else if (tab == "analytics") {
                                                insightsSubTab = "analytics"
                                                currentTab = "insights"
                                            } else {
                                                currentTab = tab
                                            }
                                        },
                                        onShowNotifications = {
                                            viewModel.markNotificationsAsRead()
                                            showNotificationsDialog = true
                                        },
                                        showWelcomeHeader = false
                                    )
                                } else {
                                    com.example.group.ui.GroupsHomeScreen(
                                        viewModel = groupViewModel,
                                        showTopHeader = false,
                                        showFloatingActionButton = false
                                    )
                                }
                            }
                        }
                    }
                }
                "insights" -> {
                    InsightsTab(
                        expenses = expenses,
                        debtsDues = debtsDues,
                        budgetLimit = budgetLimit,
                        activeHistorySection = activeHistorySection,
                        onSectionChange = { activeHistorySection = it },
                        onDeleteExpense = { viewModel.deleteExpense(it) },
                        onImportExpenses = { importedList, callback ->
                            viewModel.importExpenses(importedList, callback)
                        },
                        onRequestStoragePermission = { action ->
                            requestStoragePermission(action)
                        },
                        onSettleDebtDue = { item, logAsExp ->
                            viewModel.settleDebtDue(item, logAsExp)
                        },
                        onDeleteDebtDue = { id ->
                            viewModel.deleteDebtDue(id)
                        },
                        activeSubTab = insightsSubTab,
                        onSubTabChange = { insightsSubTab = it }
                    )
                }
                "food_diary" -> {
                    FoodDiaryTab(
                        expenses = expenses,
                        viewModel = viewModel
                    )
                }
                "profile" -> {
                    val isDarkMode by viewModel.isDarkMode.collectAsState()
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
                        isDarkMode = isDarkMode,
                        themeSelection = themeSelection,
                        activity = activity
                    )
                }
            }
        }


        androidx.compose.animation.AnimatedVisibility(
            visible = currentTab != "home" || selectedGroupId == null,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { it },
                animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { it },
                animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(32.dp),
                                spotColor = Color.Black.copy(alpha = 0.15f)
                            )
                            .background(DarkCardSurface, RoundedCornerShape(32.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tabs = listOf(
                                Triple("home", Icons.Rounded.Home, "Home"),
                                Triple("insights", Icons.Rounded.BarChart, "Insights"),
                                Triple("food_diary", Icons.Rounded.Fastfood, "Food Diary"),
                                Triple("profile", Icons.Rounded.Person, "Profile")
                            )

                            tabs.forEach { (tabId, icon, label) ->
                                val isSelected = currentTab == tabId
                                

                                val itemInteractionSource = remember { MutableInteractionSource() }
                                val itemPressed by itemInteractionSource.collectIsPressedAsState()
                                val itemScale by animateFloatAsState(if (itemPressed) 0.92f else 1f, label = "tabItemScale")

                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = itemScale
                                            scaleY = itemScale
                                        }
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(
                                            if (isSelected) PrimaryAccent else Color.Transparent
                                        )
                                        .clickable(
                                            interactionSource = itemInteractionSource,
                                            indication = LocalIndication.current
                                        ) { currentTab = tabId }
                                        .padding(horizontal = if (isSelected) 10.dp else 8.dp, vertical = 8.dp)
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
                                            tint = if (isSelected) DarkCardTextPrimary else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        if (isSelected) {
                                            Text(
                                                text = label,
                                                color = DarkCardTextPrimary,
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


                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = fabScale
                                scaleY = fabScale
                            }
                            .size(56.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                spotColor = Color.Black.copy(alpha = 0.2f)
                            )
                            .background(PrimaryAccent, CircleShape)
                            .clickable(
                                interactionSource = fabInteractionSource,
                                indication = LocalIndication.current
                            ) {
                                if (currentTab == "home" && homeMode == "group") {
                                    if (selectedGroupId == null) {
                                        groupViewModel.setShowCreateGroupDialog(true)
                                    } else {
                                        groupViewModel.setShowAddExpenseDialog(true)
                                    }
                                } else {
                                    addDialogPrefillCategory = ""
                                    showAddEntryDialog = true
                                }
                            }
                            .border(1.dp, PrimaryAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Transaction",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }


        if (showNotificationsDialog) {
            val primaryAccentVal = PrimaryAccent
            val textPrimaryVal = TextPrimary
            val textSecondaryVal = TextSecondary
            Dialog(onDismissRequest = { showNotificationsDialog = false }) {
                val notificationSections = remember(expenses, budgetLimit, totalAmount, primaryAccentVal, textPrimaryVal, textSecondaryVal) {
                    val sections = mutableListOf<Pair<String, List<NotificationItem>>>()


                    val budgetAlerts = mutableListOf<NotificationItem>()
                    val percentUsed = if (budgetLimit > 0) (totalAmount / budgetLimit * 100).toInt() else 0

                    if (totalAmount > budgetLimit) {
                        budgetAlerts.add(NotificationItem(
                            title = "Budget Exceeded",
                            text = "You've overspent by ৳${String.format(Locale.US, "%,.0f", totalAmount - budgetLimit)}. Review your recent expenses.",
                            icon = Icons.Rounded.Error,
                            color = primaryAccentVal,
                            severity = 2
                        ))
                    } else if (percentUsed >= 80) {
                        budgetAlerts.add(NotificationItem(
                            title = "High Spending Alert",
                            text = "$percentUsed% of budget used (৳${String.format(Locale.US, "%,.0f", budgetLimit - totalAmount)} remaining).",
                            icon = Icons.Rounded.Warning,
                            color = primaryAccentVal,
                            severity = 1
                        ))
                    } else if (expenses.isNotEmpty()) {
                        budgetAlerts.add(NotificationItem(
                            title = "Budget On Track",
                            text = "$percentUsed% used — ৳${String.format(Locale.US, "%,.0f", budgetLimit - totalAmount)} remaining this month.",
                            icon = Icons.Rounded.CheckCircle,
                            color = textPrimaryVal,
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
                            val topPct = if (totalAmount > 0) (topAmt / totalAmount * 100).toInt() else 0
                            analysis.add(NotificationItem(
                                title = "Top Category: ${it.key}",
                                text = "৳${String.format(Locale.US, "%,.0f", topAmt)} spent ($topPct% of total). ${if (topPct > 50) "Consider diversifying your spending." else ""}",
                                icon = Icons.Rounded.BarChart,
                                color = primaryAccentVal,
                                severity = if (topPct > 50) 1 else 0
                            ))
                        }


                        val avgPerTxn = totalAmount / expenses.size
                        analysis.add(NotificationItem(
                            title = "Avg. Transaction: ৳${String.format(Locale.US, "%,.0f", avgPerTxn)}",
                            text = "Across ${expenses.size} total transactions.",
                            icon = Icons.Rounded.Calculate,
                            color = textPrimaryVal,
                            severity = 0
                        ))


                        val catCount = categoryGroups.size
                        analysis.add(NotificationItem(
                            title = "$catCount Active ${if (catCount == 1) "Category" else "Categories"}",
                            text = categoryGroups.keys.joinToString(", "),
                            icon = Icons.Rounded.Category,
                            color = textSecondaryVal,
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
                                color = textPrimaryVal,
                                severity = 0
                            ))
                        } else {
                            trends.add(NotificationItem(
                                title = "No Spending Today",
                                text = "Great discipline! Keep it going.",
                                icon = Icons.Rounded.Savings,
                                color = textSecondaryVal,
                                severity = 0
                            ))
                        }


                        val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                        val monthStart = (cal.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val thisMonthSpent = expenses.filter { it.date >= monthStart }.sumOf { it.amount }
                        val dailyRate = if (currentDay > 0) thisMonthSpent / currentDay else 0.0
                        val projected = dailyRate * totalDaysInMonth
                        val predictedSavings = budgetLimit - projected

                        if (predictedSavings >= 0) {
                            trends.add(NotificationItem(
                                title = "Savings Forecast: +৳${String.format(Locale.US, "%,.0f", predictedSavings)}",
                                text = "At current pace, you'll save ৳${String.format(Locale.US, "%,.0f", predictedSavings)} this month.",
                                icon = Icons.Rounded.TrendingDown,
                                color = textPrimaryVal,
                                severity = 0
                            ))
                        } else {
                            trends.add(NotificationItem(
                                title = "Over-Budget Warning",
                                text = "Projected to exceed budget by ৳${String.format(Locale.US, "%,.0f", -predictedSavings)}. Reduce daily spending.",
                                icon = Icons.Rounded.TrendingUp,
                                color = primaryAccentVal,
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
                            color = textSecondaryVal,
                            severity = 0
                        ))


                        val biggest = expenses.maxByOrNull { it.amount }
                        biggest?.let {
                            insights.add(NotificationItem(
                                title = "Biggest Expense: ৳${String.format(Locale.US, "%,.0f", it.amount)}",
                                text = "${it.description.ifEmpty { it.category }} in ${it.category}.",
                                icon = Icons.Rounded.Lightbulb,
                                color = textPrimaryVal,
                                severity = 0
                            ))
                        }
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
                                items(items) { item ->
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
    totalAmount: Double,
    budgetLimit: Double,
    expenses: List<Expense>,
    hasUnreadNotifications: Boolean,
    onAddExpenseClick: (category: String) -> Unit,
    onNavigate: (tab: String) -> Unit,
    onShowNotifications: () -> Unit,
    showWelcomeHeader: Boolean = true
) {
    val context = LocalContext.current
    val debtsDues by viewModel.debtsDues.collectAsState()
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val displayExpenses = remember(expenses, selectedFilter) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val weekStart = Calendar.getInstance().apply {
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

        val filtered = when (selectedFilter) {
            "Today" -> expenses.filter { it.date >= todayStart }
            "This Week" -> expenses.filter { it.date >= weekStart }
            "This Month" -> expenses.filter { it.date >= monthStart }
            else -> expenses
        }
        filtered.take(4)
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

    val bottomPadding = 112.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = bottomPadding, top = 24.dp)
    ) {
        if (showWelcomeHeader) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
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
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        Box(
                            modifier = Modifier
                                .size(44.dp)
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
                                    modifier = Modifier.size(20.dp)
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
                                    .size(44.dp)
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
                                        .size(22.dp)
                                        .graphicsLayer { rotationZ = bellRotation }
                                )
                            }


                            if (hasUnreadNotifications) {

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-1).dp, y = 1.dp)
                                        .size(10.dp)
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
                                        .size(10.dp)
                                        .border(1.5.dp, ThemeBackground, CircleShape)
                                        .background(PrimaryAccent, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }


        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                val remainingBudget = (budgetLimit - totalAmount).coerceAtLeast(0.0)
                val remainingText = String.format(Locale.US, "%,.2f", remainingBudget)
                Text(
                    text = "REMAINING BALANCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontSize = 10.sp
                    ),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "৳$remainingText",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = (-1).sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                val spentText = String.format(Locale.US, "%,.2f", totalAmount)
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                            append("৳$spentText")
                        }
                        append(" spent of monthly target")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }


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


                    val cal = Calendar.getInstance()
                    val home7DaysData = remember(expenses) {
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
                                .filter { it.date in dayStart.timeInMillis..dayEnd.timeInMillis }
                                .sumOf { it.amount }
                            result.add(label to sum)
                        }
                        result
                    }

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

                        val lastSpendText = if (expenses.isNotEmpty()) {
                            val last = expenses.first()
                            "Latest: ৳${String.format(Locale.US, "%.0f", last.amount)} spent on ${last.category}"
                        } else {
                            "Goal target: ৳${String.format(Locale.US, "%.0f", budgetLimit)} set"
                        }

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

            val pendingDebts = remember(debtsDues) {
                debtsDues.filter { !it.isCleared && it.type == "DEBT" }.sumOf { it.amount }
            }
            val pendingDues = remember(debtsDues) {
                debtsDues.filter { !it.isCleared && it.type == "DUE" }.sumOf { it.amount }
            }

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

                val hubCategories = listOf("Transport", "Mobile", "Others")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    hubCategories.forEach { catName ->
                        val style = getCategoryStyle(catName)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            modifier = Modifier
                                .weight(1f)
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
                                    fontSize = 12.sp,
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
    budgetLimit: Double
) {
    var selectedStatsTab by remember { mutableStateOf("spending") }
    var isWeekSelected by remember { mutableStateOf(true) }

    val last4WeeksData = remember(expenses) {
        val cal = Calendar.getInstance()
        val result = mutableListOf<Pair<String, Double>>()
        for (w in 3 downTo 0) {
            val weekStart = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_YEAR, -(w * 7 + 6))
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val weekEnd = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_YEAR, -(w * 7))
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }
            val label = "W${4 - w}"
            val sum = expenses
                .filter { it.date in weekStart.timeInMillis..weekEnd.timeInMillis }
                .sumOf { it.amount }
            result.add(label to sum)
        }
        result
    }

    val totalSpent = remember(expenses) { expenses.sumOf { it.amount } }

    val categoryData = remember(expenses) {
        expenses.groupBy { it.category }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
    }

    val topCategory = remember(categoryData) { categoryData.firstOrNull() }


    val daysSinceFirst = remember(expenses) {
        if (expenses.isEmpty()) 1
        else {
            val earliest = expenses.minOf { it.date }
            val diff = System.currentTimeMillis() - earliest
            maxOf(1, (diff / (1000L * 60 * 60 * 24)).toInt())
        }
    }
    val dailyAvg = remember(totalSpent, daysSinceFirst) { totalSpent / daysSinceFirst }


    val last7DaysData = remember(expenses) {
        val cal = Calendar.getInstance()
        val today = cal.clone() as Calendar
        today.set(Calendar.HOUR_OF_DAY, 23); today.set(Calendar.MINUTE, 59); today.set(Calendar.SECOND, 59)

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
                .filter { it.date in dayStart.timeInMillis..dayEnd.timeInMillis }
                .sumOf { it.amount }
            result.add(label to sum)
        }
        result
    }

    val chartData = remember(isWeekSelected, last7DaysData, last4WeeksData) {
        if (isWeekSelected) last7DaysData else last4WeeksData
    }


    val monthlyProjection = remember(expenses) {
        val cal = Calendar.getInstance()
        val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

        val monthStart = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val thisMonthSpent = expenses.filter { it.date >= monthStart }.sumOf { it.amount }
        val dailyRate = if (currentDayOfMonth > 0) thisMonthSpent / currentDayOfMonth else 0.0
        val projected = dailyRate * totalDaysInMonth
        val predictedSavings = budgetLimit - projected

        Triple(thisMonthSpent, projected, predictedSavings)
    }


    val biggestExpense = remember(expenses) { expenses.maxByOrNull { it.amount } }


    val mostActiveDay = remember(expenses) {
        if (expenses.isEmpty()) null
        else {
            val dayFormat = SimpleDateFormat("EEEE", Locale.US)
            expenses.groupBy { dayFormat.format(Date(it.date)) }
                .maxByOrNull { it.value.size }
                ?.let { (day, list) -> day to list.size }
        }
    }


    val avgPerTransaction = remember(expenses) {
        if (expenses.isEmpty()) 0.0 else totalSpent / expenses.size
    }


    val chartColors = listOf(
        Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFEC4899),
        Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF06B6D4),
        Color(0xFFEF4444)
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
                    val (thisMonthSpent, projected, predictedSavings) = monthlyProjection
                    val isOnTrack = predictedSavings >= 0

                    val cal = java.util.Calendar.getInstance()
                    val currentDayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)
                    val dailyRate = if (currentDayOfMonth > 0) thisMonthSpent / currentDayOfMonth else 0.0


                    val totalScale = maxOf(budgetLimit, projected)
                    val actualFraction = if (totalScale > 0) (thisMonthSpent / totalScale).toFloat().coerceIn(0f, 1f) else 0f
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
                                    Text(
                                        text = "Monthly Spending Forecast", 
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Est. month-end spend based on your daily speed",
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
                                        text = "৳${String.format(Locale.US, "%,.0f", thisMonthSpent)}",
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
                                    Text(
                                        text = "Monthly Budget Limit: ৳${String.format(Locale.US, "%,.0f", budgetLimit)}",
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
                                        text = "Est. Month End",
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


@Composable
fun HistoryTab(
    expenses: List<Expense>,
    debtsDues: List<DebtDue>,
    activeHistorySection: String,
    onSectionChange: (String) -> Unit,
    onDeleteExpense: (Int) -> Unit,
    onImportExpenses: (List<Expense>, (Boolean, Int) -> Unit) -> Unit,
    onRequestStoragePermission: (() -> Unit) -> Unit,
    onSettleDebtDue: (DebtDue, Boolean) -> Unit,
    onDeleteDebtDue: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showImportGuide by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showExportFormatDialog by remember { mutableStateOf(false) }

    var activeSection by remember(activeHistorySection) { mutableStateOf(activeHistorySection) }

    val handleSectionChange = { sec: String ->
        activeSection = sec
        onSectionChange(sec)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isImporting = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val importedList = ExcelHelper.importExpenses(inputStream)
                        withContext(Dispatchers.Main) {
                            onImportExpenses(importedList) { success, count ->
                                isImporting = false
                                if (success) {
                                    Toast.makeText(context, "Successfully imported $count records!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to save imported expenses.", Toast.LENGTH_LONG).show()
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
                        ExcelHelper.exportAllData(outputStream, expenses, debtsDues)
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

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("date_desc") }
    var dateFilter by remember { mutableStateOf("all") }
    var amountFilter by remember { mutableStateOf("all") }

    val categories = remember(expenses) {
        listOf("All") + expenses.map { it.category }.distinct()
    }

    val filteredExpenses = remember(searchQuery, selectedCategory, dateFilter, amountFilter, sortBy, expenses) {

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

        expenses.filter { expense ->
            val matchesSearch = expense.description.contains(searchQuery, ignoreCase = true) ||
                    expense.category.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || expense.category == selectedCategory
            val matchesDate = when (dateFilter) {
                "today" -> expense.date >= todayStart
                "week" -> expense.date >= weekStart
                "month" -> expense.date >= monthStart
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
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ledger & Logs",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary
            )
            
            IconButton(
                onClick = { showExportFormatDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Rounded.ImportExport,
                    contentDescription = "Export/Import Options",
                    tint = PrimaryAccent,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Sub-Navigation Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .background(CardSurface, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val sections = listOf(
                "transactions" to "Transactions",
                "debts" to "Debts (I Owe)",
                "dues" to "Receivables"
            )
            sections.forEach { (secKey, secLabel) ->
                val isSelected = activeSection == secKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) PrimaryAccent else Color.Transparent
                        )
                        .clickable { handleSectionChange(secKey) }
                        .padding(vertical = 10.dp),
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search logs...") },
                leadingIcon = { Icon(Icons.Rounded.Search, "Search", tint = TextSecondary) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                var sortMenuExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, CardSurface)
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
                                Icon(
                                    imageVector = Icons.Rounded.Sort,
                                    contentDescription = "Sort By",
                                    modifier = Modifier.size(16.dp),
                                    tint = PrimaryAccent
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.size(16.dp),
                                tint = TextPrimary
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(CardSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newest First", color = TextPrimary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                sortBy = "date_desc"
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest First", color = TextPrimary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                sortBy = "date_asc"
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Amount: High to Low", color = TextPrimary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                sortBy = "amount_desc"
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Amount: Low to High", color = TextPrimary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                sortBy = "amount_asc"
                                sortMenuExpanded = false
                            }
                        )
                    }
                }


                var dateMenuExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { dateMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, CardSurface)
                    ) {
                        val label = when (dateFilter) {
                            "today" -> "Today"
                            "week" -> "This Week"
                            "month" -> "This Month"
                            else -> "All Time"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.DateRange,
                                    contentDescription = "Date Range",
                                    modifier = Modifier.size(16.dp),
                                    tint = PrimaryAccent
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.size(16.dp),
                                tint = TextPrimary
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = dateMenuExpanded,
                        onDismissRequest = { dateMenuExpanded = false },
                        modifier = Modifier.background(CardSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Time", color = TextPrimary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                dateFilter = "all"
                                dateMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Today", color = TextPrimary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                dateFilter = "today"
                                dateMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("This Week", color = TextPrimary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                dateFilter = "week"
                                dateMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("This Month", color = TextPrimary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                dateFilter = "month"
                                dateMenuExpanded = false
                            }
                        )
                    }
                }


                var amountMenuExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { amountMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, CardSurface)
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
                                Icon(
                                    imageVector = Icons.Rounded.Payments,
                                    contentDescription = "Amount Limit",
                                    modifier = Modifier.size(16.dp),
                                    tint = PrimaryAccent
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.size(16.dp),
                                tint = TextPrimary
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = amountMenuExpanded,
                        onDismissRequest = { amountMenuExpanded = false },
                        modifier = Modifier.background(CardSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Prices", color = TextPrimary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                amountFilter = "all"
                                amountMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Under ৳500", color = TextPrimary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                amountFilter = "low"
                                amountMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("৳500 - ৳2,000", color = TextPrimary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                amountFilter = "medium"
                                amountMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Over ৳2,000", color = TextPrimary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                amountFilter = "high"
                                amountMenuExpanded = false
                            }
                        )
                    }
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) PrimaryAccent else CardSurface,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else TextPrimary,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            val bottomPadding = 112.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = bottomPadding)
            ) {
                if (filteredExpenses.isEmpty()) {
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
                    items(filteredExpenses, key = { it.id }) { expense ->
                        ExpenseItem(
                            expense = expense,
                            onDelete = { expenseToDelete = expense }
                        )
                    }
                }
            }
        } else if (activeSection == "debts") {
            DebtsSection(
                type = "DEBT",
                debtsDues = debtsDues,
                onSettleDebtDue = onSettleDebtDue,
                onDeleteDebtDue = onDeleteDebtDue
            )
        } else {
            DebtsSection(
                type = "DUE",
                debtsDues = debtsDues,
                onSettleDebtDue = onSettleDebtDue,
                onDeleteDebtDue = onDeleteDebtDue
            )
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

    if (showExportFormatDialog) {
        AlertDialog(
            onDismissRequest = { showExportFormatDialog = false },
            title = { Text("Export & Import", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
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
                        Text("Export PDF Document (.pdf)", fontWeight = FontWeight.Bold)
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
                        Text("Export Excel Spreadsheet (.xls)", fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CardSurface)
                    )

                    Button(
                        onClick = {
                            showExportFormatDialog = false
                            showImportGuide = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurface, contentColor = TextPrimary)
                    ) {
                        Text("Import Logs from Excel", fontWeight = FontWeight.Bold)
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

                Text(
                    text = "To import your transactions, your Excel (.xls or .xlsx) file must be structured as follows:",
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
    isDarkMode: Boolean,
    themeSelection: String,
    activity: FragmentActivity
) {
    val context = LocalContext.current
    var inputBudget by remember { mutableStateOf(budgetLimit.toString()) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember(userName) { mutableStateOf(userName) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

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
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
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
                        text = "Monthly Savings Target",
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
                        text = "App Security",
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
                }
            }
        }

        // Dark Mode Toggle
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Appearance",
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
                            Icon(Icons.Rounded.DarkMode, "Dark Mode", tint = PrimaryAccent, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Dark Mode",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isDarkMode) "Dark theme is active" else "Light theme is active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.updateDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryAccent,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = ThemeBackground,
                                uncheckedBorderColor = CardSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = ThemeBackground)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Row 1: Classic & Ocean Blue
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isClassicSelected = themeSelection == "Classic"
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isClassicSelected) PrimaryAccent.copy(alpha = 0.1f) else ThemeBackground
                                ),
                                border = if (isClassicSelected) BorderStroke(2.dp, PrimaryAccent) else BorderStroke(1.dp, CardSurface),
                                modifier = Modifier.weight(1f).clickable { viewModel.updateTheme("Classic") }
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFFB51A28), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFF161E2F), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFFFFF5F2), CircleShape).border(1.dp, Color(0xFFFFE0D5), CircleShape))
                                    }
                                    Text(text = "Classic", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (isClassicSelected) PrimaryAccent else TextPrimary)
                                    Text(text = "Red accent", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }

                            val isBlueSelected = themeSelection == "Ocean Blue" || themeSelection == "Blue/Black"
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isBlueSelected) Color(0xFF5483B3).copy(alpha = 0.1f) else ThemeBackground
                                ),
                                border = if (isBlueSelected) BorderStroke(2.dp, Color(0xFF5483B3)) else BorderStroke(1.dp, CardSurface),
                                modifier = Modifier.weight(1f).clickable { viewModel.updateTheme("Ocean Blue") }
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFF5483B3), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFF021024), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFFC1E8FF), CircleShape).border(1.dp, Color(0xFFE6F4FE), CircleShape))
                                    }
                                    Text(text = "Ocean Blue", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (isBlueSelected) Color(0xFF5483B3) else TextPrimary)
                                    Text(text = "Deep blue accent", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }
                        }

                        // Row 2: Green Tea & Sunset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isGreenSelected = themeSelection == "Green Tea" || themeSelection == "Green" || themeSelection == "Light Green"
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isGreenSelected) Color(0xFF688E4E).copy(alpha = 0.1f) else ThemeBackground
                                ),
                                border = if (isGreenSelected) BorderStroke(2.dp, Color(0xFF688E4E)) else BorderStroke(1.dp, CardSurface),
                                modifier = Modifier.weight(1f).clickable { viewModel.updateTheme("Green Tea") }
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFF688E4E), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFF1B2727), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFFF4F7F4), CircleShape).border(1.dp, Color(0xFFD5DDDF), CircleShape))
                                    }
                                    Text(text = "Green Tea", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (isGreenSelected) Color(0xFF688E4E) else TextPrimary)
                                    Text(text = "Nature green accent", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }

                            val isSunsetSelected = themeSelection == "Sunset" || themeSelection == "Light Yellow"
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSunsetSelected) Color(0xFFA74D65).copy(alpha = 0.1f) else ThemeBackground
                                ),
                                border = if (isSunsetSelected) BorderStroke(2.dp, Color(0xFFA74D65)) else BorderStroke(1.dp, CardSurface),
                                modifier = Modifier.weight(1f).clickable { viewModel.updateTheme("Sunset") }
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFFDF7862), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFF1D1A39), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFFFDB45C), CircleShape).border(1.dp, Color(0xFFFFE0CC), CircleShape))
                                    }
                                    Text(text = "Sunset", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (isSunsetSelected) Color(0xFFA74D65) else TextPrimary)
                                    Text(text = "Warm sunset accent", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }
                        }

                        // Row 3: Grapefruit & Bubblegum
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isGrapefruitSelected = themeSelection == "Grapefruit" || themeSelection == "Purple" || themeSelection == "Light Blue"
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isGrapefruitSelected) Color(0xFFA56ABD).copy(alpha = 0.1f) else ThemeBackground
                                ),
                                border = if (isGrapefruitSelected) BorderStroke(2.dp, Color(0xFFA56ABD)) else BorderStroke(1.dp, CardSurface),
                                modifier = Modifier.weight(1f).clickable { viewModel.updateTheme("Grapefruit") }
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFFA56ABD), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFF49225B), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFFFDFBFE), CircleShape).border(1.dp, Color(0xFFE7DBEF), CircleShape))
                                    }
                                    Text(text = "Grapefruit", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (isGrapefruitSelected) Color(0xFFA56ABD) else TextPrimary)
                                    Text(text = "Purple accent", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }

                            val isBubblegumSelected = themeSelection == "Bubblegum" || themeSelection == "Pink"
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isBubblegumSelected) Color(0xFFBA96C1).copy(alpha = 0.1f) else ThemeBackground
                                ),
                                border = if (isBubblegumSelected) BorderStroke(2.dp, Color(0xFFBA96C1)) else BorderStroke(1.dp, CardSurface),
                                modifier = Modifier.weight(1f).clickable { viewModel.updateTheme("Bubblegum") }
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFFBA96C1), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFF4B3F6E), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFFF7F5F8), CircleShape).border(1.dp, Color(0xFFDCD7D5), CircleShape))
                                    }
                                    Text(text = "Bubblegum", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (isBubblegumSelected) Color(0xFFBA96C1) else TextPrimary)
                                    Text(text = "Pink accent", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Copyright,
                    contentDescription = "Copyright",
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Copyright Azwad Abrar. All rights reserved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
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
}


@Composable
fun ExpenseItem(expense: Expense, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.US) }
    val dateStr = remember(expense.date) { dateFormat.format(Date(expense.date)) }
    val style = getCategoryStyle(expense.category)

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
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ThemeBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = style.first,
                        contentDescription = expense.category,
                        tint = style.third,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.description.ifEmpty { expense.category },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$dateStr • ${expense.category}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                
                Text(
                    text = "৳${String.format(Locale.US, "%,.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).testTag("delete_expense_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = PrimaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
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
        "other", "others" -> Icons.Rounded.AddCircleOutline
        "travel", "transport", "car" -> Icons.Rounded.DirectionsCar
        "mobile", "phone", "telecom" -> Icons.Rounded.PhoneAndroid
        "entertainment", "games", "movies" -> Icons.Rounded.SportsEsports
        "medical", "health", "doctor" -> Icons.Rounded.MedicalServices
        "education", "school", "books" -> Icons.Rounded.School
        "salary", "income" -> Icons.Rounded.Payments
        "debt repayment" -> Icons.Rounded.Handshake
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
    val tint = tints[Math.abs(category.hashCode()) % tints.size]
    

    return Triple(icon, CardSurface, tint)
}


data class NotificationItem(
    val title: String,
    val text: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val severity: Int = 0
)


@Composable
private fun NotificationCard(item: NotificationItem) {
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
                    tint = item.color,
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
fun InsightsTab(
    expenses: List<Expense>,
    debtsDues: List<DebtDue>,
    budgetLimit: Double,
    activeHistorySection: String,
    onSectionChange: (String) -> Unit,
    onDeleteExpense: (Int) -> Unit,
    onImportExpenses: (List<Expense>, (Boolean, Int) -> Unit) -> Unit,
    onRequestStoragePermission: (() -> Unit) -> Unit,
    onSettleDebtDue: (DebtDue, Boolean) -> Unit,
    onDeleteDebtDue: (Int) -> Unit,
    activeSubTab: String,
    onSubTabChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(48.dp)
                .background(CardSurface, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val subTabs = listOf("ledger" to "Ledger", "analytics" to "Analytics")
            subTabs.forEach { (subTabKey, subTabLabel) ->
                val isSelected = activeSubTab == subTabKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) PrimaryAccent else Color.Transparent
                        )
                        .clickable { onSubTabChange(subTabKey) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subTabLabel,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
        
        Box(modifier = Modifier.weight(1f)) {
            if (activeSubTab == "ledger") {
                HistoryTab(
                    expenses = expenses,
                    debtsDues = debtsDues,
                    activeHistorySection = activeHistorySection,
                    onSectionChange = onSectionChange,
                    onDeleteExpense = onDeleteExpense,
                    onImportExpenses = onImportExpenses,
                    onRequestStoragePermission = onRequestStoragePermission,
                    onSettleDebtDue = onSettleDebtDue,
                    onDeleteDebtDue = onDeleteDebtDue
                )
            } else {
                AnalyticsTab(
                    expenses = expenses,
                    debtsDues = debtsDues,
                    budgetLimit = budgetLimit
                )
            }
        }
    }
}

@Composable
fun FoodDiaryTab(
    expenses: List<Expense>,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    
    val currentMonthFoodEntries = remember(expenses) {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)
        
        expenses.filter { exp ->
            if (exp.category.trim().lowercase(Locale.US) == "food") {
                val entryCal = Calendar.getInstance()
                entryCal.timeInMillis = exp.date
                entryCal.get(Calendar.YEAR) == currentYear && entryCal.get(Calendar.MONTH) == currentMonth
            } else {
                false
            }
        }.sortedByDescending { it.date }
    }

    val monthName = remember {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Food Diary",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "Your culinary log for $monthName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            if (currentMonthFoodEntries.isNotEmpty()) {
                FilledTonalButton(
                    onClick = { exportFoodDiaryToPdf(context, currentMonthFoodEntries) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PrimaryAccent.copy(alpha = 0.12f),
                        contentColor = PrimaryAccent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = "Export PDF",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (currentMonthFoodEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(CardSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fastfood,
                            contentDescription = null,
                            tint = PrimaryAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your Food Diary is Empty",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Log transactions with category 'Food' and add photo details in the Add transaction menu to build your diary!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(currentMonthFoodEntries) { item ->
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
                            val bitmap = remember(item.imageBytes) {
                                if (item.imageBytes != null) {
                                    try {
                                        BitmapFactory.decodeByteArray(item.imageBytes, 0, item.imageBytes.size)
                                    } catch (e: Exception) {
                                        null
                                    }
                                } else {
                                    null
                                }
                            }
                            
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Food Pic",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, CardSurface, RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(ThemeBackground, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fastfood,
                                        contentDescription = null,
                                        tint = TextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.foodDetails ?: "Delicious Meal 🍲",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val dateFormatted = remember(item.date) {
                                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(item.date))
                                }
                                Text(
                                    text = dateFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary.copy(alpha = 0.8f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = String.format(Locale.US, "৳%,.2f", item.amount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun exportFoodDiaryToPdf(context: Context, entries: List<Expense>) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    
    var pageNumber = 1
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
    }
    
    // Helper function for hand-drawn look hearts
    fun drawDoodleHeart(canvas: android.graphics.Canvas, x: Float, y: Float, size: Float, colorStr: String) {
        val heartPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor(colorStr)
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        val path = android.graphics.Path()
        path.moveTo(x, y + size * 0.25f)
        path.cubicTo(x - size * 0.5f, y - size * 0.5f, x - size, y + size * 0.3f, x, y + size)
        path.cubicTo(x + size, y + size * 0.3f, x + size * 0.5f, y - size * 0.5f, x, y + size * 0.25f)
        path.close()
        canvas.drawPath(path, heartPaint)
    }

    // Helper function for hand-drawn star sparkles
    fun drawDoodleSparkle(canvas: android.graphics.Canvas, x: Float, y: Float, size: Float, colorStr: String) {
        val sparklePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor(colorStr)
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        val path = android.graphics.Path()
        path.moveTo(x, y - size)
        path.quadTo(x, y, x + size, y)
        path.quadTo(x, y, x, y + size)
        path.quadTo(x, y, x - size, y)
        path.quadTo(x, y, x, y - size)
        path.close()
        canvas.drawPath(path, sparklePaint)
    }

    // Helper function to draw washi tape at a slight angle
    fun drawWashiTape(canvas: android.graphics.Canvas, cx: Float, cy: Float, width: Float, height: Float, colorStr: String, angle: Float) {
        val tapePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor(colorStr)
            isAntiAlias = true
        }
        canvas.save()
        canvas.rotate(angle, cx, cy)
        canvas.drawRect(cx - width / 2, cy - height / 2, cx + width / 2, cy + height / 2, tapePaint)
        
        // Draw jagged edges at the ends of washi tape
        val edgePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FAF3E8") // same as page background to cut out
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Left edge jagged cuts
        val leftX = cx - width / 2
        var curY = cy - height / 2
        while (curY < cy + height / 2) {
            val path = android.graphics.Path()
            path.moveTo(leftX, curY)
            path.lineTo(leftX + 3f, curY + 2f)
            path.lineTo(leftX, curY + 4f)
            path.close()
            canvas.drawPath(path, edgePaint)
            curY += 4f
        }
        
        // Right edge jagged cuts
        val rightX = cx + width / 2
        curY = cy - height / 2
        while (curY < cy + height / 2) {
            val path = android.graphics.Path()
            path.moveTo(rightX, curY)
            path.lineTo(rightX - 3f, curY + 2f)
            path.lineTo(rightX, curY + 4f)
            path.close()
            canvas.drawPath(path, edgePaint)
            curY += 4f
        }
        
        canvas.restore()
    }
    
    // Draw page backgrounds and borders
    fun initPage(canvas: android.graphics.Canvas, isFirst: Boolean, pNum: Int) {
        canvas.drawColor(android.graphics.Color.parseColor("#FAF3E8")) // Warm cream scrapbook base
        
        // Draw simple hand-drawn sketch border around page
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#E0D2C0")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRect(15f, 15f, 580f, 827f, borderPaint)
        
        val innerBorderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#EADCC9")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRect(20f, 20f, 575f, 822f, innerBorderPaint)
        
        if (isFirst) {
            // Main title "My Food Diary" in elegant bold italic serif
            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#4A3B32")
                textSize = 34f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD_ITALIC)
                isAntiAlias = true
            }
            canvas.drawText("My Food Diary", 50f, 65f, titlePaint)
            
            // Subtitle
            val subtitlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#8C7A6B")
                textSize = 13f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC)
                isAntiAlias = true
            }
            val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
            canvas.drawText("☕ Cozy Café Memories | Generated for $monthName", 50f, 90f, subtitlePaint)
            
            // Curved decorative line under header
            val linePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#C2B29F")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            val path = android.graphics.Path()
            path.moveTo(50f, 105f)
            path.quadTo(300f, 112f, 545f, 105f)
            canvas.drawPath(path, linePaint)
            
            // Draw a cute decorative heart on the title section
            drawDoodleHeart(canvas, 520f, 55f, 12f, "#DF7862")
            drawDoodleSparkle(canvas, 480f, 75f, 8f, "#FDB45C")
        } else {
            val subtitlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#8C7A6B")
                textSize = 12f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC)
                isAntiAlias = true
            }
            canvas.drawText("Food Diary — Page $pNum", 50f, 40f, subtitlePaint)
            
            val linePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#C2B29F")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1.5f
                isAntiAlias = true
            }
            val path = android.graphics.Path()
            path.moveTo(50f, 50f)
            path.quadTo(300f, 54f, 545f, 50f)
            canvas.drawPath(path, linePaint)
        }
    }
    
    initPage(canvas, true, 1)
    var currentY = 135f
    
    entries.forEachIndexed { index, entry ->
        if (currentY + 225f > 800f) {
            pdfDocument.finishPage(page)
            pageNumber++
            val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdfDocument.startPage(newPageInfo)
            canvas = page.canvas
            
            initPage(canvas, false, pageNumber)
            currentY = 80f
        }
        
        // Draw the scrapbook background card for the entry
        val cardPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRoundRect(50f, currentY, 545f, currentY + 195f, 18f, 18f, cardPaint)
        
        val cardOutlinePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#E6D8C8")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(50f, currentY, 545f, currentY + 195f, 18f, 18f, cardOutlinePaint)
        
        // Draw washi tape sticker at the top center of the card
        val tapeColor = if (index % 2 == 0) "#D4C5B9" else "#C2B29F"
        drawWashiTape(canvas, 297.5f, currentY, 70f, 15f, tapeColor, if (index % 2 == 0) -3f else 4f)
        
        var imgWidth = 0
        if (entry.imageBytes != null) {
            val opt = BitmapFactory.Options().apply {
                inSampleSize = 1
            }
            val bitmap = try {
                BitmapFactory.decodeByteArray(entry.imageBytes, 0, entry.imageBytes.size, opt)
            } catch (e: Exception) {
                null
            }
            if (bitmap != null) {
                // Polaroid layout: rounded photo container
                val polaroidFramePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#FAF8F5")
                    style = android.graphics.Paint.Style.FILL
                }
                val polaroidBorderPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#E0D2C0")
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 1.5f
                    isAntiAlias = true
                }
                
                canvas.drawRoundRect(65f, currentY + 18f, 205f, currentY + 175f, 12f, 12f, polaroidFramePaint)
                canvas.drawRoundRect(65f, currentY + 18f, 205f, currentY + 175f, 12f, 12f, polaroidBorderPaint)
                
                val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                val destRect = android.graphics.Rect(72, (currentY + 24f).toInt(), 198, (currentY + 145f).toInt())
                canvas.drawBitmap(bitmap, srcRect, destRect, paint)
                
                // Polaroid text label
                val capPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#706050")
                    textSize = 8.5f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC)
                    isAntiAlias = true
                }
                val dateLabel = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(entry.date))
                canvas.drawText("Logged $dateLabel", 82f, currentY + 163f, capPaint)
                
                imgWidth = 155
                bitmap.recycle()
            }
        }
        
        val textStartX = 65f + imgWidth + (if (imgWidth > 0) 15f else 10f)
        
        // Date Text
        val dateTextPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#8C7A6B")
            textSize = 10f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC)
            isAntiAlias = true
        }
        val dateString = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US).format(Date(entry.date))
        canvas.drawText(dateString, textStartX, currentY + 35f, dateTextPaint)
        
        // Food details label (Cursive italic look style like pesto pasta / garlic bread)
        val foodDetailsPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#3E2723") // Cozy dark brown
            textSize = 18f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD_ITALIC)
            isAntiAlias = true
        }
        val foodName = entry.foodDetails ?: "Delicious Dish 🍲"
        canvas.drawText(foodName, textStartX, currentY + 65f, foodDetailsPaint)
        
        // Notes text
        val notesPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#6D4C41")
            textSize = 12f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
            isAntiAlias = true
        }
        val note = if (entry.description.startsWith("Quick Food") || entry.description.startsWith("Imported")) {
            "Meal log entry"
        } else {
            entry.description
        }
        
        // Wrap notes text slightly to avoid overflowing bounds
        val maxNoteWidth = 525f - textStartX
        val noteText = "Notes: $note"
        if (notesPaint.measureText(noteText) > maxNoteWidth) {
            val words = noteText.split(" ")
            var line1 = ""
            var line2 = ""
            for (word in words) {
                if (notesPaint.measureText("$line1$word ") < maxNoteWidth) {
                    line1 += "$word "
                } else {
                    line2 += "$word "
                }
            }
            canvas.drawText(line1.trim(), textStartX, currentY + 95f, notesPaint)
            if (line2.isNotEmpty()) {
                canvas.drawText(line2.trim(), textStartX, currentY + 112f, notesPaint)
            }
        } else {
            canvas.drawText(noteText, textStartX, currentY + 95f, notesPaint)
        }
        
        // Price amount tag badge (a cute sticker tag)
        val amtBgPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FFF3E0") // Light orange/yellow soft tag
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        val amtBorderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FFE0B2")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        val amtPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#E65100") // Rich orange/brown text
            textSize = 16f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD_ITALIC)
            isAntiAlias = true
        }
        
        val formattedAmt = String.format(Locale.US, "৳%,.2f", entry.amount)
        val textWidth = amtPaint.measureText(formattedAmt)
        canvas.drawRoundRect(textStartX, currentY + 138f, textStartX + textWidth + 16f, currentY + 170f, 8f, 8f, amtBgPaint)
        canvas.drawRoundRect(textStartX, currentY + 138f, textStartX + textWidth + 16f, currentY + 170f, 8f, 8f, amtBorderPaint)
        canvas.drawText(formattedAmt, textStartX + 8f, currentY + 160f, amtPaint)
        
        // Add random doodle decorative elements to reinforce the scrapbook collage feel
        if (index % 3 == 0) {
            drawDoodleHeart(canvas, 515f, currentY + 155f, 10f, "#DF7862")
            drawDoodleSparkle(canvas, 500f, currentY + 30f, 6f, "#FDB45C")
        } else if (index % 3 == 1) {
            drawDoodleSparkle(canvas, 515f, currentY + 155f, 8f, "#FDB45C")
            drawDoodleHeart(canvas, 480f, currentY + 25f, 9f, "#A74D65")
        } else {
            drawDoodleHeart(canvas, 515f, currentY + 25f, 10f, "#DF7862")
            drawDoodleSparkle(canvas, 475f, currentY + 160f, 7f, "#FDB45C")
        }
        
        // Embellish the top corners or bottom corners with stars/dots
        val dotPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#D4C5B9")
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(58f, currentY + 185f, 2f, dotPaint)
        canvas.drawCircle(537f, currentY + 185f, 2f, dotPaint)
        canvas.drawCircle(58f, currentY + 10f, 2f, dotPaint)
        canvas.drawCircle(537f, currentY + 10f, 2f, dotPaint)
        
        currentY += 215f
    }
    
    pdfDocument.finishPage(page)
    
    val cacheDir = context.cacheDir
    val pdfFile = File(cacheDir, "Food_Diary_${System.currentTimeMillis()}.pdf")
    try {
        val fos = FileOutputStream(pdfFile)
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        fos.close()
        
        val authority = "com.example.provider"
        val pdfUri = androidx.core.content.FileProvider.getUriForFile(context, authority, pdfFile)
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, pdfUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "My Food Diary PDF")
            putExtra(android.content.Intent.EXTRA_TEXT, "Here is my food diary for the month!")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Food Diary PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun HomeHeaderRow(
    userName: String,
    profileImageBitmap: Bitmap?,
    hasUnreadNotifications: Boolean,
    onNavigate: (String) -> Unit,
    onShowNotifications: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
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
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
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
                        modifier = Modifier.size(20.dp)
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
                        .size(44.dp)
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
                            .size(22.dp)
                            .graphicsLayer { rotationZ = bellRotation }
                    )
                }

                if (hasUnreadNotifications) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-1).dp, y = 1.dp)
                            .size(10.dp)
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
                            .size(10.dp)
                            .border(1.5.dp, ThemeBackground, CircleShape)
                            .background(PrimaryAccent, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeModeSwitcher(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("personal" to "Personal Tracker", "group" to "Group Expenses").forEach { (mode, label) ->
            val isSelected = selectedMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) ThemeBackground else Color.Transparent)
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PrimaryAccent else TextSecondary
                )
            }
        }
    }
}
