package com.example.alkoalert

import android.util.Log
import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    fun signInAnonymously(onComplete: (Boolean) -> Unit) {
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("Auth", "Anonymous sign-in successful!")
                        onComplete(true)
                    } else {
                        Log.e("Auth", "Anonymous sign-in failed.", task.exception)
                        onComplete(false)
                    }
                }
        } else {
            Log.d("Auth", "User already signed in.")
            onComplete(true)
        }
    }
}