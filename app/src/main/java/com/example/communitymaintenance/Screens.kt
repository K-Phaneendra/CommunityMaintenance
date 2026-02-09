package com.example.communitymaintenance

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// --- 1. HOME SCREEN (Dashboard) ---
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    // Recalculate summary every time screen loads
    val summary = remember { DataManager.getSummary(context) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Community Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Summary Cards
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Balance", "₹${summary.balance}", MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
            SummaryCard("Pending", "₹${summary.pendingIncome}", MaterialTheme.colorScheme.errorContainer, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Total Income", "₹${summary.totalIncome}", Color.Gray.copy(alpha = 0.1f), Modifier.weight(1f))
            SummaryCard("Total Exp", "₹${summary.totalExpense}", Color.Gray.copy(alpha = 0.1f), Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        // Action Buttons
        Button(onClick = { navController.navigate("income") }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add Income")
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { navController.navigate("expense") },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Default.ShoppingCart, null); Spacer(Modifier.width(8.dp)); Text("Add Expense")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { navController.navigate("files") }, modifier = Modifier.fillMaxWidth()) {
            Text("View Files")
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, color: Color, modifier: Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = color), modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 14.sp)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- 4. FILE LIST ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(navController: NavController) {
    val context = LocalContext.current
    val files = remember { DataManager.getFileList(context) }
    Scaffold(topBar = { TopAppBar(title = { Text("Local Files") }) }) { p ->
        LazyColumn(Modifier.padding(p).padding(16.dp)) {
            items(files) { file ->
                Card(Modifier.fillMaxWidth().padding(4.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(file.name, fontWeight = FontWeight.Bold)
                        Text("${file.length() / 1024} KB", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}