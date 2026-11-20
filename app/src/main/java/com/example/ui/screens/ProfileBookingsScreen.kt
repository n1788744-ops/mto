package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import com.example.data.FavoriteApartment
import com.example.data.PaymentBooking
import com.example.data.RegionFilter
import com.example.data.UserProfile
import com.example.data.YOO_MONEY_WALLET
import com.example.data.formatRub
import com.example.ui.components.openYooMoneyInBrowser
import com.example.ui.viewmodel.DataUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileBookingsScreen(
  profile: UserProfile,
  favoritesState: DataUiState<List<FavoriteApartment>>,
  bookingsState: DataUiState<List<PaymentBooking>>,
  isSavingProfile: Boolean,
  onSaveProfile: (fullName: String, phoneNumber: String, preferredRegion: RegionFilter) -> Unit,
  onOpenApartment: (Apartment) -> Unit,
  onConfirmPayment: (bookingId: String) -> Unit,
  onCancelBooking: (bookingId: String) -> Unit,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current

  var fullName by remember(profile.fullName) { mutableStateOf(profile.fullName) }
  var phoneNumber by remember(profile.phoneNumber) { mutableStateOf(profile.phoneNumber) }
  var preferredRegion by remember(profile.preferredRegion) {
    mutableStateOf(RegionFilter.fromCode(profile.preferredRegion))
  }

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

    // Direct YooMoney Card
    item {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Filled.AccountBalanceWallet,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Быстрая оплата ЮMoney",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
          Text(
            text = "Кошелёк: $YOO_MONEY_WALLET\nСайт: https://yoomoney.ru",
            style = MaterialTheme.typography.bodyMedium,
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
              Text("Платить")
            }
          }
        }
      }
    }

    // Bookings section
    item {
      Text(
        text = "Заявки и оплаты ЮMoney",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    when (bookingsState) {
      is DataUiState.Loading -> {
        item { CircularProgressIndicator() }
      }
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
            val statusLabel =
              when (booking.status) {
                "confirmed" -> "Оплачено"
                "cancelled" -> "Отменена"
                else -> "Ожидает оплаты"
              }
            val statusContainer =
              when (booking.status) {
                "confirmed" -> MaterialTheme.colorScheme.tertiaryContainer
                "cancelled" -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.primaryContainer
              }
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
                Text(
                  text = "ЮMoney: ${booking.yoomoneyWallet}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!booking.comment.isNullOrBlank()) {
                  Text(
                    text = "Комментарий: ${booking.comment}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (booking.status == "pending_payment") {
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                      onClick = { openYooMoneyInBrowser(context) },
                      modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                      Text("Перейти в ЮMoney")
                    }
                    OutlinedButton(
                      onClick = { onConfirmPayment(booking.id) },
                      modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                      Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Оплачено")
                    }
                  }
                  OutlinedButton(
                    onClick = { onCancelBooking(booking.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                  ) {
                    Text("Отменить заявку")
                  }
                }
              }
            }
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
