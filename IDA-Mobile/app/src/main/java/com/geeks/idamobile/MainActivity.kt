package com.geeks.idamobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.geeks.idamobile.ui.IdaMobileApp
import com.geeks.idamobile.viewmodels.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IdaMobileApp(
                uiState = viewModel.uiState,
                onRefreshNativeBridge = viewModel::refreshNativeBridge
            )
        }
    }
}

