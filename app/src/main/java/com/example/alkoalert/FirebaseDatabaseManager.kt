package com.example.alkoalert

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class FirebaseDatabaseManager {
    fun fetchOffersFromFirebase(
        databaseReference: DatabaseReference,
        context: Context,
        callback: (List<Offer>) -> Unit
    ) {
        if (offerCache.isNotEmpty()) {
            return
        }
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("Storage Ref", "storageref")
                val offersList = mutableListOf<Offer>()

                snapshot.children.forEachIndexed { index, child ->
                    try {
                        val offer = child.getValue(Offer::class.java)
                        Log.d("FirebaseDebug", "Parsed Offer: $offer")
                        if (offer != null) {
                            offersList.add(offer)
                            offerCache[index] = offer
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseDebug", "Error parsing offer at index $index: ${child.value}", e)
                    }
                }

                if (offersList.isNotEmpty()) {
                    callback(offersList)
                } else {
                    Toast.makeText(context, "No offers found in Firebase", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to fetch data from Firebase", Toast.LENGTH_LONG).show()
            }
        })
    }
    @Composable
    fun LoadImageFromStorage(
        storagePath: String, imageUriCache: MutableMap<String, String>
    ) {
        val imageUri = remember { mutableStateOf(imageUriCache[storagePath]) }

        LaunchedEffect(storagePath) {
            val storageRef = FirebaseStorage.getInstance().reference.child(storagePath)
            storageRef.downloadUrl
                .addOnSuccessListener { uri ->
                    imageUri.value = uri.toString()
                    imageUriCache[storagePath] = uri.toString()
                }
                .addOnFailureListener { e ->
                    Log.e("LoadImage", "Error loading image from Firebase Storage", e)
                }
        }
        Box(modifier = Modifier.size(56.dp)) {
            imageUri.value?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current).data(data = uri).apply(block = fun ImageRequest.Builder.() {
                            crossfade(false)
                            memoryCachePolicy(CachePolicy.ENABLED)
                            diskCachePolicy(CachePolicy.ENABLED)
                        }).build()
                    ),
                    contentDescription = "Shop Icon",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }

    companion object {
        var offerCache = mutableMapOf<Int, Offer>()
    }
}
