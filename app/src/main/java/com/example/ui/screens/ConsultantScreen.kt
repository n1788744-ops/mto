package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Apartment
import com.example.data.ConsultantCategory
import com.example.data.KnowledgeArticle
import com.example.data.formatRub
import com.example.ui.viewmodel.ConsultantMessage
import com.example.ui.viewmodel.ConsultantUiState

@Composable
fun ConsultantScreen(
  messages: List<ConsultantMessage>,
  uiState: ConsultantUiState,
  selectedCategory: ConsultantCategory,
  categoryArticles: List<KnowledgeArticle>,
  kbCount: Int,
  onSelectCategory: (ConsultantCategory) -> Unit,
  onOpenArticle: (KnowledgeArticle) -> Unit,
  onRefreshKb: () -> Unit,
  onSendMessage: (String) -> Unit,
  onOpenApartment: (Apartment) -> Unit,
  onPayApartment: (Apartment) -> Unit,
) {
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val isBusy = uiState is ConsultantUiState.Loading

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .imePadding()
  ) {
    // Header with API knowledge base status & category chips
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = CircleShape,
              modifier = Modifier.size(42.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.SupportAgent,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Консультант ЮгДом",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.Storage,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Собственная база API · $kbCount тем",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          IconButton(
            onClick = onRefreshKb,
            modifier = Modifier.size(48.dp).testTag("consultant_refresh_kb_btn"),
          ) {
            Icon(
              imageVector = Icons.Filled.Refresh,
              contentDescription = "Обновить базу знаний",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category filter chips
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ConsultantCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { onSelectCategory(cat) },
              label = { Text(cat.label) },
              modifier = Modifier.testTag("consultant_cat_${cat.code}"),
            )
          }
        }

        // Quick KB articles for selected category
        if (categoryArticles.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            categoryArticles.take(5).forEach { article ->
              AssistChip(
                onClick = { onOpenArticle(article) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                },
                label = {
                  Text(
                    text = article.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 200.dp),
                  )
                },
                modifier = Modifier.testTag("kb_article_${article.articleId}"),
              )
            }
          }
        }
      }
    }

    // Chat messages list
    LazyColumn(
      state = listState,
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(messages, key = { it.id }) { msg ->
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = if (msg.isFromUser) Alignment.End else Alignment.Start,
        ) {
          Surface(
            color =
              if (msg.isFromUser) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surface,
            shape =
              RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (msg.isFromUser) 18.dp else 4.dp,
                bottomEnd = if (msg.isFromUser) 4.dp else 18.dp,
              ),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 335.dp),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              if (!msg.isFromUser && !msg.sourceLabel.isNullOrBlank()) {
                Surface(
                  color = MaterialTheme.colorScheme.tertiaryContainer,
                  shape = RoundedCornerShape(8.dp),
                ) {
                  Text(
                    text = msg.sourceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
              }

              Text(
                text = msg.text,
                style = MaterialTheme.typography.bodyMedium,
                color =
                  if (msg.isFromUser) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurface,
              )

              if (msg.matchedApartments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  msg.matchedApartments.forEach { apt ->
                    ConsultantApartmentSnippet(
                      apartment = apt,
                      onOpen = { onOpenApartment(apt) },
                      onPay = { onPayApartment(apt) },
                    )
                  }
                }
              }

              if (!msg.isFromUser && msg.followUpSuggestions.isNotEmpty()) {
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  msg.followUpSuggestions.forEach { prompt ->
                    AssistChip(
                      onClick = { if (!isBusy) onSendMessage(prompt) },
                      label = { Text(prompt, style = MaterialTheme.typography.labelMedium) },
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Input bar
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = { Text("Поиск по базе: аренда, защита, Мелитополь...") },
          maxLines = 3,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.weight(1f).testTag("consultant_message_input"),
        )
        Surface(
          color =
            if (inputText.isNotBlank() && !isBusy) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
          shape = CircleShape,
        ) {
          IconButton(
            onClick = {
              val toSend = inputText
              inputText = ""
              onSendMessage(toSend)
            },
            enabled = inputText.isNotBlank() && !isBusy,
            modifier = Modifier.size(50.dp).testTag("consultant_send_button"),
          ) {
            if (isBusy) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Найти ответ в базе",
                tint =
                  if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ConsultantApartmentSnippet(
  apartment: Apartment,
  onOpen: () -> Unit,
  onPay: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Image(
          painter = painterResource(id = apartment.photos.first().resId),
          contentDescription = apartment.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = apartment.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${formatRub(apartment.priceRub)} ${apartment.dealType.priceSuffix} · Бронь ${formatRub(apartment.depositOrBookingRub)} ₽",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onOpen,
          modifier = Modifier.weight(1f).heightIn(min = 42.dp),
          shape = RoundedCornerShape(10.dp),
        ) {
          Text("Открыть", style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButton(
          onClick = onPay,
          modifier = Modifier.weight(1f).heightIn(min = 42.dp),
          shape = RoundedCornerShape(10.dp),
        ) {
          Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Платить", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}
