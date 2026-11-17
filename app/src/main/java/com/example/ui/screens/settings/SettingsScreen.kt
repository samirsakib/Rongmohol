package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.core.config.toBengaliDigits
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.auth.signOutUser
import com.example.ui.components.TwoPaneAdaptiveLayout

@Composable
fun SettingsScreen(
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var authError by remember { mutableStateOf<String?>(null) }
  var showPrivacyDialog by remember { mutableStateOf(false) }

  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Filled.PrivacyTip,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      },
      title = {
        Text(
          text = stringResource(R.string.settings_privacy_title),
          style = MaterialTheme.typography.titleLarge,
        )
      },
      text = {
        Text(
          text = stringResource(R.string.settings_privacy_body),
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        Button(
          onClick = { showPrivacyDialog = false },
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("privacy_dialog_close_button"),
        ) {
          Text(stringResource(R.string.settings_privacy_close))
        }
      },
    )
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 1100.dp)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = stringResource(R.string.settings_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = stringResource(R.string.settings_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      TwoPaneAdaptiveLayout(
        primaryPane = { paneModifier ->
          Column(
            modifier = paneModifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Card(
              shape = MaterialTheme.shapes.large,
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = stringResource(R.string.settings_audio_section),
                  style = MaterialTheme.typography.titleLarge,
                )
                PreferenceSwitchRow(
                  title = stringResource(R.string.settings_sound_label),
                  subtitle = stringResource(R.string.settings_sound_desc),
                  checked = uiState.preferences.soundEnabled,
                  onCheckedChange = viewModel::setSoundEnabled,
                  testTag = "switch_sound",
                )
                PreferenceSwitchRow(
                  title = stringResource(R.string.settings_haptics_label),
                  subtitle = stringResource(R.string.settings_haptics_desc),
                  checked = uiState.preferences.hapticsEnabled,
                  onCheckedChange = viewModel::setHapticsEnabled,
                  testTag = "switch_haptics",
                )
                PreferenceSwitchRow(
                  title = stringResource(R.string.settings_numerals_label),
                  subtitle = stringResource(R.string.settings_numerals_desc),
                  checked = uiState.preferences.useBengaliNumerals,
                  onCheckedChange = viewModel::setUseBengaliNumerals,
                  testTag = "switch_bengali_numerals",
                )
              }
            }

            Card(
              shape = MaterialTheme.shapes.large,
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = stringResource(R.string.settings_about_section),
                  style = MaterialTheme.typography.titleLarge,
                )
                val digits = uiState.preferences.useBengaliNumerals
                Text(
                  text = "${stringResource(R.string.settings_version_label)}: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE.toBengaliDigits(digits)})",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = BuildConfig.APPLICATION_ID,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                  onClick = { showPrivacyDialog = true },
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("settings_privacy_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.PrivacyTip,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.size(8.dp))
                  Text(
                    text = stringResource(R.string.settings_privacy_button),
                    style = MaterialTheme.typography.labelLarge,
                  )
                }
              }
            }
          }
        },
        secondaryPane = { paneModifier ->
          Column(
            modifier = paneModifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Card(
              shape = MaterialTheme.shapes.large,
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = stringResource(R.string.settings_theme_section),
                  style = MaterialTheme.typography.titleLarge,
                )
                PreferenceSwitchRow(
                  title = stringResource(R.string.settings_dark_mode_label),
                  subtitle = stringResource(R.string.settings_dark_mode_desc),
                  checked = uiState.preferences.darkThemeEnabled,
                  onCheckedChange = viewModel::setDarkThemeEnabled,
                  testTag = "switch_dark_mode",
                )
                PreferenceSwitchRow(
                  title = stringResource(R.string.settings_dynamic_color_label),
                  subtitle = stringResource(R.string.settings_dynamic_color_desc),
                  checked = uiState.preferences.dynamicColorEnabled,
                  onCheckedChange = viewModel::setDynamicColorEnabled,
                  testTag = "switch_dynamic_color",
                )
              }
            }

            Card(
              shape = MaterialTheme.shapes.large,
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = stringResource(R.string.settings_account_section),
                  style = MaterialTheme.typography.titleLarge,
                )
                val user = uiState.user
                if (user != null) {
                  Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = "${stringResource(R.string.settings_signed_in_as)}: ${user.email ?: user.uid}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  OutlinedButton(
                    onClick = {
                      viewModel.leaveGuestMode()
                      signOutUser(
                        credentialManager = credentialManager,
                        scope = coroutineScope,
                      )
                    },
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = 48.dp)
                      .testTag("sign_out_button"),
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.Logout,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                      text = stringResource(R.string.auth_sign_out),
                      style = MaterialTheme.typography.labelLarge,
                    )
                  }
                } else {
                  Text(
                    text = uiState.preferences.guestDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                  ) {
                    Text(
                      text = stringResource(R.string.auth_guest_badge),
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSecondaryContainer,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                  }
                  if (authError != null) {
                    Text(
                      text = authError!!,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.error,
                    )
                  }
                  Button(
                    onClick = {
                      authError = null
                      onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = { viewModel.leaveGuestMode() },
                        onAuthError = { authError = it },
                        scope = coroutineScope,
                      )
                    },
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = 48.dp)
                      .testTag("settings_link_google_button"),
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.Login,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                      text = stringResource(R.string.auth_link_google_button),
                      style = MaterialTheme.typography.labelLarge,
                    )
                  }
                  OutlinedButton(
                    onClick = viewModel::leaveGuestMode,
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = 48.dp)
                      .testTag("settings_switch_session_button"),
                  ) {
                    Text(
                      text = stringResource(R.string.auth_switch_session),
                      style = MaterialTheme.typography.labelLarge,
                    )
                  }
                }
              }
            }
          }
        },
      )
    }
  }
}

@Composable
private fun PreferenceSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(end = 12.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
    )
  }
}
