package com.example.communitymaintenance

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

// --- MAIN SCREEN CONTROLLER ---
@Composable
fun IncomeScreen(navController: NavController) {
    val context = LocalContext.current
    var isFormOpen by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<Record?>(null) }

    // --- FILTER STATES ---
    // Default to current month
    var selectedMonthCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var searchQuery by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }

    // --- DATA LOADING & FILTERING ---
    val incomeRecords = remember(refreshTrigger, selectedMonthCalendar.timeInMillis, searchQuery) {
        val db = DataManager.loadDatabase(context).records
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val targetMonth = monthFormat.format(selectedMonthCalendar.time)

        db.filter { record ->
            // 1. Must be Income
            record.type == "income" &&
                    // 2. Must match selected Month (e.g. "2026-02")
                    record.date.startsWith(targetMonth) &&
                    // 3. Must match Search Query (Flat number)
                    (searchQuery.isEmpty() || record.flat_no?.contains(searchQuery, ignoreCase = true) == true)
        }.sortedByDescending { it.date }
    }

    if (isFormOpen) {
        IncomeForm(
            existingRecord = recordToEdit,
            onSave = { isFormOpen = false; refreshTrigger++ },
            onCancel = { isFormOpen = false }
        )
    } else {
        IncomeList(
            records = incomeRecords,
            currentMonth = selectedMonthCalendar,
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            onMonthChange = { newCal -> selectedMonthCalendar = newCal },
            onAddClick = { recordToEdit = null; isFormOpen = true },
            onEditClick = { record -> recordToEdit = record; isFormOpen = true },
            onDeleteClick = { id -> DataManager.deleteRecord(context, id); refreshTrigger++ },
            navController = navController
        )
    }
}

// --- VIEW 1: THE LIST WITH FILTERS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeList(
    records: List<Record>,
    currentMonth: Calendar,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onMonthChange: (Calendar) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Record) -> Unit,
    onDeleteClick: (String) -> Unit,
    navController: NavController
) {
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.US).format(currentMonth.time)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Income Records") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) { Icon(Icons.Default.Add, "Add") }
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {

            // --- MONTH SELECTOR ---
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newCal = currentMonth.clone() as Calendar
                    newCal.add(Calendar.MONTH, -1)
                    onMonthChange(newCal)
                }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev") }

                Text(monthLabel, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                IconButton(onClick = {
                    val newCal = currentMonth.clone() as Calendar
                    newCal.add(Calendar.MONTH, 1)
                    onMonthChange(newCal)
                }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next") }
            }

            Spacer(Modifier.height(12.dp))

            // --- SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Search Flat Number") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // --- LIST ---
            if (records.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records for this month.", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(records) { record ->
                        IncomeCard(record, onEditClick, onDeleteClick)
                    }
                }
            }
        }
    }
}

// --- CARD COMPONENT (Same as before, slight UI polish) ---
@Composable
fun IncomeCard(record: Record, onEdit: (Record) -> Unit, onDelete: (String) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete?") },
            text = { Text("Delete income from Flat ${record.flat_no}?") },
            confirmButton = {
                TextButton(onClick = { onDelete(record.id); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Flat ${record.flat_no ?: "?"}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(record.date, fontSize = 12.sp, color = Color.Gray)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("₹${record.amount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    record.status?.uppercase() ?: "",
                    fontSize = 10.sp,
                    color = if (record.status == "paid") Color(0xFF2E7D32) else Color(0xFFEF6C00),
                    fontWeight = FontWeight.Bold
                )
            }

            // Edit/Delete Icons
            Row {
                IconButton(onClick = { onEdit(record) }) { Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

// --- FORM COMPONENT (Same as before) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeForm(
    existingRecord: Record?,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val flats = remember { DataManager.getFlats(context) }

    var date by remember { mutableStateOf(existingRecord?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var selectedFlat by remember { mutableStateOf(existingRecord?.flat_no ?: (if (flats.isNotEmpty()) flats[0] else "")) }
    var amount by remember { mutableStateOf(existingRecord?.amount?.toInt()?.toString() ?: "") }
    var status by remember { mutableStateOf(existingRecord?.status?.replaceFirstChar { it.uppercase() } ?: "Paid") }

    var flatExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context, { _, y, m, d ->
            calendar.set(y, m, d)
            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (existingRecord == null) "Add Income" else "Edit Income") },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") } })
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {

            OutlinedTextField(
                value = date, onValueChange = {}, readOnly = true, label = { Text("Date") },
                trailingIcon = { Icon(Icons.Default.DateRange, null, Modifier.clickable { datePickerDialog.show() }) },
                modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }
            )
            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(expanded = flatExpanded, onExpandedChange = { flatExpanded = it }) {
                OutlinedTextField(value = selectedFlat, onValueChange = {}, readOnly = true, label = { Text("Flat") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = flatExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = flatExpanded, onDismissRequest = { flatExpanded = false }) {
                    flats.forEach { flat -> DropdownMenuItem(text = { Text(flat) }, onClick = { selectedFlat = flat; flatExpanded = false }) }
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = amount, onValueChange = { if (it.all { c -> c.isDigit() }) amount = it },
                label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                OutlinedTextField(value = status, onValueChange = {}, readOnly = true, label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    listOf("Paid", "Pending").forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { status = s; statusExpanded = false }) }
                }
            }
            Spacer(Modifier.height(24.dp))

            Button(onClick = {
                if (amount.isNotEmpty()) {
                    if (existingRecord == null) {
                        val id = "INC-${date}-${UUID.randomUUID().toString().take(4)}"
                        val record = Record(id, "income", date, amount.toDouble(), flat_no = selectedFlat, status = status.lowercase())
                        DataManager.saveRecord(context, record)
                    } else {
                        val updated = existingRecord.copy(date = date, amount = amount.toDouble(), flat_no = selectedFlat, status = status.lowercase())
                        DataManager.updateRecord(context, updated)
                    }
                    onSave()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save Income") }
        }
    }
}