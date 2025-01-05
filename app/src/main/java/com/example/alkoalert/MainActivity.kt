package com.example.alkoalert

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
                    val filePath = "wino_1.png"
                    val navController = rememberNavController()
                    AppNavigation(navController, filePath)
                }
            }
        }
    }
}


@Composable
fun AppNavigation(navController: NavHostController, filePath : String) {
    NavHost(navController = navController, startDestination = "start") {
        composable("start"){ StarterScreenActivity(navController = navController, filePath)}
        composable("image/{filepath}") { ImageActivity(filePath) }
        composable("home/{filepath}") { HomeScreen(navController = navController, filePath) }
    }
}