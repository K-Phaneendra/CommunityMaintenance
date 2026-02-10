package com.example.communitymaintenance

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance()) }

    val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(selectedMonth.time)
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(selectedMonth.time)

    val allRecords = DataManager.loadDatabase(context).records
    val monthRecords = allRecords.filter { it.date.startsWith(monthStr) }

    // Calculations for UI Preview
    val paidIncome = monthRecords.filter { it.type == "income" && it.status == "paid" }.sumOf { it.amount }
    val monthlyExpense = monthRecords.filter { it.type == "expense" }.sumOf { it.amount }
    val monthlyPending = monthRecords.filter { it.type == "income" && it.status == "pending" }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Reports") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {

            // Month Selector
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium).padding(8.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val new = selectedMonth.clone() as Calendar
                    new.add(Calendar.MONTH, -1)
                    selectedMonth = new
                }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }

                Text(monthName, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                IconButton(onClick = {
                    val new = selectedMonth.clone() as Calendar
                    new.add(Calendar.MONTH, 1)
                    selectedMonth = new
                }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
            }

            Spacer(Modifier.height(24.dp))

            // UI Summary Table Preview
            Card(
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Monthly Preview", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))

                    ReportRow("Total Income (Paid)", "₹$paidIncome", Color(0xFF2E7D32))
                    ReportRow("Total Expenses", "₹$monthlyExpense", Color.Red)
                    ReportRow("Total Pending", "₹$monthlyPending", Color(0xFFEF6C00))

                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    ReportRow("Net Balance", "₹${paidIncome - monthlyExpense}", MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Export Section
            val year = SimpleDateFormat("yyyy", Locale.US).format(selectedMonth.time)
            Button(
                onClick = {
                    val csvFile = DataManager.generateYearlyCsv(context, year)
                    shareFile(context, csvFile)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Export Year $year CSV")
            }

            Text(
                "The CSV will be organized with separate tables for Pending Income, Expenses, and Summary for each month.",
                fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 12.dp),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ReportRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween) {
        Text(label, fontSize = 16.sp)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// Helper function to share the CSV
fun shareFile(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.provider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Community Maintenance Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(intent, "Share Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        // This will help you find the error in Android Studio's Logcat
        android.util.Log.e("REPORT_ERROR", "Sharing failed: ${e.message}")
    }
}
