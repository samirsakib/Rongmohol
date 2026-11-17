package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.PreferencesRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.AppThemeViewModel
import com.example.ui.theme.BanglaCardGameTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject
  lateinit var authRepository: AuthRepository

  @Inject
  lateinit var preferencesRepository: PreferencesRepository

  private val themeViewModel: AppThemeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val prefs by themeViewModel.preferences.collectAsStateWithLifecycle()
      val useDarkTheme = prefs.darkThemeEnabled || isSystemInDarkTheme()
      BanglaCardGameTheme(
        darkTheme = useDarkTheme,
        dynamicColor = prefs.dynamicColorEnabled,
      ) {
        AppNavigation(
          authRepository = authRepository,
          preferencesRepository = preferencesRepository,
        )
      }
    }
  }
}
