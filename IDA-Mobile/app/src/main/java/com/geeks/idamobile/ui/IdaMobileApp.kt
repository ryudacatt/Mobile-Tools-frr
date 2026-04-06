package com.geeks.idamobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geeks.idamobile.viewmodels.MainUiState

@Composable
fun IdaMobileApp(
    uiState: MainUiState,
    onRefreshNativeBridge: () -> Unit
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "IDA-Mobile Foundation",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                    text = uiState.nativeStatus,
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = onRefreshNativeBridge) {
                    Text("Recheck JNI")
                }
            }
        }
    }
}

