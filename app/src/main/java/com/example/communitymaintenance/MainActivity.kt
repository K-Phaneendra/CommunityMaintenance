package com.example.communitymaintenance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { HomeScreen(navController) }
                    composable("income") { IncomeScreen(navController) }
                    composable("expense") { ExpenseScreen(navController) }
                    composable("files") { FileListScreen(navController) }
                    composable("reports") { ReportsScreen(navController) }
                    composable("admin") { AdminScreen(navController) }
                }
            }
        }
    }
}