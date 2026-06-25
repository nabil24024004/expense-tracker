package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.DialogProperties
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryDialog(
    initialCategory: String = "",
    existingCategories: List<String> = listOf("Food", "Other"),
    onDismiss: () -> Unit,
    onExpenseConfirm: (amount: Double, description: String, category: String, date: Long, imageBytes: ByteArray?, foodDetails: String?) -> Unit,
    onDebtConfirm: (personName: String, amount: Double, description: String, type: String, dueDate: Long?) -> Unit
) {
    // Top-level tab state: "TRANSACTION" or "DEBT"
    var activeTab by remember { mutableStateOf("TRANSACTION") }

    // Common context
    val context = LocalContext.current

    // --- TRANSACTION STATE ---
    var expAmount by remember { mutableStateOf("") }
    var expDescription by remember { mutableStateOf("") }
    var expCategory by remember { mutableStateOf(initialCategory) }
    var expDateMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var expDateStr by remember {
        mutableStateOf(
            SimpleDateFormat("dd MMM, yyyy", Locale.US).format(Date(System.currentTimeMillis()))
        )
    }

    val expDatePickerDialog = remember {
        val calendar = Calendar.getInstance()
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth)
                val now = Calendar.getInstance()
                selectedCal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                selectedCal.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                selectedCal.set(Calendar.SECOND, now.get(Calendar.SECOND))
                expDateMs = selectedCal.timeInMillis
                expDateStr = SimpleDateFormat("dd MMM, yyyy", Locale.US).format(selectedCal.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    var foodDetails by remember { mutableStateOf("") }
    var foodImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = compressUriToByteArray(context, uri)
            if (bytes != null) {
                foodImageBytes = bytes
            }
        }
    }

    // --- DEBT STATE ---
    var debtPersonName by remember { mutableStateOf("") }
    var debtAmount by remember { mutableStateOf("") }
    var debtDescription by remember { mutableStateOf("") }
    var debtType by remember { mutableStateOf("DEBT") } // "DEBT" or "DUE"
    var debtDueDateMs by remember { mutableStateOf<Long?>(null) }
    var debtDueDateStr by remember { mutableStateOf("") }

    val debtDatePickerDialog = remember {
        val calendar = Calendar.getInstance()
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth, 23, 59, 59)
                debtDueDateMs = selectedCal.timeInMillis
                debtDueDateStr = SimpleDateFormat("dd MMM, yyyy", Locale.US).format(selectedCal.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeBackground),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .border(1.dp, CardSurface, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Tab Selector (always visible)
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(CardSurface, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf("TRANSACTION" to "Transaction", "DEBT" to "Debt")
                        tabs.forEach { (tabKey, tabLabel) ->
                            val isSelected = activeTab == tabKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (isSelected) PrimaryAccent else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { activeTab = tabKey },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabLabel,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Scrollable fields Column
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (activeTab == "TRANSACTION") {
                        // --- TRANSACTION TAB CONTENT ---
                        Text(
                            text = "Log Transaction",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                        )

                        OutlinedTextField(
                            value = expAmount,
                            onValueChange = { expAmount = it },
                            label = { Text("Amount (৳)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

                        OutlinedTextField(
                            value = expDescription,
                            onValueChange = { expDescription = it },
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
                            value = expCategory,
                            onValueChange = { expCategory = it },
                            label = { Text("Enter Category") },
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

                        if (existingCategories.isNotEmpty()) {
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
                                items(existingCategories) { catName ->
                                    val isSelected = expCategory.trim().lowercase(Locale.US) == catName.trim().lowercase(Locale.US)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { expCategory = catName },
                                        label = { Text(catName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryAccent,
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

                        if (expCategory.trim().lowercase(Locale.US) == "food") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Food Details (Diary Log)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryAccent
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = foodDetails,
                                onValueChange = { foodDetails = it },
                                label = { Text("What did you eat?") },
                                placeholder = { Text("e.g. Avocado Toast, cappuccino...") },
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
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            if (foodImageBytes == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .border(
                                            border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .background(CardSurface.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .clickable { photoPickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Rounded.AddAPhoto,
                                            contentDescription = "Attach Food Picture",
                                            tint = TextSecondary
                                        )
                                        Text(
                                            text = "Attach Food Picture",
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                val bitmap = remember(foodImageBytes) {
                                    try {
                                        BitmapFactory.decodeByteArray(foodImageBytes, 0, foodImageBytes!!.size)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (bitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, CardSurface, RoundedCornerShape(12.dp))
                                    ) {
                                        androidx.compose.foundation.Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Food Picture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(32.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                .clickable { foodImageBytes = null },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Rounded.Close,
                                                contentDescription = "Remove Picture",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Transaction Date Picker Field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CardSurface, RoundedCornerShape(12.dp))
                                .clickable { expDatePickerDialog.show() }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Transaction Date",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = expDateStr,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
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
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        // --- DEBT TAB CONTENT ---
                        Text(
                            text = "Log Debt",
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
                            val types = listOf("DEBT" to "Owe", "DUE" to "Owed to Me")
                            types.forEach { (typeKey, typeLabel) ->
                                val isSelected = debtType == typeKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            if (isSelected) PrimaryAccent else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { debtType = typeKey },
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

                        // Person Name Field
                        OutlinedTextField(
                            value = debtPersonName,
                            onValueChange = { debtPersonName = it },
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
                            value = debtAmount,
                            onValueChange = { debtAmount = it },
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
                            value = debtDescription,
                            onValueChange = { debtDescription = it },
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

                        // Due Date Picker Field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CardSurface, RoundedCornerShape(12.dp))
                                .clickable { debtDatePickerDialog.show() }
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
                                        text = debtDueDateStr.ifEmpty { "Select Due Date" },
                                        color = if (debtDueDateStr.isEmpty()) TextSecondary else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
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
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Fixed bottom button Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSurface.copy(alpha = 0.15f))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeTab == "TRANSACTION") {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val amt = expAmount.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    val finalCategory = if (expCategory.trim().isEmpty()) "Other" else expCategory.trim()
                                    val isFood = finalCategory.trim().lowercase(Locale.US) == "food"
                                    onExpenseConfirm(
                                        amt,
                                        expDescription,
                                        finalCategory,
                                        expDateMs,
                                        if (isFood) foodImageBytes else null,
                                        if (isFood) foodDetails else null
                                    )
                                }
                            },
                            modifier = Modifier.testTag("save_expense_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val amt = debtAmount.toDoubleOrNull() ?: 0.0
                                if (debtPersonName.trim().isNotEmpty() && amt > 0) {
                                    onDebtConfirm(
                                        debtPersonName.trim(),
                                        amt,
                                        debtDescription.trim(),
                                        debtType,
                                        debtDueDateMs
                                    )
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
}

fun compressUriToByteArray(context: android.content.Context, uri: Uri): ByteArray? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (originalBitmap == null) return null
        
        val maxDim = 600
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scaledBitmap = if (width > maxDim || height > maxDim) {
            val ratio = width.toFloat() / height.toFloat()
            val newWidth = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
            val newHeight = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }
        
        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        outputStream.close()
        
        if (scaledBitmap != originalBitmap) {
            scaledBitmap.recycle()
        }
        originalBitmap.recycle()
        
        bytes
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
