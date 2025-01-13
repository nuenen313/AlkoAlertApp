package com.example.alkoalert

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class FirebaseDatabaseManager {
    fun fetchOffersFromFirebase(
        databaseReference: DatabaseReference,
        context: Context,
        callback: (List<Offer>) -> Unit
    ) {
        if (offerCache.isNotEmpty()) {
            return
        }
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offersList = mutableListOf<Offer>()

                snapshot.children.forEachIndexed { index, child ->
                    val offer = child.getValue(Offer::class.java)
                    if (offer != null) {
                        offersList.add(offer)
                        offerCache[index] = offer
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
    companion object {
        var offerCache = mutableMapOf<Int, Offer>()
    }
}
