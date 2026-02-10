package com.example.communitymaintenance

import android.content.Context
import com.google.gson.GsonBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.Calendar
import java.util.UUID

// --- Data Models ---
data class MaintenanceData(
    val schema_version: Int = 1,
    val app: String = "community-maintenance",
    val currency: String = "INR",
    val records: MutableList<Record> = mutableListOf()
)

data class Record(
    val id: String,
    val type: String, // "income" or "expense"
    val date: String,
    val amount: Double,
    val flat_no: String? = null,
    val status: String? = null, // "paid" or "pending"
    val expense_name: String? = null,
    val photo_files: List<String> = emptyList()
)

data class DashboardSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val pendingIncome: Double
)

// --- Singleton Manager ---
object DataManager {
    private const val DB_FILE = "maintenance.json"
    private const val FLATS_FILE = "flats.json"
    private const val EXPENSES_FILE = "standardExpenses.json"
    private val gson = GsonBuilder().setPrettyPrinting().create()

    // Load Dropdown Data
    fun getFlats(context: Context): List<String> {
        return try {
            val json = context.assets.open(FLATS_FILE).bufferedReader().use { it.readText() }
            gson.fromJson(json, Array<String>::class.java).toList()
        } catch (e: Exception) { listOf("101", "102", "Error Loading") }
    }

    fun getStandardExpenses(context: Context): List<String> {
        return try {
            val json = context.assets.open(EXPENSES_FILE).bufferedReader().use { it.readText() }
            gson.fromJson(json, Array<String>::class.java).toList()
        } catch (e: Exception) { listOf("Maintenance", "Other") }
    }

    // Database Operations
    fun loadDatabase(context: Context): MaintenanceData {
        val file = File(context.filesDir, DB_FILE)
        if (!file.exists()) return MaintenanceData()
        return try {
            gson.fromJson(file.readText(), MaintenanceData::class.java)
        } catch (e: Exception) { MaintenanceData() }
    }

    fun saveRecord(context: Context, record: Record) {
        val data = loadDatabase(context)
        data.records.add(record)
        File(context.filesDir, DB_FILE).writeText(gson.toJson(data))
    }

    fun getFileList(context: Context): List<File> {
        return context.filesDir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    // --- NEW: Dashboard Calculations ---
    fun getSummary(context: Context): DashboardSummary {
        val records = loadDatabase(context).records

        val income = records.filter { it.type == "income" && it.status?.lowercase() == "paid" }.sumOf { it.amount }
        val pending = records.filter { it.type == "income" && it.status?.lowercase() == "pending" }.sumOf { it.amount }
        val expense = records.filter { it.type == "expense" }.sumOf { it.amount }

        return DashboardSummary(
            totalIncome = income,
            totalExpense = expense,
            balance = income - expense,
            pendingIncome = pending
        )
    }

    fun deleteRecord(context: Context, recordId: String) {
        val data = loadDatabase(context)
        val iterator = data.records.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().id == recordId) {
                iterator.remove()
                break
            }
        }
        File(context.filesDir, DB_FILE).writeText(gson.toJson(data))
    }

    fun updateRecord(context: Context, updatedRecord: Record) {
        val data = loadDatabase(context)
        val index = data.records.indexOfFirst { it.id == updatedRecord.id }
        if (index != -1) {
            data.records[index] = updatedRecord
            File(context.filesDir, DB_FILE).writeText(gson.toJson(data))
        }
    }

    fun generateYearlyCsv(context: Context, year: String): File {
        val data = loadDatabase(context)
        val file = File(context.filesDir, "Maintenance_Report_$year.csv")
        val sb = StringBuilder()

        // --- 1. YEARLY SUMMARY HEADER ---
        // Paid Income only
        val yearlyIncome = data.records.filter { it.type == "income" && it.status == "paid" && it.date.startsWith(year) }.sumOf { it.amount }
        val yearlyExpense = data.records.filter { it.type == "expense" && it.date.startsWith(year) }.sumOf { it.amount }
        val yearlyPending = data.records.filter { it.type == "income" && it.status == "pending" && it.date.startsWith(year) }.sumOf { it.amount }

        sb.append("YEARLY SUMMARY REPORT - $year\n")
        sb.append("Total Paid Income,Total Expense,Total Pending,Final Balance\n")
        sb.append("$yearlyIncome,$yearlyExpense,$yearlyPending,${yearlyIncome - yearlyExpense}\n\n")

        // --- 2. MONTHLY BREAKDOWN ---
        for (m in 1..12) {
            val monthStr = String.format("%s-%02d", year, m)
            val monthDate = SimpleDateFormat("yyyy-MM", Locale.US).parse(monthStr)
            val monthName = if (monthDate != null) SimpleDateFormat("MMMM", Locale.US).format(monthDate) else "Month $m"

            val monthRecords = data.records.filter { it.date.startsWith(monthStr) }

            if (monthRecords.isNotEmpty()) {
                // Changed from "===" to "---" to avoid Excel formula errors
                sb.append("------------------------------------------\n")
                sb.append("MONTHLY REPORT: $monthName $year\n")
                sb.append("------------------------------------------\n\n")

                // Table A: Pending Income
                val pendingIncome = monthRecords.filter { it.type == "income" && it.status == "pending" }
                sb.append("TABLE: PENDING INCOME\n")
                sb.append("Flat,Amount\n")
                if (pendingIncome.isEmpty()) {
                    sb.append("None,0\n")
                } else {
                    pendingIncome.forEach { sb.append("${it.flat_no},${it.amount}\n") }
                }
                sb.append("\n")

                // Table B: Total Expenses
                val expenses = monthRecords.filter { it.type == "expense" }
                sb.append("TABLE: MONTHLY EXPENSES\n")
                sb.append("Category,Amount\n")
                if (expenses.isEmpty()) {
                    sb.append("None,0\n")
                } else {
                    expenses.forEach { sb.append("${it.expense_name},${it.amount}\n") }
                }
                sb.append("\n")

                // Table C: Monthly Summary Table
                val mIncome = monthRecords.filter { it.type == "income" && it.status == "paid" }.sumOf { it.amount }
                val mExpense = expenses.sumOf { it.amount }
                val mPending = pendingIncome.sumOf { it.amount }

                sb.append("TABLE: MONTH SUMMARY\n")
                sb.append("Total Income (Paid),Total Expense,Total Pending\n")
                sb.append("$mIncome,$mExpense,$mPending\n\n")
            }
        }

        file.writeText(sb.toString())
        return file
    }
}