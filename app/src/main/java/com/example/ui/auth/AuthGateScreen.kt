package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R

@Composable
fun AuthGateScreen(
  onAuthSuccess: () -> Unit,
  onEnterGuestMode: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isCheckingAutoSignIn by remember { mutableStateOf(true) }
  var isSigningIn by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var guestName by rememberSaveable { mutableStateOf("অতিথি খেলোয়াড়") }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isCheckingAutoSignIn = false
        onAuthSuccess()
      },
      onUnauthenticated = {
        isCheckingAutoSignIn = false
      },
      scope = coroutineScope,
    )
  }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
              MaterialTheme.colorScheme.background,
              MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
            )
          )
        )
        .safeDrawingPadding()
        .padding(20.dp),
      contentAlignment = Alignment.Center,
    ) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 640.dp
        val startGoogleFlow = {
          errorMessage = null
          isSigningIn = true
          onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
              isSigningIn = false
              onAuthSuccess()
            },
            onAuthError = { msg ->
              isSigningIn = false
              errorMessage = msg
            },
            scope = coroutineScope,
            onAuthCancelled = { isSigningIn = false },
          )
        }

        if (isWide) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .widthIn(max = 980.dp)
              .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            AuthHeroArtwork(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight(0.75f),
            )
            AuthActionCard(
              isCheckingAutoSignIn = isCheckingAutoSignIn,
              isSigningIn = isSigningIn,
              errorMessage = errorMessage,
              guestName = guestName,
              onGuestNameChange = { guestName = it },
              onSignInClick = startGoogleFlow,
              onGuestClick = { onEnterGuestMode(guestName) },
              modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            )
          }
        } else {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .widthIn(max = 520.dp)
              .align(Alignment.Center)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            AuthHeroArtwork(
              modifier = Modifier
                .fillMaxWidth()
                .height(195.dp),
            )
            Spacer(modifier = Modifier.height(18.dp))
            AuthActionCard(
              isCheckingAutoSignIn = isCheckingAutoSignIn,
              isSigningIn = isSigningIn,
              errorMessage = errorMessage,
              guestName = guestName,
              onGuestNameChange = { guestName = it },
              onSignInClick = startGoogleFlow,
              onGuestClick = { onEnterGuestMode(guestName) },
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AuthHeroArtwork(modifier: Modifier = Modifier) {
  Image(
    painter = painterResource(id = R.drawable.img_lobby_hero),
    contentDescription = stringResource(R.string.home_hero_desc),
    contentScale = ContentScale.Crop,
    modifier = modifier.clip(MaterialTheme.shapes.large),
  )
}

@Composable
private fun AuthActionCard(
  isCheckingAutoSignIn: Boolean,
  isSigningIn: Boolean,
  errorMessage: String?,
  guestName: String,
  onGuestNameChange: (String) -> Unit,
  onSignInClick: () -> Unit,
  onGuestClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
      ) {
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }

      Text(
        text = stringResource(R.string.auth_headline),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )

      Text(
        text = stringResource(R.string.auth_subheadline),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      if (errorMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      if (isCheckingAutoSignIn) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.padding(vertical = 8.dp),
        ) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          Text(
            text = stringResource(R.string.auth_checking_session),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        Button(
          onClick = onSignInClick,
          enabled = !isSigningIn,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("google_sign_in_button"),
        ) {
          if (isSigningIn) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Login,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
              text = stringResource(R.string.auth_google_button),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          HorizontalDivider(modifier = Modifier.weight(1f))
          Text(
            text = stringResource(R.string.auth_guest_divider),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          HorizontalDivider(modifier = Modifier.weight(1f))
        }

        OutlinedTextField(
          value = guestName,
          onValueChange = onGuestNameChange,
          label = { Text(stringResource(R.string.auth_guest_name_label)) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("guest_name_input"),
        )

        OutlinedButton(
          onClick = onGuestClick,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("guest_login_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.PersonOutline,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            text = stringResource(R.string.auth_guest_button),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}
