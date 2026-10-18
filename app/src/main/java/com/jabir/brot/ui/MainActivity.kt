package com.jabir.brot.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jabir.brot.ReelCountApp
import com.jabir.brot.ui.navigation.MainNavigation
import com.jabir.brot.ui.theme.ReelCountTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val userPrefs = ReelCountApp.instance.userPreferencesRepository
            val darkThemeSetting by userPrefs.darkTheme.collectAsState(initial = null)
            val pureDarkSetting by userPrefs.pureDarkTheme.collectAsState(initial = true)
            val isDark = darkThemeSetting ?: isSystemInDarkTheme()

            ReelCountTheme(darkTheme = isDark, pureDark = pureDarkSetting) {
                MainNavigation()
            }
        }
    }
}
