package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiConditionsRepository
import com.example.data.Apartment
import com.example.data.formatRub
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class AiChatMessage(
  val id: String,
  val isFromUser: Boolean,
  val text: String,
  val isStreaming: Boolean = false,
)

sealed interface AiUiState {
  data object Idle : AiUiState
  data object Loading : AiUiState
  data class Streaming(val partialText: String) : AiUiState
  data object Success : AiUiState
  data class Error(val message: String) : AiUiState
}

class AiConditionsViewModel(
  private val repository: AiConditionsRepository = AiConditionsRepository(),
) : ViewModel() {

  private val _messages =
    MutableStateFlow(
      listOf(
        AiChatMessage(
          id = "welcome_ai",
          isFromUser = false,
          text =
            "Здравствуйте! Я ИИ-консультант «ЮгДом». Расскажу об условиях аренды и покупки квартир в Мелитополе, Херсонской и Запорожской областях, а также об оплате задатка или аренды через ЮMoney (4100119599343199). О чём хотите узнать?",
        )
      )
    )
  val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

  private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
  val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

  private var lastFailedPrompt: String? = null

  fun askAboutApartment(apartment: Apartment) {
    val prompt =
      "Расскажи подробно об условиях для квартиры «${apartment.title}» (${apartment.city}, ${apartment.address}). " +
        "Тип сделки: ${apartment.dealType.displayName}, стоимость: ${formatRub(apartment.priceRub)} ${apartment.dealType.priceSuffix}, " +
        "сумма брони/залога: ${formatRub(apartment.depositOrBookingRub)} ₽. Как оформить и оплатить через ЮMoney?"
    sendMessage(prompt)
  }

  fun retryLastMessage() {
    val prompt = lastFailedPrompt ?: return
    sendMessage(prompt)
  }

  fun sendMessage(rawText: String) {
    val prompt = rawText.trim()
    if (prompt.isEmpty() || _uiState.value is AiUiState.Loading || _uiState.value is AiUiState.Streaming) {
      return
    }

    lastFailedPrompt = null

    val history: List<Content> =
      _messages.value
        .filter { !it.isStreaming && it.id != "welcome_ai" }
        .map { msg ->
          content(role = if (msg.isFromUser) "user" else "model") { text(msg.text) }
        }

    val userMsgId = "u_${System.currentTimeMillis()}"
    val aiMsgId = "a_${System.currentTimeMillis() + 1}"

    _messages.update { current ->
      current +
        AiChatMessage(id = userMsgId, isFromUser = true, text = prompt) +
        AiChatMessage(id = aiMsgId, isFromUser = false, text = "", isStreaming = true)
    }

    viewModelScope.launch {
      _uiState.value = AiUiState.Loading
      val accumulated = StringBuilder()
      try {
        withTimeout(60_000L) {
          repository.streamAnswer(prompt, history).collect { chunk ->
            if (chunk.isNotEmpty()) {
              accumulated.append(chunk)
              val currentText = accumulated.toString()
              _uiState.value = AiUiState.Streaming(currentText)
              _messages.update { list ->
                list.map { item ->
                  if (item.id == aiMsgId) item.copy(text = currentText, isStreaming = true) else item
                }
              }
            }
          }
        }
        val finalText = accumulated.toString().trim()
        if (finalText.isNotEmpty()) {
          _messages.update { list ->
            list.map { item ->
              if (item.id == aiMsgId) item.copy(text = finalText, isStreaming = false) else item
            }
          }
          _uiState.value = AiUiState.Success
        } else {
          removeEmptyPlaceholder(aiMsgId)
          lastFailedPrompt = prompt
          _uiState.value = AiUiState.Error("ИИ вернул пустой ответ. Попробуйте переформулировать вопрос.")
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI generation timed out", e)
        removeEmptyPlaceholder(aiMsgId)
        lastFailedPrompt = prompt
        _uiState.value =
          AiUiState.Error("Превышено время ожидания ответа (60 сек). Проверьте сеть и повторите.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Firebase AI generation failed", e)
        removeEmptyPlaceholder(aiMsgId)
        lastFailedPrompt = prompt
        _uiState.value =
          AiUiState.Error(e.localizedMessage ?: "Не удалось получить ответ от ИИ-консультанта.")
      }
    }
  }

  private fun removeEmptyPlaceholder(aiMsgId: String) {
    _messages.update { list ->
      list.filterNot { it.id == aiMsgId && it.text.isBlank() }
    }
  }

  private companion object {
    const val TAG = "AiConditionsVM"
  }
}
