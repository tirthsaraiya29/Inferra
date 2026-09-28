package com.inferra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.inferra.ui.navigation.InferraNavHost
import com.inferra.ui.theme.InferraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InferraTheme {
                InferraNavHost()
            }
        }
    }
}
