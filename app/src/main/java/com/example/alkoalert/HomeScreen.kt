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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, initialTab: String = "Aktualne") {
    val selectedTab = remember { mutableStateOf(initialTab) }
    val offers = remember { mutableStateListOf<Offer>() }
    val context = LocalContext.current
    val firebaseAuthManager = FirebaseAuthManager()
    val firebaseDatabaseManager = FirebaseDatabaseManager()
    val favoriteOffers = remember { mutableStateListOf<Offer>() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var route by remember { mutableStateOf("Home") }

    LaunchedEffect(Unit) {
        firebaseAuthManager.signInAnonymously { isAuthenticated ->
            if (isAuthenticated) {
                Log.d("Auth", "User signed in successfully")
            } else {
                Log.e("Auth", "Failed to sign in user")
            }
        }
        if (FirebaseDatabaseManager.offerCache.isNotEmpty()) {
            Log.e("CACHE", "Cache not empty")
            offers.clear()
            offers.addAll(FirebaseDatabaseManager.offerCache.values)
        } else {
            Log.e("CACHE", "Cache empty")
            val firebaseDatabase = FirebaseDatabase.getInstance(
                "your-firebase-url"
            )
            firebaseDatabase.setPersistenceEnabled(true)
            val databaseReference = firebaseDatabase.getReference("offers")
            firebaseDatabaseManager.fetchOffersFromFirebase(databaseReference, context) { fetchedOffers ->
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
                    val favoritesJson = Gson().toJson(favoriteOffers)
                    val encodedJson = URLEncoder.encode(favoritesJson, StandardCharsets.UTF_8.toString())
                    navController.navigate("favorites/$encodedJson")
                },
//                navigateToSettings ={},
                closeDrawer = { scope.launch { drawerState.close() } }
            )
        },
        content = {
            Log.d("Favorites", "Nav Drawer")
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = "AlkoAlert") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        },
                    )
                },
                content = { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        TabSwitcher(selectedTab = selectedTab, favoriteOffers = favoriteOffers,
                            context = context)
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
    )
}

@Composable
fun ShopColumn(offers: List<Offer>, tab: String, navController: NavHostController, context: Context,
               selectedTab: MutableState<String>, firebaseDatabaseManager: FirebaseDatabaseManager,
               favoriteOffers: SnapshotStateList<Offer>) {
    LaunchedEffect(Unit) {
        favoriteOffers.addAll(loadFavorites(context))
    }
    val currentDate = LocalDate.now()
    val formattedOffers = offers.map { offer ->
        offer.copy(date = offer.date.replace("-", " "))
    }
    val imageCacheAktualne = remember { mutableMapOf<String, String>() }
    val imageCacheNadchodzace = remember { mutableMapOf<String, String>() }
    val lazyListState = rememberLazyListState()

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
    val iterator = favoriteOffers.iterator()
    while (iterator.hasNext()) {
        val favoriteOffer = iterator.next()
        val isValid = displayedOffers.any { newOffer ->
            newOffer.shop == favoriteOffer.shop &&
                    newOffer.date == favoriteOffer.date &&
                    newOffer.storage_path == favoriteOffer.storage_path &&
                    newOffer.type == favoriteOffer.type
        }
        if (!isValid) {
            iterator.remove()
        }
    }
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
            .padding(horizontal = 1.dp, vertical = 3.dp)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
        ) {
            items(displayedOffers) { offer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                        .clickable {
                            if (offer.storage_path.isNotEmpty()) {
                                val encodedFilePath = Uri.encode(offer.storage_path)
                                navController.navigate(
                                    "image/$encodedFilePath?tab=${selectedTab.value}")
                            } else {
                                Toast.makeText(context,
                                    "Invalid image path", Toast.LENGTH_SHORT).show()
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor =
                    MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(4.dp)
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
                                saveFavorites(context, favoriteOffers)
                            } else {
                                favoriteOffers.add(offer)
                                saveFavorites(context, favoriteOffers)
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
fun TabSwitcher(selectedTab: MutableState<String>, favoriteOffers: SnapshotStateList<Offer>,
                context: Context) {
    Log.d("Favorites", "Tab Switcher")
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
            icon = Icons.Filled.ShoppingCart,
            favoriteOffers = favoriteOffers,
            context = context
        )
        TabButton(
            selectedTab = selectedTab,
            tab = "Nadchodzące",
            icon = Icons.Outlined.DateRange,
            favoriteOffers = favoriteOffers,
            context = context
        )
    }
}

@Composable
fun TabButton(selectedTab: MutableState<String>, tab: String, icon: ImageVector,
              favoriteOffers: SnapshotStateList<Offer>, context: Context) {
    val selectedColor = if (selectedTab.value == tab) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }
    Log.d("Favorites", "Tab Button")

    Button(
        onClick = {
            favoriteOffers.addAll(loadFavorites(context))
            selectedTab.value = tab },
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
    Log.d("Favorites", "Saved favorites: $json")
    editor.putString("favorite_offers", json)
    editor.apply()
}

fun loadFavorites(context: Context): List<Offer> {
    val sharedPreferences = context.getSharedPreferences("AlkoAlert", Context.MODE_PRIVATE)
    val gson = Gson()
    val json = sharedPreferences.getString("favorite_offers", null)
    Log.d("Favorites", "Loaded favorites: $json")
    return if (json != null) {
        val type = object : com.google.gson.reflect.TypeToken<List<Offer>>() {}.type
        gson.fromJson(json, type)
    } else {
        emptyList()
    }
}

@Composable
fun AppDrawer(
    route: String,
    modifier: Modifier = Modifier,
    navigateToHome: () -> Unit,
    navigateToFavorites: () -> Unit,
//    navigateToSettings: () -> Unit,
    closeDrawer: () -> Unit
) {
    ModalDrawerSheet(modifier = modifier) {
        DrawerHeader()
        Spacer(modifier = Modifier.padding(5.dp))
        NavigationDrawerItem(
            label = { Text(text = "Oferty", style = MaterialTheme.typography.bodyLarge) },
            selected = route == "Home",
            onClick = {
                navigateToHome()
                closeDrawer()
            },
            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
            shape = MaterialTheme.shapes.small
        )
        NavigationDrawerItem(
            label = { Text(text = "Polubione oferty", style = MaterialTheme.typography.bodyLarge) },
            selected = route == "Favorites",
            onClick = {
                navigateToFavorites()
                closeDrawer()
            },
            icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = null) },
            shape = MaterialTheme.shapes.small
        )
        NavigationDrawerItem(
            label = { Text(text = "Ustawienia", style = MaterialTheme.typography.bodyLarge) },
            selected = route == "Settings",
            onClick = {
//                TODO:navigateToSettings()
                closeDrawer()
            },
            icon = { Icon(imageVector = Icons.Filled.Settings, contentDescription = null) },
            shape = MaterialTheme.shapes.small
        )
    }
}

@Composable
fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "AlkoAlert",
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Menu")
    }
}