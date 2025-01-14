package com.example.alkoalert

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavHostController, favoritesJson: String,
                    firebaseDatabaseManager: FirebaseDatabaseManager) {
    BackHandler {
        navController.popBackStack()
    }
    val context = LocalContext.current
    val imageUriCache = remember { mutableStateMapOf<String, String>() }
    val decodedFavoritesJson = URLDecoder.decode(favoritesJson, StandardCharsets.UTF_8.toString())
    val type = object : TypeToken<List<Offer>>() {}.type
    val favoriteOffers: List<Offer> = Gson().fromJson(decodedFavoritesJson, type)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Zapisane oferty") }
            )
        },
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(favoriteOffers) { offer ->
                    OfferCard(
                        offer = offer,
                        navController = navController,
                        firebaseDatabaseManager = firebaseDatabaseManager,
                        context = context,
                        imageUriCache = imageUriCache
                    )
                }
            }
        }
    )
}

@Composable
fun OfferCard(
    offer: Offer,
    navController: NavHostController,
    firebaseDatabaseManager: FirebaseDatabaseManager,
    context: Context,
    imageUriCache: MutableMap<String, String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clickable {
                if (offer.storage_path.isNotEmpty()) {
                    val encodedFilePath = Uri.encode(offer.storage_path)
                    val selectedTab = "favorites"
                    navController.navigate("image/$encodedFilePath?tab=${selectedTab}")
                } else {
                    Toast.makeText(context, "Invalid image path", Toast.LENGTH_SHORT).show()
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            firebaseDatabaseManager.LoadImageFromStorage(storagePath = offer.storage_path,
                imageUriCache = imageUriCache)

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = offer.shop.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${offer.type.replaceFirstChar { it.uppercase() }}," +
                            " ${offer.date.replace(" od ", "od").replace(" do ", "do")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
