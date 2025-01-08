package com.example.alkoalert

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

@Composable
fun ImageActivity(navController: NavHostController, filePath: String) {

    val storageRef = Firebase.storage.reference

    var imageUrl by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filePath) {
        storageRef.child(filePath).downloadUrl.addOnSuccessListener { uri ->
            imageUrl = uri.toString()
            Log.d("Firebase", "Image loaded successfully: $uri")
        }.addOnFailureListener { exception ->
            errorMessage = exception.message
            Log.e("Firebase", "Failed to load image: ${exception.message}")
        }
    }

    Box(modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center) {
        when {
            imageUrl != null -> {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Firebase Image",
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .aspectRatio(1f, matchHeightConstraintsFirst = false),
                    contentScale = ContentScale.Fit
                )
            }
            errorMessage != null -> {
                Text(
                    text = "Error: $errorMessage",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            else -> {
                Text(
                    text = "Ładowanie...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}