package com.deep.sideglance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.deep.sideglance.ui.navigation.SideGlanceNavHost
import com.deep.sideglance.ui.theme.SideGlanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SideGlanceTheme {
                SideGlanceNavHost()
            }
        }
    }
}