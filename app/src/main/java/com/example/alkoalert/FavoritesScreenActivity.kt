package com.example.alkoalert

import android.net.Uri
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavHostController,
    favoritesJson: String,
    firebaseDatabaseManager: FirebaseDatabaseManager
) {
    BackHandler {
        navController.popBackStack()
    }
    val context = LocalContext.current
    val imageUriCache = remember { mutableMapOf<String, String>() }
    val decodedFavoritesJson = URLDecoder.decode(favoritesJson, StandardCharsets.UTF_8.toString())
    val type = object : TypeToken<MutableList<Offer>>() {}.type
    val originalFavoritesList: MutableList<Offer> = Gson().fromJson(decodedFavoritesJson, type)
    val favoriteOffersList = remember { mutableStateOf<List<Offer>>(emptyList()) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val route by remember { mutableStateOf("Favorites") }
    val favoritesJsonGson = Gson().toJson(favoriteOffersList)
    val encodedJson = URLEncoder.encode(favoritesJsonGson, StandardCharsets.UTF_8.toString())
    var isValidationComplete by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        val validatedList = mutableListOf<Offer>()
        var completedChecks = 0
        val totalOffers = originalFavoritesList.size

        originalFavoritesList.forEach { offer ->
            firebaseDatabaseManager.checkImageExists(offer.storage_path) { exists ->
                if (exists) {
                    validatedList.add(offer)
                }
                completedChecks++
                if (completedChecks == totalOffers) {
                    favoriteOffersList.value = validatedList
                    isValidationComplete = true
                    Log.d("FavoritesScreen", "Updated favoriteOffersList: ${favoriteOffersList.value}")
                }
            }
        }
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
        },content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = "Polubione oferty") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        },
                    )
                },
                content = { innerPadding ->
                    if (isValidationComplete) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
                        ) {
                            items(favoriteOffersList.value) { offer ->
                                Log.d("Oferty", favoriteOffersList.toString())
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                        .clickable {
                                            if (offer.storage_path.isNotEmpty()) {
                                                val encodedFilePath = Uri.encode(offer.storage_path)
                                                navController.navigate("image/$encodedFilePath?tab=favorites")
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Invalid image path",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        firebaseDatabaseManager.LoadImageFromStorage(
                                            storagePath = offer.storage_path,
                                            imageUriCache = imageUriCache
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column {
                                            Text(
                                                text = offer.shop.replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = "${offer.type.replaceFirstChar { it.uppercase() }}," +
                                                        " ${
                                                            offer.date
                                                                .replace(" od ", "od")
                                                                .replace(" do ", "do")
                                                                .replace(" ", ".")
                                                                .replace("od.", "od ")
                                                                .replace("do", " do ")
                                                        }",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    )
}

