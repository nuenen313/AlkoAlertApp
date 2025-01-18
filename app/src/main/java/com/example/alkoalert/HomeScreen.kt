package com.example.alkoalert

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.google.gson.Gson
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.database.FirebaseDatabase
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, initialTab: String = "Aktualne") {
    val selectedTab = remember { mutableStateOf(initialTab) }
    val searchQuery = remember { mutableStateOf("") }
    val offers = remember { mutableStateListOf<Offer>() }
    val context = LocalContext.current
    val firebaseAuthManager = FirebaseAuthManager()
    val firebaseDatabaseManager = FirebaseDatabaseManager()
    val favoriteOffers = remember { mutableStateListOf<Offer>() }

    LaunchedEffect(Unit) {
        favoriteOffers.addAll(loadFavorites(context))
        firebaseAuthManager.signInAnonymously { isAuthenticated ->
            if (isAuthenticated) {
                Log.d("Auth", "User signed in successfully")
            } else {
                Log.e("Auth", "Failed to sign in user")
            }
        }
        if (FirebaseDatabaseManager.offerCache.isNotEmpty()) {
            Log.e("CACHE","Cache not empty")
            offers.clear()
            offers.addAll(FirebaseDatabaseManager.offerCache.values)
        } else {
            Log.e("CACHE", "Cache empty")
            val firebaseDatabase = FirebaseDatabase.getInstance(
                "your-firebase-url"
            )
            firebaseDatabase.setPersistenceEnabled(true)
            val databaseReference = firebaseDatabase.getReference("offers")
            firebaseDatabaseManager.fetchOffersFromFirebase(databaseReference, context)
            { fetchedOffers ->
                val iterator = favoriteOffers.iterator()
                while (iterator.hasNext()) {
                    val favoriteOffer = iterator.next()
                    val isValid = fetchedOffers.any { newOffer ->
                        newOffer.shop == favoriteOffer.shop &&
                                newOffer.date == favoriteOffer.date &&
                                newOffer.storage_path == favoriteOffer.storage_path &&
                                newOffer.type == favoriteOffer.type
                    }
                    if (!isValid) {
                        iterator.remove()
                    }
                }
                offers.clear()
                offers.addAll(fetchedOffers)
                favoriteOffers.addAll(loadFavorites(context))
            }
        }
    }

    BackHandler {
        saveFavorites(context, favoriteOffers)
        exitProcess(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "AlkoAlert") },
                actions = {
                    IconButton(onClick = { /* TODO: Add location action */ }) {
                        Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Location")
                    }
                    SearchBar(searchQuery)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val favoritesJson = Gson().toJson(favoriteOffers)
                val encodedJson = URLEncoder.encode(favoritesJson, StandardCharsets.UTF_8.toString())
                saveFavorites(context, favoriteOffers)
                navController.navigate("favorites/$encodedJson")
            }) {
                Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorites")
            }
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TabSwitcher(selectedTab = selectedTab)
                ShopColumn(
                    offers = offers,
                    tab = selectedTab.value,
                    navController = navController,
                    context = context,
                    selectedTab = selectedTab,
                    firebaseDatabaseManager = firebaseDatabaseManager,
                    favoriteOffers = favoriteOffers
                )
            }
        }
    )
}

@Composable
fun ShopColumn(offers: List<Offer>, tab: String, navController: NavHostController, context: Context,
               selectedTab: MutableState<String>, firebaseDatabaseManager: FirebaseDatabaseManager,
               favoriteOffers: SnapshotStateList<Offer>) {
    val scrollState = rememberScrollState()
    val currentDate = LocalDate.now()
    val formattedOffers = offers.map { offer ->
        offer.copy(date = offer.date.replace("-", " "))
    }
    val imageCacheAktualne = remember { mutableMapOf<String, String>() }
    val imageCacheNadchodzace = remember { mutableMapOf<String, String>() }

    val categorizedOffers = remember(formattedOffers) {
        val aktualne = mutableListOf<Offer>()
        val nadchodzace = mutableListOf<Offer>()

        for (offer in formattedOffers) {
            val dateRange = offer.date
                .replace("od", "")
                .replace("do", "")
                .trim()
                .split(" ")
            if (dateRange.size >= 2) {
                val startDay = dateRange[0]
                val startMonth = dateRange[1]

                val formattedStartDate = "2025-${startMonth
                    .padStart(2, '0')}-${startDay.padStart(2, '0')}"
                val startDate = try {
                    LocalDate.parse(formattedStartDate, DateTimeFormatter
                        .ofPattern("yyyy-MM-dd"))
                } catch (e: Exception) {
                    Log.e("DateParsing",
                        "Error parsing start date: $formattedStartDate", e)
                    null
                }
                if (startDate != null && startDate.isAfter(currentDate)) {
                    nadchodzace.add(offer)
                } else {
                    aktualne.add(offer)
                }
            } else {
                Log.e("DateParsing", "Invalid date range format: ${offer.date}")
            }
        }
        mapOf("Aktualne" to aktualne, "Nadchodzące" to nadchodzace)
    }

    val displayedOffers = categorizedOffers[tab] ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 1.dp, vertical = 3.dp)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
    ) {
        items(displayedOffers) { offer ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 3.dp)
                    .clickable {
                        if (offer.storage_path.isNotEmpty()) {
                            val encodedFilePath = Uri.encode(offer.storage_path)
                            navController.navigate("image/$encodedFilePath?tab=${selectedTab.value}")
                        } else {
                            Toast
                                .makeText(context, "Invalid image path", Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    if (tab == "Aktualne") {
                        firebaseDatabaseManager.LoadImageFromStorage(
                            storagePath = offer.storage_path,
                            imageUriCache = imageCacheAktualne
                        )
                    } else {
                        firebaseDatabaseManager.LoadImageFromStorage(
                            storagePath = offer.storage_path,
                            imageUriCache = imageCacheNadchodzace
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = offer.shop.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${offer.type.replaceFirstChar { it.uppercase() }}," +
                                    " ${offer.date
                                        .replace(" od ", "od")
                                        .replace(" do ", "do")
                                        .replace(" ", ".")
                                        .replace("od.", "od ")
                                        .replace("do", " do ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val isFavorite = favoriteOffers.contains(offer)
                    val icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
                    val iconColor = if (isFavorite) Color.Red else Color.Gray

                    IconButton(onClick = {
                        if (isFavorite) {
                            favoriteOffers.remove(offer)
                        } else {
                            favoriteOffers.add(offer)
                        }
                    }) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Favorite",
                            tint = iconColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(searchQuery: MutableState<String>) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.onBackground, shape = MaterialTheme.shapes.small)
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
            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        )
    }
}

fun saveFavorites(context: Context, favoriteOffers: List<Offer>) {
    val sharedPreferences = context.getSharedPreferences("AlkoAlert", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val gson = Gson()
    val json = gson.toJson(favoriteOffers)
    editor.putString("favorite_offers", json)
    editor.apply()
}

fun loadFavorites(context: Context): List<Offer> {
    val sharedPreferences = context.getSharedPreferences("AlkoAlert", Context.MODE_PRIVATE)
    val gson = Gson()
    val json = sharedPreferences.getString("favorite_offers", null)
    return if (json != null) {
        val type = object : com.google.gson.reflect.TypeToken<List<Offer>>() {}.type
        gson.fromJson(json, type)
    } else {
        emptyList()
    }
}

