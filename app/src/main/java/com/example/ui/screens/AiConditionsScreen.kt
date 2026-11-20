package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.YOO_MONEY_WALLET
import com.example.ui.components.openYooMoneyInBrowser
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.AiUiState

@Composable
fun AiConditionsScreen(
  messages: List<AiChatMessage>,
  uiState: AiUiState,
  onSendMessage: (String) -> Unit,
  onRetry: () -> Unit,
) {
  val context = LocalContext.current
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val isBusy = uiState is AiUiState.Loading || uiState is AiUiState.Streaming

  LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
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
    // Header
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
              modifier = Modifier.size(40.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.AutoAwesome,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "ИИ-консультант по условиям",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Аренда, покупка, документы и ЮMoney",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          FilledTonalButton(
            onClick = { openYooMoneyInBrowser(context) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("ai_pay_yoomoney_btn"),
          ) {
            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ЮMoney")
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick question chips
        val suggestions =
          listOf(
            "Условия аренды квартиры",
            "Какие документы нужны для покупки?",
            "Как оплатить бронь на $YOO_MONEY_WALLET?",
            "Условия возврата залога",
          )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          suggestions.forEachIndexed { idx, prompt ->
            AssistChip(
              onClick = { if (!isBusy) onSendMessage(prompt) },
              label = { Text(prompt) },
              modifier = Modifier.testTag("ai_quick_chip_$idx"),
            )
          }
        }
      }
    }

    // Chat messages list
    LazyColumn(
      state = listState,
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(messages, key = { it.id }) { msg ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (msg.isFromUser) Arrangement.End else Arrangement.Start,
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
            modifier = Modifier.widthIn(max = 320.dp),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              if (msg.text.isEmpty() && msg.isStreaming) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                  Text(
                    text = "ИИ готовит ответ об условиях...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              } else {
                Text(
                  text = msg.text,
                  style = MaterialTheme.typography.bodyMedium,
                  color =
                    if (msg.isFromUser) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }

      if (uiState is AiUiState.Error) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("ai_error_banner"),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
              Button(
                onClick = onRetry,
                modifier = Modifier.heightIn(min = 48.dp).testTag("ai_retry_btn"),
              ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Повторить запрос")
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
          placeholder = { Text("Задайте вопрос об условиях...") },
          maxLines = 3,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.weight(1f).testTag("ai_message_input"),
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
            modifier = Modifier.size(50.dp).testTag("ai_send_button"),
          ) {
            if (isBusy) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Отправить вопрос ИИ",
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
