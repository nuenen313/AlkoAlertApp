package com.example.alkoalert

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.alkoalert.ui.theme.AlkoAlertTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlkoAlertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController)
                }
            }
        }
    }
}


@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "start") {
        composable("start"){ StarterScreenActivity(navController = navController)}
        composable("image/{encodedFilePath}") { backStackEntry ->
            val encodedFilePath = backStackEntry.arguments?.getString("encodedFilePath")
            val filePath = Uri.decode(encodedFilePath)  // Decode the encoded file path

            if (filePath != null) {
                ImageActivity(navController = navController, filePath)
            }
        }
        composable("home") { HomeScreen(navController = navController) }
    }
}