package com.example.communitymaintenance

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

// --- MAIN SCREEN CONTROLLER ---
@Composable
fun ExpenseScreen(navController: NavController) {
    val context = LocalContext.current
    var isFormOpen by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<Record?>(null) }

    var selectedMonthCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var searchQuery by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }

    // Filter Logic
    val expenseRecords = remember(refreshTrigger, selectedMonthCalendar.timeInMillis, searchQuery) {
        val db = DataManager.loadDatabase(context).records
        val targetMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(selectedMonthCalendar.time)

        db.filter { record ->
            record.type == "expense" &&
                    record.date.startsWith(targetMonth) &&
                    (searchQuery.isEmpty() || record.expense_name?.contains(searchQuery, ignoreCase = true) == true)
        }.sortedByDescending { it.date }
    }

    if (isFormOpen) {
        ExpenseForm(
            existingRecord = recordToEdit,
            onSave = { isFormOpen = false; refreshTrigger++ },
            onCancel = { isFormOpen = false }
        )
    } else {
        ExpenseList(
            records = expenseRecords,
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

// --- VIEW 1: THE LIST ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseList(
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
                title = { Text("Expense Records") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) { Icon(Icons.Default.Add, "Add") }
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            // Month Flipper
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), MaterialTheme.shapes.medium).padding(8.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newCal = currentMonth.clone() as Calendar
                    newCal.add(Calendar.MONTH, -1)
                    onMonthChange(newCal)
                }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
                Text(monthLabel, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    val newCal = currentMonth.clone() as Calendar
                    newCal.add(Calendar.MONTH, 1)
                    onMonthChange(newCal)
                }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery, onValueChange = onSearchChange,
                label = { Text("Search Expense Type") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            if (records.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No expenses found.", color = Color.Gray) }
            } else {
                LazyColumn {
                    items(records) { record ->
                        ExpenseCard(record, onEditClick, onDeleteClick)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(record: Record, onEdit: (Record) -> Unit, onDelete: (String) -> Unit) {
    var showDel by remember { mutableStateOf(false) }
    var showPhoto by remember { mutableStateOf(false) } // State for Photo Viewer

    // 1. Delete Confirmation Dialog
    if (showDel) {
        AlertDialog(
            onDismissRequest = { showDel = false },
            title = { Text("Delete Expense?") },
            text = { Text("Remove ${record.expense_name} record?") },
            confirmButton = { TextButton(onClick = { onDelete(record.id); showDel = false }) { Text("Delete", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showDel = false }) { Text("Cancel") } }
        )
    }

    // 2. Photo Viewer Dialog
    if (showPhoto && record.photo_files.isNotEmpty()) {
        PhotoViewDialog(fileName = record.photo_files.first()) {
            showPhoto = false
        }
    }

    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(record.expense_name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(record.date, fontSize = 12.sp, color = Color.Gray)

                // Show "Receipt" indicator
                if (record.photo_files.isNotEmpty()) {
                    Row(
                        modifier = Modifier.clickable { showPhoto = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                        Text(" View Receipt", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                    }
                }
            }

            Text("₹${record.amount.toInt()}", fontWeight = FontWeight.Bold, color = Color.Red)

            // Actions
            Row {
                if (record.photo_files.isNotEmpty()) {
                    IconButton(onClick = { showPhoto = true }) {
                        Icon(Icons.Default.PlayArrow, "View Photo", tint = Color.Gray) // PlayArrow or a custom Eye icon
                    }
                }
                IconButton(onClick = { onEdit(record) }) { Icon(Icons.Default.Edit, null, tint = Color.Blue) }
                IconButton(onClick = { showDel = true }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
            }
        }
    }
}

// --- VIEW 2: THE FORM (Camera/Gallery enabled) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseForm(existingRecord: Record?, onSave: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val expenses = remember { DataManager.getStandardExpenses(context) }

    var date by remember { mutableStateOf(existingRecord?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var selectedExp by remember { mutableStateOf(existingRecord?.expense_name ?: expenses[0]) }
    var amount by remember { mutableStateOf(existingRecord?.amount?.toInt()?.toString() ?: "") }
    var photoFileName by remember { mutableStateOf(existingRecord?.photo_files?.firstOrNull()) }
    var expExpanded by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }

    // Photo Launchers
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (!success) photoFileName = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val filename = "IMG_GAL_${System.currentTimeMillis()}.jpg"
            context.contentResolver.openInputStream(it)?.use { input ->
                File(context.filesDir, filename).outputStream().use { output -> input.copyTo(output) }
            }
            photoFileName = filename
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if(existingRecord == null) "Add Expense" else "Edit Expense") },
            navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Default.Close, null) } })
    }) { p ->
        Column(Modifier.padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
            // Date
            OutlinedTextField(value = date, onValueChange = {}, readOnly = true, label = { Text("Date") },
                trailingIcon = { Icon(Icons.Default.DateRange, null, Modifier.clickable {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(context, { _, y, m, d ->
                        cal.set(y, m, d)
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }) }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))

            // Type
            ExposedDropdownMenuBox(expanded = expExpanded, onExpandedChange = { expExpanded = it }) {
                OutlinedTextField(value = selectedExp, onValueChange = {}, readOnly = true, label = { Text("Expense Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = expExpanded, onDismissRequest = { expExpanded = false }) {
                    expenses.forEach { exp -> DropdownMenuItem(text = { Text(exp) }, onClick = { selectedExp = exp; expExpanded = false }) }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Amount
            OutlinedTextField(value = amount, onValueChange = { if (it.all { c -> c.isDigit() }) amount = it },
                label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))

            // Photo Button
            Button(onClick = { showPhotoDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp))
                Text(if (photoFileName == null) "Attach Receipt/Photo" else "Photo Attached: $photoFileName")
            }

            if (showPhotoDialog) {
                AlertDialog(onDismissRequest = { showPhotoDialog = false }, title = { Text("Source") }, text = { Text("Take photo or pick from gallery?") },
                    confirmButton = { TextButton(onClick = {
                        val name = "IMG_${System.currentTimeMillis()}.jpg"
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", File(context.filesDir, name))
                        photoFileName = name
                        cameraLauncher.launch(uri)
                        showPhotoDialog = false
                    }) { Text("Camera") } },
                    dismissButton = { TextButton(onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        showPhotoDialog = false
                    }) { Text("Gallery") } }
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(onClick = {
                if (amount.isNotEmpty()) {
                    val record = if (existingRecord == null) {
                        Record(id = "EXP-${System.currentTimeMillis()}", type = "expense", date = date, amount = amount.toDouble(), expense_name = selectedExp, photo_files = listOfNotNull(photoFileName))
                    } else {
                        existingRecord.copy(date = date, amount = amount.toDouble(), expense_name = selectedExp, photo_files = listOfNotNull(photoFileName))
                    }
                    if (existingRecord == null) DataManager.saveRecord(context, record) else DataManager.updateRecord(context, record)
                    onSave()
                }
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text(if (existingRecord == null) "Save Expense" else "Update Expense")
            }
        }
    }
}

@Composable
fun PhotoViewDialog(fileName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val file = File(context.filesDir, fileName)

    // State for zoom and pan
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val bitmap = remember(fileName) {
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        } else {
            null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // This modifier detects pinch-to-zoom and dragging
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f) // Limit zoom between 1x and 5x

                            // Only allow panning if we are zoomed in
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Expense Receipt",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("Image not found", color = Color.White)
                }

                // Close Button - Positioned at the top to stay visible
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Reset Zoom Label (Optional helper)
                if (scale > 1f) {
                    Text(
                        "Pinch to zoom / Drag to move",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    )
                }
            }
        }
    }
}
