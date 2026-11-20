package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Apartment
import com.example.data.DealType
import com.example.data.YOO_MONEY_WALLET
import com.example.data.formatRub
import com.example.ui.components.MetaChip

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ApartmentDetailScreen(
  apartment: Apartment,
  isFavorite: Boolean,
  onBack: () -> Unit,
  onToggleFavorite: () -> Unit,
  onAskAiConditions: () -> Unit,
  onPayYooMoney: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val pagerState = rememberPagerState(pageCount = { apartment.photos.size })

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .navigationBarsPadding()
  ) {
    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
    ) {
      // Swipeable photo gallery
      Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
      ) {
        HorizontalPager(
          state = pagerState,
          modifier = Modifier.fillMaxSize().testTag("apartment_photo_pager"),
        ) { page ->
          val photo = apartment.photos[page]
          Box(modifier = Modifier.fillMaxSize()) {
            Image(
              painter = painterResource(id = photo.resId),
              contentDescription = photo.caption,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
            Surface(
              color = Color.Black.copy(alpha = 0.55f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
            ) {
              Text(
                text = photo.caption,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          }
        }

        // Top controls overlay
        Row(
          modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = CircleShape,
          ) {
            IconButton(
              onClick = onBack,
              modifier = Modifier.size(48.dp).testTag("detail_back_btn"),
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад к каталогу")
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = CircleShape,
          ) {
            IconButton(
              onClick = onToggleFavorite,
              modifier = Modifier.size(48.dp).testTag("detail_favorite_btn"),
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

        // Photo counter badge & dots
        Row(
          modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          repeat(apartment.photos.size) { index ->
            Box(
              modifier =
                Modifier
                  .size(if (pagerState.currentPage == index) 9.dp else 6.dp)
                  .clip(CircleShape)
                  .background(
                    if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                    else Color.White.copy(alpha = 0.7f)
                  )
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp)) {
            Text(
              text = "${pagerState.currentPage + 1}/${apartment.photos.size}",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
      }

      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color =
              if (apartment.dealType == DealType.RENT) MaterialTheme.colorScheme.secondary
              else MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = apartment.dealType.displayName,
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
          }

          Text(
            text = "${formatRub(apartment.priceRub)} ${apartment.dealType.priceSuffix}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        Text(
          text = apartment.title,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${apartment.address} · ${apartment.region.displayName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        val roomsLabel = if (apartment.rooms == 0) "Студия" else "${apartment.rooms} комн."
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          MetaChip(text = roomsLabel)
          MetaChip(text = "${apartment.areaSqM} м²")
          MetaChip(text = "Этаж ${apartment.floor} из ${apartment.totalFloors}")
        }

        Text(
          text = apartment.description,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          apartment.features.forEach { feature ->
            Surface(
              color = MaterialTheme.colorScheme.tertiaryContainer,
              shape = RoundedCornerShape(10.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Filled.CheckCircle,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = feature,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
              }
            }
          }
        }

        // Conditions & AI Consultant Card
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Условия сделки",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
              text = apartment.conditionsSummary,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            FilledTonalButton(
              onClick = onAskAiConditions,
              modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("detail_ask_ai_btn"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Спросить ИИ об условиях")
            }
          }
        }

        // YooMoney Payment Summary Card
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "Онлайн-оплата через ЮMoney",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              text = "Официальный счёт: $YOO_MONEY_WALLET · https://yoomoney.ru",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              text =
                if (apartment.dealType == DealType.RENT)
                  "Рекомендуемый платёж (бронь/первый месяц): ${formatRub(apartment.depositOrBookingRub)} ₽"
                else
                  "Задаток за фиксацию цены: ${formatRub(apartment.depositOrBookingRub)} ₽",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
          }
        }
      }
    }

    // Sticky Bottom Pay Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 8.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${formatRub(apartment.depositOrBookingRub)} ₽",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Счёт $YOO_MONEY_WALLET",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = onPayYooMoney,
          modifier = Modifier.heightIn(min = 50.dp).testTag("detail_pay_yoomoney_btn"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Платить · ЮMoney")
        }
      }
    }
  }
}
