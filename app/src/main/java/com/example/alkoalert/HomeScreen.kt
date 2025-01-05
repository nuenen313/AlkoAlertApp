package com.example.alkoalert

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.alkoalert.ui.theme.AlkoAlertTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, filePath : String) {
    val selectedTab = remember { mutableStateOf("Aktualne") }
    val searchQuery = remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "AlkoAlert") },
                actions = {
                    IconButton(onClick = { /* TODO: Add location action */ }) {
                        Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Location",
                            tint = if (isSystemInDarkTheme())
                                colorResource(id = R.color.tertiary_light)
                            else
                                colorResource(id = R.color.tertiary_dark))
                    }
                    SearchBar(searchQuery)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: FAB Action */ }) {
                Icon(imageVector = Icons.Filled.ShoppingCart, contentDescription = "Shopping List",
                    tint = if (isSystemInDarkTheme())
                        colorResource(id = R.color.tertiary_light)
                    else
                        colorResource(id = R.color.tertiary_dark))
            }
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TabSwitcher(selectedTab = selectedTab)
                ShopColumn(tab = selectedTab.value, navController = navController, filePath = filePath)
            }
        }
    )
}

@Composable
fun SearchBar(searchQuery: MutableState<String>) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        BasicTextField(
            value = searchQuery.value,
            onValueChange = { searchQuery.value = it },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { /*TODO: Handle search action here */ }
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        modifier = Modifier.padding(end = 8.dp),
                        tint = if (isSystemInDarkTheme())
                            colorResource(id = R.color.tertiary_light)
                        else
                            colorResource(id = R.color.tertiary_dark)
                    )
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun TabSwitcher(selectedTab: MutableState<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabButton(
            selectedTab = selectedTab,
            tab = "Aktualne",
            icon = Icons.Filled.ShoppingCart
        )
        TabButton(
            selectedTab = selectedTab,
            tab = "Nadchodzące",
            icon = Icons.Outlined.DateRange
        )
    }
}

@Composable
fun TabButton(selectedTab: MutableState<String>, tab: String, icon: ImageVector) {
    val selectedColor = if (selectedTab.value == tab) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    Button(
        onClick = { selectedTab.value = tab },
        modifier = Modifier
            .padding(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = selectedColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tab,
            modifier = Modifier.padding(end = 4.dp),
            tint = if (isSystemInDarkTheme())
                colorResource(id = R.color.tertiary_light)
            else
                colorResource(id = R.color.tertiary_dark)
        )
        Text(
            text = tab,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selectedTab.value == tab) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ShopColumn(tab: String, navController: NavHostController, filePath: String) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 1.dp, vertical = 8.dp)
            .background(
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            )
    ) {
        repeat(10) { index ->
            val shopName = "Shop ${index + 1}"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clickable {
                        if (index == 0) {
                            navController.navigate("image/${filePath}")
                        } else {
                            Log.d("ShopColumn", "Clicked on $shopName - No ImageActivity for now.")
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        // TODO: Placeholder for Shop Icon
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = shopName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Category: $tab",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun PreviewHomeScreen() {
    AlkoAlertTheme {
        val navController = rememberNavController()
        HomeScreen(navController = navController, filePath = "image")
    }
}
