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
    
    var currentTab by remember { mutableStateOf("home") }
    var showAddDialog by remember { mutableStateOf(false) }
    var addDialogPrefillCategory by remember { mutableStateOf("") }
    

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
            if (showAddDialog) {
                AddExpenseDialog(
                    initialCategory = addDialogPrefillCategory,
                    existingCategories = existingCategories,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { amt, desc, cat ->
                        viewModel.addExpense(amt, desc, cat)
                        showAddDialog = false
                    }
                )
            }

            when (currentTab) {
                "home" -> HomeTab(
                    viewModel = viewModel,
                    userName = userName,
                    profileImageBitmap = profileImageBitmap,
                    totalAmount = totalAmount,
                    budgetLimit = budgetLimit,
                    expenses = expenses,
                    hasUnreadNotifications = hasUnreadNotifications,
                    onAddExpenseClick = { category ->
                        addDialogPrefillCategory = category
                        showAddDialog = true
                    },
                    onNavigate = { tab -> currentTab = tab },
                    onShowNotifications = {
                        viewModel.markNotificationsAsRead()
                        showNotificationsDialog = true
                    }
                )
                "analytics" -> AnalyticsTab(
                    expenses = expenses,
                    budgetLimit = budgetLimit
                )
                "history" -> HistoryTab(
                    expenses = expenses,
                    onDeleteExpense = { viewModel.deleteExpense(it) },
                    onImportExpenses = { importedList, callback ->
                        viewModel.importExpenses(importedList, callback)
                    },
                    onRequestStoragePermission = { action ->
                        requestStoragePermission(action)
                    }
                )
                "profile" -> ProfileTab(
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
                    activity = activity
                )
            }
        }


        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
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
                            Triple("history", Icons.AutoMirrored.Rounded.List, "Logs"),
                            Triple("analytics", Icons.Rounded.BarChart, "Stats"),
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
                                        if (isSelected) ThemeBackground else Color.Transparent
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
                                        tint = if (isSelected) DarkCardSurface else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = label,
                                            color = DarkCardSurface,
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
                            addDialogPrefillCategory = ""
                            showAddDialog = true
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


        if (showNotificationsDialog) {
            Dialog(onDismissRequest = { showNotificationsDialog = false }) {
                val notificationSections = remember(expenses, budgetLimit, totalAmount) {
                    val sections = mutableListOf<Pair<String, List<NotificationItem>>>()


                    val budgetAlerts = mutableListOf<NotificationItem>()
                    val percentUsed = if (budgetLimit > 0) (totalAmount / budgetLimit * 100).toInt() else 0

                    if (totalAmount > budgetLimit) {
                        budgetAlerts.add(NotificationItem(
                            title = "Budget Exceeded",
                            text = "You've overspent by ৳${String.format(Locale.US, "%,.0f", totalAmount - budgetLimit)}. Review your recent expenses.",
                            icon = Icons.Rounded.Error,
                            color = PrimaryAccent,
                            severity = 2
                        ))
                    } else if (percentUsed >= 80) {
                        budgetAlerts.add(NotificationItem(
                            title = "High Spending Alert",
                            text = "$percentUsed% of budget used (৳${String.format(Locale.US, "%,.0f", budgetLimit - totalAmount)} remaining).",
                            icon = Icons.Rounded.Warning,
                            color = PrimaryAccent,
                            severity = 1
                        ))
                    } else if (expenses.isNotEmpty()) {
                        budgetAlerts.add(NotificationItem(
                            title = "Budget On Track",
                            text = "$percentUsed% used — ৳${String.format(Locale.US, "%,.0f", budgetLimit - totalAmount)} remaining this month.",
                            icon = Icons.Rounded.CheckCircle,
                            color = TextPrimary,
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
                                color = PrimaryAccent,
                                severity = if (topPct > 50) 1 else 0
                            ))
                        }


                        val avgPerTxn = totalAmount / expenses.size
                        analysis.add(NotificationItem(
                            title = "Avg. Transaction: ৳${String.format(Locale.US, "%,.0f", avgPerTxn)}",
                            text = "Across ${expenses.size} total transactions.",
                            icon = Icons.Rounded.Calculate,
                            color = TextPrimary,
                            severity = 0
                        ))


                        val catCount = categoryGroups.size
                        analysis.add(NotificationItem(
                            title = "$catCount Active ${if (catCount == 1) "Category" else "Categories"}",
                            text = categoryGroups.keys.joinToString(", "),
                            icon = Icons.Rounded.Category,
                            color = TextSecondary,
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
                                color = TextPrimary,
                                severity = 0
                            ))
                        } else {
                            trends.add(NotificationItem(
                                title = "No Spending Today",
                                text = "Great discipline! Keep it going.",
                                icon = Icons.Rounded.Savings,
                                color = TextSecondary,
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
                                color = TextPrimary,
                                severity = 0
                            ))
                        } else {
                            trends.add(NotificationItem(
                                title = "Over-Budget Warning",
                                text = "Projected to exceed budget by ৳${String.format(Locale.US, "%,.0f", -predictedSavings)}. Reduce daily spending.",
                                icon = Icons.Rounded.TrendingUp,
                                color = PrimaryAccent,
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
                            color = TextSecondary,
                            severity = 0
                        ))


                        val biggest = expenses.maxByOrNull { it.amount }
                        biggest?.let {
                            insights.add(NotificationItem(
                                title = "Biggest Expense: ৳${String.format(Locale.US, "%,.0f", it.amount)}",
                                text = "${it.description.ifEmpty { it.category }} in ${it.category}.",
                                icon = Icons.Rounded.Lightbulb,
                                color = TextPrimary,
                                severity = 0
                            ))
                        }
                    } else {
                        insights.add(NotificationItem(
                            title = "Get Started",
                            text = "Tap the bolt button to log your first expense and unlock insights!",
                            icon = Icons.Rounded.Lightbulb,
                            color = TextSecondary,
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
    onShowNotifications: () -> Unit
) {
    val context = LocalContext.current
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

                val hubCategories = remember(expenses) {
                    (listOf("Food", "Other") + expenses.map { it.category }).distinct()
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hubCategories) { catName ->
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
            items(displayExpenses) { expense ->
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
    budgetLimit: Double
) {

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

    val bottomPadding = 112.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = bottomPadding, top = 24.dp)
    ) {

        item {
            Text(
                text = "Spending Analytics",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Deep insights into your spending patterns.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

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
    onDeleteExpense: (Int) -> Unit,
    onImportExpenses: (List<Expense>, (Boolean, Int) -> Unit) -> Unit,
    onRequestStoragePermission: (() -> Unit) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showImportGuide by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showExportFormatDialog by remember { mutableStateOf(false) }

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
                        ExcelHelper.exportExpenses(outputStream, expenses)
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
                        PdfHelper.exportExpenses(outputStream, expenses)
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
        Text(
            text = "Transaction Logs",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            color = TextPrimary,
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
        )
        Text(
            text = "Filter, search, and review your logs.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Button(
                onClick = { showImportGuide = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryAccent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Logs", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }


            Button(
                onClick = {
                    if (expenses.isEmpty()) {
                        Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                    } else {
                        showExportFormatDialog = true
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardSurface,
                    contentColor = TextPrimary
                ),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PrimaryAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Logs", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                }
            }
        }

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
                items(filteredExpenses) { expense ->
                    ExpenseItem(
                        expense = expense,
                        onDelete = { expenseToDelete = expense }
                    )
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

    if (showExportFormatDialog) {
        AlertDialog(
            onDismissRequest = { showExportFormatDialog = false },
            title = { Text("Export Transactions", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select your preferred format for exporting transaction logs:", color = TextSecondary)
                    
                    Button(
                        onClick = {
                            showExportFormatDialog = false
                            onRequestStoragePermission {
                                pdfExportLauncher.launch("expenses_export.pdf")
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
                                exportLauncher.launch("expenses_export.xls")
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
    val dateFormat = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.US)
    val dateStr = dateFormat.format(Date(expense.date))

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
                val style = getCategoryStyle(expense.category)
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
        "salary", "income" -> Icons.Rounded.Payments
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
