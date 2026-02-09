package com.example.communitymaintenance

import android.content.Context
import com.google.gson.GsonBuilder
import java.io.File

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
}