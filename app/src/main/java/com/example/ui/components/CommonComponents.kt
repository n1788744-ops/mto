package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Apartment
import com.example.data.DealType
import com.example.data.YOO_MONEY_HOME_URL
import com.example.data.YOO_MONEY_PAY_URL
import com.example.data.YOO_MONEY_WALLET
import com.example.data.formatRub

fun openUriInBrowser(context: Context, uri: Uri) {
  val intent = Intent(Intent.ACTION_VIEW, uri)
  try {
    context.startActivity(intent)
  } catch (_: ActivityNotFoundException) {
    try {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(YOO_MONEY_HOME_URL)))
    } catch (_: Exception) {
      Toast.makeText(
          context,
          "Откройте https://yoomoney.ru и переведите на $YOO_MONEY_WALLET",
          Toast.LENGTH_LONG,
        )
        .show()
    }
  }
}

fun openYooMoneyInBrowser(context: Context) {
  openUriInBrowser(context, Uri.parse(YOO_MONEY_PAY_URL))
}

@Composable
fun ApartmentCard(
  apartment: Apartment,
  isFavorite: Boolean,
  onToggleFavorite: () -> Unit,
  onOpenDetail: () -> Unit,
  onPayClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("apartment_card_${apartment.id}")
        .clickable(onClick = onOpenDetail),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
      ) {
        val cover = apartment.photos.first()
        Image(
          painter = painterResource(id = cover.resId),
          contentDescription = apartment.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                  startY = 220f,
                )
              )
        )

        Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          Surface(
            color =
              if (apartment.dealType == DealType.RENT) MaterialTheme.colorScheme.secondary
              else MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = apartment.dealType.displayName,
              style = MaterialTheme.typography.labelMedium,
              color = Color.White,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shape = CircleShape,
          ) {
            IconButton(
              onClick = onToggleFavorite,
              modifier = Modifier.size(48.dp).testTag("favorite_btn_${apartment.id}"),
            ) {
              Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isFavorite) "Убрать из избранного" else "В избранное",
                tint =
                  if (isFavorite) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }

        Row(
          modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${formatRub(apartment.priceRub)} ${apartment.dealType.priceSuffix}",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )

          Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(8.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${apartment.photos.size} фото",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
              )
            }
          }
        }
      }

      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = apartment.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = apartment.address,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.height(10.dp))

        val roomsLabel = if (apartment.rooms == 0) "Студия" else "${apartment.rooms}-комн."
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          MetaChip(text = roomsLabel)
          MetaChip(text = "${apartment.areaSqM} м²")
          MetaChip(text = "${apartment.floor}/${apartment.totalFloors} эт.")
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onOpenDetail,
            modifier =
              Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("details_btn_${apartment.id}"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text("Подробнее")
          }
          Button(
            onClick = onPayClick,
            modifier =
              Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("pay_card_btn_${apartment.id}"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AccountBalanceWallet,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Платить")
          }
        }
      }
    }
  }
}

@Composable
fun MetaChip(text: String) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(8.dp),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YooMoneyPaymentSheet(
  apartment: Apartment,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onSubmitAndPay: (amountRub: Long, paymentMethod: String, comment: String?) -> Unit,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var amountText by remember(apartment.id) {
    mutableStateOf(apartment.depositOrBookingRub.toString())
  }
  var paymentMethod by remember(apartment.id) { mutableStateOf("AC") }
  var comment by remember(apartment.id) { mutableStateOf("") }

  val minAmount = apartment.depositOrBookingRub
  val parsedAmount = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
  val isAmountValid = parsedAmount >= minAmount

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Защищённая оплата ЮMoney",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = apartment.title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Официальный счёт ЮMoney",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = YOO_MONEY_WALLET,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              text = "https://yoomoney.ru",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
          }
          FilledTonalButton(
            onClick = {
              clipboard.setText(AnnotatedString(YOO_MONEY_WALLET))
              Toast.makeText(context, "Счёт $YOO_MONEY_WALLET скопирован", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.heightIn(min = 48.dp).testTag("copy_yoomoney_wallet_btn"),
          ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = "Копировать номер счёта", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Копировать")
          }
        }
      }

      // Payment method choice (Card AC vs YooMoney Wallet PC)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = paymentMethod == "AC",
          onClick = { paymentMethod = "AC" },
          leadingIcon = { Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp)) },
          label = { Text("Банковская карта") },
          modifier = Modifier.weight(1f).testTag("pay_method_ac"),
        )
        FilterChip(
          selected = paymentMethod == "PC",
          onClick = { paymentMethod = "PC" },
          leadingIcon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp)) },
          label = { Text("Кошелёк ЮMoney") },
          modifier = Modifier.weight(1f).testTag("pay_method_pc"),
        )
      }

      OutlinedTextField(
        value = amountText,
        onValueChange = { input -> amountText = input.filter { it.isDigit() }.take(9) },
        label = {
          Text(
            if (apartment.dealType == DealType.RENT) "Сумма аренды / залога (мин. ${formatRub(minAmount)} ₽)"
            else "Сумма задатка бронирования (мин. ${formatRub(minAmount)} ₽)"
          )
        },
        isError = !isAmountValid,
        supportingText = {
          if (!isAmountValid) {
            Text("Минимальная сумма по условиям объекта: ${formatRub(minAmount)} ₽")
          }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("yoomoney_amount_input"),
      )

      OutlinedTextField(
        value = comment,
        onValueChange = { comment = it.take(300) },
        label = { Text("Комментарий к заявке (необязательно)") },
        placeholder = { Text("Например: дата просмотра или заезда") },
        maxLines = 2,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("yoomoney_comment_input"),
      )

      Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "Заявка получает уникальный код заказа (label). Возврат без перевода не подтверждает бронь — оплата сверяется по API ЮMoney.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
          )
        }
      }

      Button(
        onClick = {
          if (isAmountValid) {
            onSubmitAndPay(parsedAmount, paymentMethod, comment)
          }
        },
        enabled = isAmountValid && !isSubmitting,
        modifier =
          Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("confirm_yoomoney_pay_btn"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
        } else {
          Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Платить ${formatRub(parsedAmount)} ₽ · ЮMoney")
        }
      }
    }
  }
}
