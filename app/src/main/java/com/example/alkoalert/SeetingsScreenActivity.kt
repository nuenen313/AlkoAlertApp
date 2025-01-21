package com.example.alkoalert

import LocalDarkMode
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController, favoritesJson: String,) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("AlkoAlert", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val darkModeState = LocalDarkMode.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val route by remember { mutableStateOf("Settings") }
    val encodedJson = URLEncoder.encode(favoritesJson, StandardCharsets.UTF_8.toString())
    BackHandler {
        navController.popBackStack()
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            AppDrawer(
                route = route,
                navigateToHome = {
                    navController.navigate("home")
                },
                navigateToFavorites = {
                    navController.navigate("favorites/$encodedJson")
                },
                navigateToSettings = {
                    navController.navigate("settings/$encodedJson")
                },
                closeDrawer = { scope.launch { drawerState.close() } }
            )
        }, content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = "Ustawienia") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        },
                    )
                },
                content = { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        Text(
                            text = "Tryb nocny",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )

                        Switch(
                            checked = darkModeState.value,
                            onCheckedChange = { isChecked ->
                                darkModeState.value = isChecked
                                editor.putBoolean("night_mode", isChecked)
                                editor.apply()
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            )
        }
    )
}