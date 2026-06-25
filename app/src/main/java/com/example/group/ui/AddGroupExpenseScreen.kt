package com.example.group.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.group.calculator.SplitCalculator
import com.example.group.data.entity.*
import com.example.group.viewmodel.GroupViewModel
import com.example.ui.screens.compressUriToByteArray
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupExpenseScreen(
    groupId: Int,
    expenseToEdit: GroupExpenseEntity?,
    viewModel: GroupViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val members by viewModel.selectedGroupMembers.collectAsState()
    
    // Core Expense Fields
    var title by remember { mutableStateOf(expenseToEdit?.title ?: "") }
    var amountStr by remember { mutableStateOf(expenseToEdit?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(expenseToEdit?.category ?: "Food") }
    var date by remember { mutableStateOf(expenseToEdit?.date ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(expenseToEdit?.notes ?: "") }
    var receiptBytes by remember { mutableStateOf(expenseToEdit?.receiptBytes) }

    // Payer Mode: 0 = Single Payer, 1 = Multiple Payers
    var payerMode by remember { mutableStateOf(0) }
    var singlePayerId by remember { mutableStateOf<Int?>(null) }
    val multiplePayersMap = remember { mutableStateMapOf<Int, String>() }

    // Split Mode: EQUAL, EXACT, PERCENTAGE, SHARES
    var splitMethod by remember { mutableStateOf(SplitCalculator.SplitMethod.EQUAL) }
    val selectedParticipants = remember { mutableStateListOf<Int>() }
    val splitInputsMap = remember { mutableStateMapOf<Int, String>() }

    // Initialize editing details
    LaunchedEffect(members, expenseToEdit) {
        if (members.isNotEmpty()) {
            if (expenseToEdit == null) {
                // Default: First member paid all
                singlePayerId = members.first().id
                // Default: All members participate in split
                selectedParticipants.clear()
                selectedParticipants.addAll(members.map { it.id })
            } else {
                // Load details for editing
                val existingPayers = viewModel.getExpensePayersDirect(expenseToEdit.id)
                val existingParticipants = viewModel.getExpenseParticipantsDirect(expenseToEdit.id)

                if (existingPayers.size == 1) {
                    payerMode = 0
                    singlePayerId = existingPayers.first().memberId
                } else {
                    payerMode = 1
                    existingPayers.forEach { p ->
                        multiplePayersMap[p.memberId] = p.paidAmount.toString()
                    }
                }

                if (existingParticipants.isNotEmpty()) {
                    val firstPart = existingParticipants.first()
                    splitMethod = when (firstPart.splitMethod) {
                        "EXACT" -> SplitCalculator.SplitMethod.EXACT
                        "PERCENTAGE" -> SplitCalculator.SplitMethod.PERCENTAGE
                        "SHARES" -> SplitCalculator.SplitMethod.SHARES
                        else -> SplitCalculator.SplitMethod.EQUAL
                    }

                    selectedParticipants.clear()
                    selectedParticipants.addAll(existingParticipants.map { it.memberId })

                    existingParticipants.forEach { part ->
                        if (splitMethod != SplitCalculator.SplitMethod.EQUAL) {
                            val inputVal = part.rawWeight ?: part.shareAmount
                            splitInputsMap[part.memberId] = inputVal.toString()
                        }
                    }
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = compressUriToByteArray(context, uri)
            if (bytes != null) {
                receiptBytes = bytes
            } else {
                Toast.makeText(context, "Failed to load/compress receipt image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = ThemeBackground,
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (expenseToEdit == null) "Add Expense" else "Edit Expense", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Cancel", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeBackground, titleContentColor = TextPrimary)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThemeBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        val totalAmt = amountStr.toDoubleOrNull() ?: 0.0
                        if (title.isBlank() || totalAmt <= 0.0) {
                            Toast.makeText(context, "Please enter valid title and total amount.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Generate Payers
                        val payers = mutableListOf<ExpensePayerEntity>()
                        if (payerMode == 0) {
                            if (singlePayerId == null) {
                                Toast.makeText(context, "Please select who paid.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            payers.add(ExpensePayerEntity(expenseId = 0, memberId = singlePayerId!!, paidAmount = totalAmt))
                        } else {
                            var paidSum = 0.0
                            members.forEach { m ->
                                val pAmt = multiplePayersMap[m.id]?.toDoubleOrNull() ?: 0.0
                                if (pAmt > 0.0) {
                                    payers.add(ExpensePayerEntity(expenseId = 0, memberId = m.id, paidAmount = pAmt))
                                    paidSum += pAmt
                                }
                            }
                            if (Math.abs(paidSum - totalAmt) > 0.02) {
                                Toast.makeText(context, "Sum of paid amounts ($paidSum) must equal total expense amount ($totalAmt).", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        }

                        // Generate Participants & split amounts
                        if (selectedParticipants.isEmpty()) {
                            Toast.makeText(context, "At least one member must be selected to split the expense.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val calculatedShares: Map<Int, Double>
                        try {
                            val inputs = selectedParticipants.associateWith { id ->
                                splitInputsMap[id]?.toDoubleOrNull() ?: if (splitMethod == SplitCalculator.SplitMethod.SHARES) 1.0 else 0.0
                            }
                            calculatedShares = SplitCalculator.calculateSplit(
                                method = splitMethod,
                                totalAmount = totalAmt,
                                participants = selectedParticipants.toList(),
                                inputs = inputs
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Invalid split input configuration.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val participants = selectedParticipants.map { mId ->
                            val rawW = splitInputsMap[mId]?.toDoubleOrNull()
                            ExpenseParticipantEntity(
                                expenseId = 0,
                                memberId = mId,
                                shareAmount = calculatedShares[mId] ?: 0.0,
                                splitMethod = splitMethod.name,
                                rawWeight = rawW
                            )
                        }

                        if (expenseToEdit == null) {
                            viewModel.addExpense(
                                groupId = groupId,
                                title = title.trim(),
                                amount = totalAmt,
                                currency = members.firstOrNull()?.let { "" } ?: "USD", // using group default
                                category = category,
                                date = date,
                                notes = notes.trim().ifEmpty { null },
                                receiptBytes = receiptBytes,
                                payers = payers,
                                participants = participants
                            )
                        } else {
                            viewModel.updateExpense(
                                expense = expenseToEdit.copy(
                                    title = title.trim(),
                                    amount = totalAmt,
                                    category = category,
                                    date = date,
                                    notes = notes.trim().ifEmpty { null },
                                    receiptBytes = receiptBytes
                                ),
                                payers = payers,
                                participants = participants
                            )
                        }

                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    Text(text = if (expenseToEdit == null) "Save Expense" else "Update Expense", color = DarkCardTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // General Details
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = CardSurface),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Total Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = CardSurface),
                    singleLine = true
                )
            }

            // Categories
            item {
                Text("Category", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Food", "Transport", "Accommodation", "Entertainment", "Others").forEach { cat ->
                        val isSelected = category.lowercase() == cat.lowercase()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryAccent else CardSurface)
                                .clickable { category = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) DarkCardTextPrimary else TextPrimary
                            )
                        }
                    }
                }
            }

            // Date Picker Field
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardSurface, RoundedCornerShape(12.dp))
                        .clickable {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = date
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, day)
                                    date = newCal.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Expense Date", color = TextSecondary, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = SimpleDateFormat("dd MMM, yyyy", Locale.US).format(Date(date)),
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(imageVector = Icons.Rounded.CalendarToday, contentDescription = "Select Date", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Payer Selection Section
            item {
                Text("Who Paid?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(CardSurface, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf("Single Payer", "Multiple Payers")
                    modes.forEachIndexed { idx, label ->
                        val isSelected = payerMode == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isSelected) PrimaryAccent else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { payerMode = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) DarkCardTextPrimary else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (payerMode == 0) {
                // Single Payer dropdown/selector
                item {
                    Text("Select Payer", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        members.forEach { m ->
                            val isSelected = singlePayerId == m.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryAccent else CardSurface)
                                    .clickable { singlePayerId = m.id }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = m.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) DarkCardTextPrimary else TextPrimary)
                            }
                        }
                    }
                }
            } else {
                // Multiple Payers Inputs list
                items(members) { m ->
                    val pAmt = multiplePayersMap[m.id] ?: ""
                    OutlinedTextField(
                        value = pAmt,
                        onValueChange = { multiplePayersMap[m.id] = it },
                        label = { Text("Amount paid by ${m.name}") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = CardSurface),
                        singleLine = true
                    )
                }
            }

            // Split Rules Section
            item {
                Text("Split Options", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(CardSurface, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SplitCalculator.SplitMethod.values().forEach { method ->
                        val isSelected = splitMethod == method
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isSelected) PrimaryAccent else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { splitMethod = method },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = method.name,
                                color = if (isSelected) DarkCardTextPrimary else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Participants Checklist
            item {
                Text("Split Between", fontSize = 12.sp, color = TextSecondary)
            }

            items(members) { m ->
                val isSelected = selectedParticipants.contains(m.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSurface)
                        .clickable {
                            if (isSelected) {
                                selectedParticipants.remove(m.id)
                            } else {
                                selectedParticipants.add(m.id)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked == true) selectedParticipants.add(m.id)
                                else selectedParticipants.remove(m.id)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryAccent, uncheckedColor = TextSecondary)
                        )
                        Text(text = m.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    }

                    // For EXACT, PERCENTAGE, SHARES show specific fields
                    if (isSelected && splitMethod != SplitCalculator.SplitMethod.EQUAL) {
                        val inputVal = splitInputsMap[m.id] ?: ""
                        val labelSuffix = when (splitMethod) {
                            SplitCalculator.SplitMethod.EXACT -> "Amount"
                            SplitCalculator.SplitMethod.PERCENTAGE -> "%"
                            SplitCalculator.SplitMethod.SHARES -> "Shares"
                            else -> ""
                        }
                        OutlinedTextField(
                            value = inputVal,
                            onValueChange = { splitInputsMap[m.id] = it },
                            label = { Text(labelSuffix, fontSize = 10.sp) },
                            modifier = Modifier.width(100.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = ThemeBackground),
                            singleLine = true
                        )
                    }
                }
            }

            // Notes & Receipt Attachment
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = CardSurface)
                )
            }

            item {
                Text("Receipt image", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                if (receiptBytes == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .border(BorderStroke(1.dp, TextSecondary.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp))
                            .background(CardSurface.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Rounded.AddAPhoto, contentDescription = "Add Receipt", tint = TextSecondary)
                            Text("Attach Receipt Photo", color = TextSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    val bitmap = remember(receiptBytes) {
                        BitmapFactory.decodeByteArray(receiptBytes, 0, receiptBytes!!.size)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Receipt",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { receiptBytes = null }
                                .padding(6.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
