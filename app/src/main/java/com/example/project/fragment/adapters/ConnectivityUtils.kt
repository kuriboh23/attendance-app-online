package com.example.project.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project.data.ConnectivityObserver
import com.example.project.data.NetworkConnectivityObserver
import kotlinx.coroutines.launch

fun AppCompatActivity.observeConnectivity(onStatusChanged: (ConnectivityObserver.Status) -> Unit) {
    val connectivityObserver = NetworkConnectivityObserver(this)

    lifecycleScope.launch {
        connectivityObserver.observe().collect { status ->
            onStatusChanged(status)
        }
    }
}
