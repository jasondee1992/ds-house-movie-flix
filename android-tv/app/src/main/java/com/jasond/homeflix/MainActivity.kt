package com.jasond.homeflix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jasond.homeflix.ui.HomeFlixApp
import com.jasond.homeflix.ui.theme.HomeFlixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HomeFlixTheme { HomeFlixApp() } }
    }
}

