package com.example.govchatbotapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.govchatbotapp.data.ComplaintEntity
import com.example.govchatbotapp.data.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = remember { FirebaseRepository() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val complaints by repository.getAllComplaints()
        .catch { e -> errorMessage = e.message }
        .collectAsState(initial = emptyList())
        
    val coroutineScope = rememberCoroutineScope()
    
    val categories = listOf("All", "Water", "Road", "Electricity", "Other")
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    
    val filteredComplaints = remember(complaints, selectedCategoryIndex) {
        if (selectedCategoryIndex == 0) {
            complaints
        } else {
            val selectedCat = categories[selectedCategoryIndex]
            complaints.filter { it.department.contains(selectedCat, ignoreCase = true) }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                edgePadding = 0.dp
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                }
            } else if (filteredComplaints.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No complaints found for this category.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredComplaints) { complaint ->
                        AdminComplaintCard(
                            complaint = complaint,
                            onUpdateStatus = { newStatus ->
                                coroutineScope.launch {
                                    repository.updateComplaintStatus(complaint.id, newStatus)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminComplaintCard(complaint: ComplaintEntity, onUpdateStatus: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(complaint.timestamp))
    
    val statusColor = when (complaint.status.lowercase()) {
        "resolved" -> Color(0xFF4CAF50)
        "working on it" -> Color(0xFFFF9800)
        "not resolved" -> Color(0xFFF44336)
        else -> Color.Gray
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = complaint.department,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = complaint.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.Gray
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = complaint.description, style = MaterialTheme.typography.bodyMedium)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Submitted: $dateString",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                var expanded by remember { mutableStateOf(false) }
                
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text("Update Status")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Working on it") },
                            onClick = { onUpdateStatus("Working on it"); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Resolved") },
                            onClick = { onUpdateStatus("Resolved"); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Not Resolved") },
                            onClick = { onUpdateStatus("Not Resolved"); expanded = false }
                        )
                    }
                }
            }
            } // Close if (isExpanded)
        }
    }
}
