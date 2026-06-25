package com.example.group.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.group.data.entity.GroupEntity
import com.example.group.viewmodel.GroupViewModel
import com.example.ui.theme.*

@Composable
fun GroupsHomeScreen(
    viewModel: GroupViewModel,
    showTopHeader: Boolean = true,
    showFloatingActionButton: Boolean = true
) {
    val selectedGroupId by viewModel.selectedGroupId.collectAsState()

    AnimatedContent(
        targetState = selectedGroupId,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "GroupScreenNavigation"
    ) { targetGroupId ->
        if (targetGroupId != null) {
            GroupDetailScreen(
                groupId = targetGroupId,
                viewModel = viewModel,
                onBack = { viewModel.selectGroup(null) }
            )
        } else {
            GroupListScreen(
                viewModel = viewModel,
                showTopHeader = showTopHeader,
                showFloatingActionButton = showFloatingActionButton
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    viewModel: GroupViewModel,
    showTopHeader: Boolean = true,
    showFloatingActionButton: Boolean = true
) {
    val groups by viewModel.allGroups.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Active, 1 = Archived

    val showCreateGroupTrigger by viewModel.showCreateGroupDialog.collectAsState()
    LaunchedEffect(showCreateGroupTrigger) {
        if (showCreateGroupTrigger) {
            showAddDialog = true
            viewModel.setShowCreateGroupDialog(false)
        }
    }

    val filteredGroups = remember(groups, selectedTab) {
        if (selectedTab == 0) groups.filter { !it.isArchived }
        else groups.filter { it.isArchived }
    }

    Scaffold(
        containerColor = ThemeBackground,
        floatingActionButton = {
            if (showFloatingActionButton) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = PrimaryAccent,
                    contentColor = DarkCardTextPrimary,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 80.dp) // Leave room for bottom navigation bar
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Group",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = if (showTopHeader) 16.dp else 0.dp, start = 16.dp, end = 16.dp)
        ) {
            // Header
            if (showTopHeader) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Group Expenses",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Split and settle with ease",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Custom modern tab selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Active Groups", "Archived").forEachIndexed { index, label ->
                    val isTabSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTabSelected) ThemeBackground else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTabSelected) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            contentDescription = "No groups",
                            tint = TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = if (selectedTab == 0) "No active groups yet" else "No archived groups",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        if (selectedTab == 0) {
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                            ) {
                                Text("Create First Group", color = DarkCardTextPrimary)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp) // Bottom padding to avoid FAB and bottom bar overlap
                ) {
                    items(filteredGroups, key = { it.id }) { group ->
                        GroupCard(
                            group = group,
                            onClick = { viewModel.selectGroup(group.id) },
                            onArchive = { viewModel.toggleGroupArchive(group) },
                            onDelete = { viewModel.deleteGroup(group) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddGroupDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, currency, color, desc, members ->
                viewModel.createGroup(name, currency, color, desc, members)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun GroupCard(
    group: GroupEntity,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Small color indicator circle
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(group.color))
                    )
                    Text(
                        text = group.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            tint = TextSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(CardSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (group.isArchived) "Unarchive" else "Archive", color = TextPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (group.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            },
                            onClick = {
                                onArchive()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Group", color = Color.Red) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = Color.Red
                                )
                            },
                            onClick = {
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }

            if (!group.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = group.description,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.People,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Members",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = "Currency: ${group.currency}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryAccent
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, currency: String, color: Int, description: String?, members: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var selectedColor by remember { mutableStateOf(0xFFEA3B35.toInt()) } // default red
    
    val currencies = listOf("USD", "EUR", "GBP", "INR", "JPY", "CAD", "AUD", "BDT")
    val colors = listOf(
        0xFFEA3B35.toInt(), // Rose Red
        0xFF3B82F6.toInt(), // Sky Blue
        0xFF10B981.toInt(), // Emerald Green
        0xFFF59E0B.toInt(), // Amber Gold
        0xFF8B5CF6.toInt(), // Purple
        0xFFEC4899.toInt()  // Pink
    )

    var newMemberName by remember { mutableStateOf("") }
    val membersList = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Create Group",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable fields
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Group Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = CardSurface
                            ),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = CardSurface
                            ),
                            singleLine = true
                        )
                    }

                    item {
                        Text("Select Currency", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Quick Selector Grid or Row scrollable
                            Box(modifier = Modifier.fillMaxWidth()) {
                                var expanded by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardSurface)
                                        .clickable { expanded = true }
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = currency, color = TextPrimary)
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowDropDown,
                                            contentDescription = null,
                                            tint = TextSecondary
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(CardSurface)
                                ) {
                                    currencies.forEach { curr ->
                                        DropdownMenuItem(
                                            text = { Text(curr, color = TextPrimary) },
                                            onClick = {
                                                currency = curr
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Group Color Theme", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            colors.forEach { clr ->
                                val isSelected = selectedColor == clr
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(clr))
                                        .clickable { selectedColor = clr }
                                        .padding(2.dp)
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Add Group Members", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newMemberName,
                                onValueChange = { newMemberName = it },
                                label = { Text("Name") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryAccent,
                                    unfocusedBorderColor = CardSurface
                                ),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    if (newMemberName.isNotBlank() && !membersList.contains(newMemberName.trim())) {
                                        membersList.add(newMemberName.trim())
                                        newMemberName = ""
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(PrimaryAccent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Member",
                                    tint = DarkCardTextPrimary
                                )
                            }
                        }
                    }

                    if (membersList.isNotEmpty()) {
                        item {
                            Text("Members to Add:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        items(membersList) { mbr ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardSurface)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(text = mbr, color = TextPrimary, fontSize = 14.sp)
                                }
                                IconButton(
                                    onClick = { membersList.remove(mbr) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(
                                    name.trim(),
                                    currency,
                                    selectedColor,
                                    description.trim().ifEmpty { null },
                                    membersList.toList()
                                )
                            }
                        },
                        enabled = name.isNotBlank() && membersList.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Text("Create", color = DarkCardTextPrimary)
                    }
                }
            }
        }
    }
}
