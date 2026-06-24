package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.RouteDatabase
import com.example.data.RouteRepository
import com.example.ui.screens.MainMapScreen
import com.example.ui.screens.ReuseStopsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.RouteViewModel
import com.example.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Lazy initialize our local Route SQLite database
        val database = RouteDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = RouteRepository(database.stopDao())

        // Instantiate RouteViewModel via Factory
        val factory = ViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[RouteViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val activeScreen by viewModel.activeScreen.collectAsState()

                    when (activeScreen) {
                        AppScreen.MAIN -> {
                            MainMapScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppScreen.REUSE_STOPS -> {
                            ReuseStopsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
