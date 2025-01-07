package com.example.alkoalert

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.example.alkoalert.ui.theme.AlkoAlertTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val selectedTab = remember { mutableStateOf("Aktualne") }
    val searchQuery = remember { mutableStateOf("") }
    val offers = remember { mutableStateListOf<Offer>() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val firebaseDatabase = FirebaseDatabase.getInstance(
            "your-firebase-link")
        val databaseReference = firebaseDatabase.getReference("offers")
        fetchOffersFromFirebase(databaseReference, context) { fetchedOffers ->
            offers.clear()
            offers.addAll(fetchedOffers)
        }
    }

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
                ShopColumn(offers = offers, tab = selectedTab.value,
                    navController = navController, context = context)
            }
        }
    )
}

@Composable
fun ShopColumn(offers: List<Offer>, tab: String, navController: NavHostController, context: Context) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 1.dp, vertical = 1.dp)
            .background(
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            )
    ) {
        offers.forEachIndexed { index, offer ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clickable {
                        if (offer.storage_path.isNotEmpty()) {
                            val encodedFilePath = Uri.encode(offer.storage_path)
                            navController.navigate("image/$encodedFilePath")
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
                    LoadImageFromStorage(storagePath = offer.storage_path)

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = offer.shop,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${offer.type}, ${offer.date}",
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
fun LoadImageFromStorage(storagePath: String) {
    val imageUri = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(storagePath) {
        val storageRef: StorageReference = FirebaseStorage.getInstance().reference.child(storagePath)
        storageRef.downloadUrl.addOnSuccessListener {
            imageUri.value = it.toString()
        }.addOnFailureListener {
            Log.e("LoadImage", "Error loading image from Firebase Storage", it)
        }
    }

    imageUri.value?.let {
        Image(painter = rememberImagePainter(it), contentDescription = "Shop Icon", modifier = Modifier.size(48.dp))
    } ?: run {
        Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary)) {
        }
    }
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
            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        )
    }
}

fun fetchOffersFromFirebase(
    databaseReference: DatabaseReference,
    context: Context,
    callback: (List<Offer>) -> Unit
) {
    databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val offersList = mutableListOf<Offer>()

            snapshot.children.forEach { child ->
                val offer = child.getValue(Offer::class.java)
                if (offer != null) {
                    offersList.add(offer)
                }
            }

            if (offersList.isNotEmpty()) {
                callback(offersList) // Pass the fetched data to the callback
            } else {
                Toast.makeText(context, "No offers found in Firebase", Toast.LENGTH_LONG).show()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(context, "Failed to fetch data from Firebase", Toast.LENGTH_LONG).show()
        }
    })
}
