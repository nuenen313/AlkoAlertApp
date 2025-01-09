package com.example.alkoalert

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun StarterScreenActivity(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        MyAlertDialog(navController = navController)
    }
}

@Composable
fun MyAlertDialog(navController: NavHostController) {
    val shouldShowDialog = remember { mutableStateOf(false) }

    if (shouldShowDialog.value) {
        AlertDialog(
            onDismissRequest = {
                shouldShowDialog.value = false
            },
            title = {
                Text(text = stringResource(id = R.string.alert_dialog_title))
            },
            text = {
                Text(text = stringResource(id = R.string.alert_dialog_text))
            },
            confirmButton = {
                Button(
                    onClick = {
                        shouldShowDialog.value = false
                        navController.navigate("home")
                    }
                ) {
                    Text(
                        text = "Potwierdzam",
                        color = Color.White
                    )
                }
            },
            /*dismissButton = {
                Button(
                    onClick = {
                        shouldShowDialog.value = false
                    }
                ) {
                    Text(
                        text = "Anuluj",
                        color = Color.White
                    )
                }
            }*/
        )
    }
}
