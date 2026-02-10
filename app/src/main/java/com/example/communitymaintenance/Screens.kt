package com.example.communitymaintenance

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    val summary = remember { DataManager.getSummary(context) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Community Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // ... Summary Cards remain the same ...
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

        // --- ACTION BUTTONS ---
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

        // NEW: Reports Button
        Button(
            onClick = { navController.navigate("reports") },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Info, null); Spacer(Modifier.width(8.dp)); Text("Financial Reports")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = { navController.navigate("files") }, modifier = Modifier.fillMaxWidth()) {
            Text("View Local JSON Files")
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
    // Get all files in the internal storage (JSON and CSV)
    val files = remember { context.filesDir.listFiles()?.toList() ?: emptyList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Files") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { p ->
        LazyColumn(Modifier.padding(p).padding(16.dp)) {
            items(files) { file ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.name, fontWeight = FontWeight.Bold)
                            Text("${file.length() / 1024} KB", fontSize = 12.sp, color = Color.Gray)
                        }

                        // Download/Share Button
                        IconButton(onClick = { shareAnyFile(context, file) }) {
                            Icon(Icons.Default.Share, contentDescription = "Export File", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

// Global helper to share any file (JSON or CSV)
fun shareAnyFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Must match Manifest!
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            // Determine type based on extension
            type = if (file.name.endsWith(".csv")) "text/csv" else "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Export ${file.name}"))
    } catch (e: Exception) {
        e.printStackTrace()
        // If it still crashes, this will print the error in Logcat
    }
}
