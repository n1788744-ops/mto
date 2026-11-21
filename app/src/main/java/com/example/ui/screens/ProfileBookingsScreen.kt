package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Apartment
import com.example.data.ApartmentCatalog
import com.example.data.BookingStatus
import com.example.data.FavoriteApartment
import com.example.data.PaymentBooking
import com.example.data.RegionFilter
import com.example.data.UserProfile
import com.example.data.YOO_MONEY_OAUTH_REDIRECT_URI
import com.example.data.YOO_MONEY_WALLET
import com.example.data.YooMoneyMerchantStatus
import com.example.data.formatRub
import com.example.ui.components.openUriInBrowser
import com.example.ui.components.openYooMoneyInBrowser
import com.example.ui.viewmodel.DataUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileBookingsScreen(
  profile: UserProfile,
  favoritesState: DataUiState<List<FavoriteApartment>>,
  bookingsState: DataUiState<List<PaymentBooking>>,
  yooMoneyStatus: YooMoneyMerchantStatus,
  isConnectingMerchant: Boolean,
  verifyingBookingId: String?,
  isSavingProfile: Boolean,
  onSaveProfile: (fullName: String, phoneNumber: String, preferredRegion: RegionFilter) -> Unit,
  onOpenApartment: (Apartment) -> Unit,
  onPayBookingCheckout: (PaymentBooking) -> Unit,
  onVerifyBookingApi: (PaymentBooking) -> Unit,
  onSubmitManualOperationId: (PaymentBooking, String) -> Unit,
  onCancelBooking: (bookingId: String) -> Unit,
  onStartYooMoneyOAuth: () -> Result<Uri>,
  onConnectYooMoneyCode: (String) -> Unit,
  onDisconnectYooMoneyOAuth: () -> Unit,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current

  var fullName by remember(profile.fullName) { mutableStateOf(profile.fullName) }
  var phoneNumber by remember(profile.phoneNumber) { mutableStateOf(profile.phoneNumber) }
  var preferredRegion by remember(profile.preferredRegion) {
    mutableStateOf(RegionFilter.fromCode(profile.preferredRegion))
  }
  var showOAuthCodeField by remember { mutableStateOf(false) }
  var oauthCodeInput by remember { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Личный кабинет",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        OutlinedButton(
          onClick = onSignOut,
          modifier = Modifier.heightIn(min = 48.dp).testTag("profile_sign_out_btn"),
        ) {
          Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Выйти")
        }
      }
    }

    // Registration Profile Card
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Регистрационные данные",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it.take(25) },
            label = { Text("Номер телефона") },
            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
          )
          OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it.take(80) },
            label = { Text("Ваше имя") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            RegionFilter.entries.forEach { region ->
              FilterChip(
                selected = preferredRegion == region,
                onClick = { preferredRegion = region },
                label = { Text(region.shortName) },
              )
            }
          }
          Button(
            onClick = { onSaveProfile(fullName, phoneNumber, preferredRegion) },
            enabled = !isSavingProfile && phoneNumber.count { it.isDigit() } >= 5,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("profile_save_btn"),
            shape = RoundedCornerShape(14.dp),
          ) {
            if (isSavingProfile) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Text("Сохранить данные")
            }
          }
        }
      }
    }

    // YooMoney Wallet & Anti-Fraud OAuth2 Control Card
    item {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Filled.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "ЮMoney и защита платежей",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
            if (yooMoneyStatus.isConnected) {
              Surface(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(8.dp),
              ) {
                Text(
                  text = "API активен",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onTertiary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
          }

          Text(
            text = "Кошелёк получателя: $YOO_MONEY_WALLET (https://yoomoney.ru)\n" +
              "• Уникальный код заявки (label) на каждый платёж\n" +
              "• Блокировка возврата из браузера без зачисления\n" +
              "• Запрет повторного использования ID операции",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                clipboard.setText(AnnotatedString(YOO_MONEY_WALLET))
                Toast.makeText(context, "Счёт $YOO_MONEY_WALLET скопирован", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("profile_copy_wallet_btn"),
            ) {
              Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Копировать")
            }
            Button(
              onClick = { openYooMoneyInBrowser(context) },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("profile_pay_yoomoney_btn"),
            ) {
              Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("ЮMoney")
            }
          }

          // OAuth2 Merchant Verification section
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              if (yooMoneyStatus.isConnected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "OAuth2 API подключён: ${yooMoneyStatus.connectedWallet}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
                if (yooMoneyStatus.balanceRub != null) {
                  Text(
                    text = "Баланс кошелька: ${formatRub(yooMoneyStatus.balanceRub.toLong())} ₽",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                OutlinedButton(
                  onClick = onDisconnectYooMoneyOAuth,
                  modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                ) {
                  Text("Отключить OAuth2 токен")
                }
              } else {
                Text(
                  text = "Проверка входящих оплат через OAuth2 API ЮMoney",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text =
                    if (yooMoneyStatus.credentialsConfigured) {
                      "Redirect URI: $YOO_MONEY_OAUTH_REDIRECT_URI"
                    } else {
                      "Чтобы включить авто-проверку через OAuth2 API ЮMoney, задайте YOOMONEY_CLIENT_ID и YOOMONEY_CLIENT_SECRET в AI Studio Secrets."
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(
                    onClick = {
                      onStartYooMoneyOAuth().fold(
                        onSuccess = { authUri -> openUriInBrowser(context, authUri) },
                        onFailure = { err ->
                          Toast.makeText(
                              context,
                              err.localizedMessage ?: "Секреты ЮMoney не настроены",
                              Toast.LENGTH_LONG,
                            )
                            .show()
                        },
                      )
                    },
                    enabled = !isConnectingMerchant,
                    modifier = Modifier.weight(1f).heightIn(min = 46.dp).testTag("yoomoney_oauth_connect_btn"),
                  ) {
                    if (isConnectingMerchant) {
                      CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                      Text("Подключить OAuth2")
                    }
                  }
                  OutlinedButton(
                    onClick = { showOAuthCodeField = !showOAuthCodeField },
                    modifier = Modifier.heightIn(min = 46.dp),
                  ) {
                    Text("Код")
                  }
                }

                if (showOAuthCodeField) {
                  OutlinedTextField(
                    value = oauthCodeInput,
                    onValueChange = { oauthCodeInput = it },
                    label = { Text("Код или ссылка редиректа ЮMoney") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                  )
                  Button(
                    onClick = {
                      onConnectYooMoneyCode(oauthCodeInput)
                      oauthCodeInput = ""
                      showOAuthCodeField = false
                    },
                    enabled = oauthCodeInput.isNotBlank() && !isConnectingMerchant,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                  ) {
                    Text("Обменять код на токен")
                  }
                }
              }
            }
          }
        }
      }
    }

    // Bookings section
    item {
      Text(
        text = "Заявки и верификация ЮMoney",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    when (bookingsState) {
      is DataUiState.Loading -> item { CircularProgressIndicator() }
      is DataUiState.Error -> {
        item {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = bookingsState.message,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
      }
      is DataUiState.Success -> {
        if (bookingsState.data.isEmpty()) {
          item {
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "У вас пока нет созданных заявок на бронь или оплату. Выберите квартиру и нажмите «Платить».",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        } else {
          items(bookingsState.data, key = { it.id }) { booking ->
            BookingAntiFraudCard(
              booking = booking,
              isVerifying = verifyingBookingId == booking.id,
              onPayCheckout = { onPayBookingCheckout(booking) },
              onVerifyApi = { onVerifyBookingApi(booking) },
              onSubmitOperationId = { opId -> onSubmitManualOperationId(booking, opId) },
              onCancel = { onCancelBooking(booking.id) },
            )
          }
        }
      }
    }

    // Favorites section
    item {
      Text(
        text = "Избранные квартиры",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    when (favoritesState) {
      is DataUiState.Loading -> item { CircularProgressIndicator() }
      is DataUiState.Error -> {
        item {
          Text(
            text = favoritesState.message,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
      is DataUiState.Success -> {
        if (favoritesState.data.isEmpty()) {
          item {
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "Нажмите на сердечко у любой квартиры, чтобы сохранить её в избранное.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        } else {
          items(favoritesState.data, key = { it.id }) { fav ->
            val apt = ApartmentCatalog.listings.firstOrNull { it.id == fav.apartmentId }
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clickable(enabled = apt != null) { if (apt != null) onOpenApartment(apt) },
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  Icons.Filled.Favorite,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = fav.apartmentTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = "${formatRub(fav.priceRub)} ₽ · ${RegionFilter.fromCode(fav.region).displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun BookingAntiFraudCard(
  booking: PaymentBooking,
  isVerifying: Boolean,
  onPayCheckout: () -> Unit,
  onVerifyApi: () -> Unit,
  onSubmitOperationId: (String) -> Unit,
  onCancel: () -> Unit,
) {
  var showOpInput by remember(booking.id) { mutableStateOf(false) }
  var opIdText by remember(booking.id) { mutableStateOf(booking.submittedOperationId.orEmpty()) }

  val statusLabel =
    when (booking.status) {
      BookingStatus.VERIFIED_PAID -> "Подтверждено ЮMoney"
      BookingStatus.PENDING_VERIFICATION -> "На проверке чека"
      BookingStatus.VERIFICATION_FAILED -> "Не найдено / отказ"
      BookingStatus.CANCELLED -> "Отменена"
      else -> "Ожидает оплаты"
    }
  val statusContainer =
    when (booking.status) {
      BookingStatus.VERIFIED_PAID -> MaterialTheme.colorScheme.tertiaryContainer
      BookingStatus.PENDING_VERIFICATION -> MaterialTheme.colorScheme.secondaryContainer
      BookingStatus.VERIFICATION_FAILED -> MaterialTheme.colorScheme.errorContainer
      BookingStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
      else -> MaterialTheme.colorScheme.primaryContainer
    }
  val isEditable =
    booking.status == BookingStatus.AWAITING_PAYMENT ||
      booking.status == BookingStatus.PENDING_VERIFICATION ||
      booking.status == BookingStatus.VERIFICATION_FAILED

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${formatRub(booking.amountRub)} ₽",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        Surface(color = statusContainer, shape = RoundedCornerShape(8.dp)) {
          Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Text(
        text = booking.apartmentTitle,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
      )

      val methodLabel = if (booking.paymentMethod == "PC") "Кошелёк ЮMoney" else "Банковская карта"
      Text(
        text = "Метка заказа: ${booking.paymentLabel} · $methodLabel",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (!booking.comment.isNullOrBlank()) {
        Text(
          text = "Комментарий: ${booking.comment}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (!booking.verificationNote.isNullOrBlank()) {
        Surface(
          color =
            if (booking.status == BookingStatus.VERIFICATION_FAILED)
              MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = booking.verificationNote,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(8.dp),
          )
        }
      }

      if (booking.status == BookingStatus.VERIFIED_PAID && !booking.yoomoneyOperationId.isNullOrBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "ID операции: ${booking.yoomoneyOperationId} · Проверено: ${formatRub(booking.verifiedAmountRub ?: booking.amountRub)} ₽",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }

      if (isEditable) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onPayCheckout,
            enabled = !isVerifying,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("booking_pay_${booking.id}"),
          ) {
            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Оплатить")
          }
          OutlinedButton(
            onClick = onVerifyApi,
            enabled = !isVerifying,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("booking_verify_${booking.id}"),
          ) {
            if (isVerifying) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Проверить API")
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = { showOpInput = !showOpInput },
            enabled = !isVerifying,
            modifier = Modifier.weight(1f).heightIn(min = 46.dp),
          ) {
            Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ID чека")
          }
          OutlinedButton(
            onClick = onCancel,
            enabled = !isVerifying,
            modifier = Modifier.weight(1f).heightIn(min = 46.dp),
          ) {
            Text("Отменить")
          }
        }

        if (showOpInput) {
          OutlinedTextField(
            value = opIdText,
            onValueChange = { opIdText = it.take(64) },
            label = { Text("Номер операции ЮMoney из чека") },
            placeholder = { Text("Например: 715492819201") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("booking_op_input_${booking.id}"),
          )
          Button(
            onClick = {
              onSubmitOperationId(opIdText)
              showOpInput = false
            },
            enabled = opIdText.trim().length >= 6 && !isVerifying,
            modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
          ) {
            Text("Отправить на антифрод-проверку")
          }
        }
      }
    }
  }
}
