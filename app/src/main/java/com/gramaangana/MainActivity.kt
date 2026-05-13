package com.gramaangana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.gramaangana.ui.HubDashboard
import com.gramaangana.ui.MainViewModel
import com.gramaangana.ui.theme.GramaAnganaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GramaAnganaTheme {
                HubDashboard(viewModel)
            }
        }
    }
}
