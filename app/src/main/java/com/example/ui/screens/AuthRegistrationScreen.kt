package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.RegionFilter
import com.example.data.onGoogleSignInClicked

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthRegistrationScreen(
  isGoogleSignedIn: Boolean,
  initialName: String,
  initialPhone: String,
  isSavingProfile: Boolean,
  externalError: String?,
  onGoogleAuthSuccess: (pendingPhone: String, pendingName: String, preferredRegion: RegionFilter) -> Unit,
  onSavePhoneRegistration: (fullName: String, phoneNumber: String, preferredRegion: RegionFilter) -> Unit,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var phoneNumber by remember(initialPhone) { mutableStateOf(initialPhone) }
  var fullName by remember(initialName) { mutableStateOf(initialName) }
  var preferredRegion by remember { mutableStateOf(RegionFilter.MELITOPOL) }
  var isAuthLoading by remember { mutableStateOf(false) }
  var errorMessage by remember(externalError) { mutableStateOf(externalError) }

  val phoneDigitsCount = phoneNumber.count { it.isDigit() }
  val isPhoneValid = phoneDigitsCount >= 7

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
        .statusBarsPadding()
        .imePadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier
          .widthIn(max = 560.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = {
            if (isGoogleSignedIn) {
              onSignOut()
            } else {
              phoneNumber = ""
              errorMessage = null
            }
          },
          modifier = Modifier.size(48.dp).testTag("welcome_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Назад",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp),
          )
        }

        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(12.dp),
        ) {
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Exact headline & subtitle matching the provided screenshot
      Text(
        text = stringResource(R.string.welcome_title),
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.welcome_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Orange-outlined rounded phone field matching screenshot
      OutlinedTextField(
        value = phoneNumber,
        onValueChange = {
          phoneNumber = it.take(25)
          errorMessage = null
        },
        label = {
          Text(
            text = stringResource(R.string.phone_label),
            color = MaterialTheme.colorScheme.primary,
          )
        },
        placeholder = { Text("+7 (990) 123-45-67") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Filled.Phone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary,
          ),
        modifier = Modifier.fillMaxWidth().testTag("phone_number_input"),
      )

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = fullName,
        onValueChange = { fullName = it.take(80) },
        label = { Text(stringResource(R.string.name_label)) },
        placeholder = { Text("Например: Алексей") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("full_name_input"),
      )

      Spacer(modifier = Modifier.height(18.dp))

      Text(
        text = "Интересующая локация",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(8.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        RegionFilter.entries.forEach { region ->
          val selected = preferredRegion == region
          FilterChip(
            selected = selected,
            onClick = { preferredRegion = region },
            label = { Text(region.shortName) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            modifier = Modifier.testTag("reg_region_${region.code}"),
          )
        }
      }

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = errorMessage.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      if (!isGoogleSignedIn) {
        Button(
          onClick = {
            if (!isPhoneValid) {
              errorMessage = "Введите номер телефона (минимум 7 цифр)"
              return@Button
            }
            errorMessage = null
            isAuthLoading = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isAuthLoading = false
                onGoogleAuthSuccess(phoneNumber, fullName, preferredRegion)
              },
              onAuthError = { msg ->
                isAuthLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = { isAuthLoading = false },
            )
          },
          enabled = !isAuthLoading,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 54.dp)
              .testTag("google_sign_in_button"),
        ) {
          if (isAuthLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
          } else {
            Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Зарегистрироваться с Google",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = {
            errorMessage = null
            isAuthLoading = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isAuthLoading = false
                onGoogleAuthSuccess(phoneNumber, fullName, preferredRegion)
              },
              onAuthError = { msg ->
                isAuthLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = { isAuthLoading = false },
            )
          },
          enabled = !isAuthLoading,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("existing_google_login_button"),
        ) {
          Text("Уже есть аккаунт? Войти через Google")
        }
      } else {
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Google-аккаунт подключён. Подтвердите номер телефона для завершения регистрации.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = {
            if (!isPhoneValid) {
              errorMessage = "Введите номер телефона (минимум 7 цифр)"
              return@Button
            }
            errorMessage = null
            onSavePhoneRegistration(fullName, phoneNumber, preferredRegion)
          },
          enabled = !isSavingProfile,
          shape = RoundedCornerShape(16.dp),
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 54.dp)
              .testTag("finish_registration_button"),
        ) {
          if (isSavingProfile) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
          } else {
            Text(stringResource(R.string.save_registration))
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))
      Text(
        text =
          "Сразу после регистрации вы получите доступ к каталогу квартир в Мелитополе, Херсонской и Запорожской областях, поиску по карте и ИИ-консультанту.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
