package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Apartment
import com.example.data.ApartmentCatalog
import com.example.data.ConsultantCategory
import com.example.data.ConsultantRepository
import com.example.data.KnowledgeArticle
import com.example.data.YOO_MONEY_WALLET
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConsultantMessage(
  val id: String,
  val isFromUser: Boolean,
  val text: String,
  val sourceLabel: String? = null,
  val matchedArticles: List<KnowledgeArticle> = emptyList(),
  val matchedApartments: List<Apartment> = emptyList(),
  val followUpSuggestions: List<String> = emptyList(),
)

sealed interface ConsultantUiState {
  data object Idle : ConsultantUiState
  data object Loading : ConsultantUiState
  data class Ready(val kbCount: Int) : ConsultantUiState
}

class ConsultantViewModel(
  private val repository: ConsultantRepository,
) : ViewModel() {

  private val _selectedCategory = MutableStateFlow(ConsultantCategory.ALL)
  val selectedCategory: StateFlow<ConsultantCategory> = _selectedCategory.asStateFlow()

  private val _kbCount = MutableStateFlow(repository.getKnowledgeBaseSize())
  val kbCount: StateFlow<Int> = _kbCount.asStateFlow()

  private val _categoryArticles =
    MutableStateFlow(repository.getArticlesByCategory(ConsultantCategory.ALL))
  val categoryArticles: StateFlow<List<KnowledgeArticle>> = _categoryArticles.asStateFlow()

  private val _uiState = MutableStateFlow<ConsultantUiState>(ConsultantUiState.Idle)
  val uiState: StateFlow<ConsultantUiState> = _uiState.asStateFlow()

  private val _messages =
    MutableStateFlow(
      listOf(
        ConsultantMessage(
          id = "welcome_consultant",
          isFromUser = false,
          text =
            "Здравствуйте! Я консультант компании «ЮгДом», работающий на нашей собственной базе знаний и API каталога (${ApartmentCatalog.listings.size} квартир).\n\n" +
              "Подскажу точные условия аренды и покупки, список документов, подбор квартир по бюджету и правила защищённой оплаты через ЮMoney ($YOO_MONEY_WALLET).",
          sourceLabel = "База знаний ЮгДом API",
          followUpSuggestions =
            listOf(
              "Условия аренды квартиры",
              "Документы для покупки квартиры",
              "Как работает защита оплаты ЮMoney?",
              "Квартиры в Мелитополе",
            ),
        )
      )
    )
  val messages: StateFlow<List<ConsultantMessage>> = _messages.asStateFlow()

  init {
    refreshDatabase()
  }

  fun refreshDatabase() {
    viewModelScope.launch {
      repository.refreshKnowledgeBase().onSuccess { count ->
        _kbCount.value = count
        _categoryArticles.value = repository.getArticlesByCategory(_selectedCategory.value)
      }
    }
  }

  fun selectCategory(category: ConsultantCategory) {
    _selectedCategory.value = category
    _categoryArticles.value = repository.getArticlesByCategory(category)
  }

  fun openArticleFromDatabase(article: KnowledgeArticle) {
    val userMsg =
      ConsultantMessage(
        id = "u_${System.currentTimeMillis()}",
        isFromUser = true,
        text = article.title,
      )
    val replyMsg =
      ConsultantMessage(
        id = "c_${System.currentTimeMillis() + 1}",
        isFromUser = false,
        text = "${article.title}\n\n${article.summary}",
        sourceLabel = "Статья базы знаний ЮгДом API",
        matchedArticles = listOf(article),
        followUpSuggestions =
          listOf(
            "Квартиры в Мелитополе",
            "Как работает защита оплаты ЮMoney?",
          ),
      )
    _messages.update { it + userMsg + replyMsg }
  }

  fun askAboutApartment(apartment: Apartment) {
    val userPrompt = "Условия и проверка оплаты для «${apartment.title}» (${apartment.city})"
    viewModelScope.launch {
      _uiState.value = ConsultantUiState.Loading
      val reply =
        repository.queryConsultant(
          rawQuery = userPrompt,
          selectedCategory = ConsultantCategory.ALL,
          focusedApartment = apartment,
        )
      appendExchange(userPrompt, reply)
      _uiState.value = ConsultantUiState.Ready(_kbCount.value)
    }
  }

  fun sendMessage(rawText: String) {
    val query = rawText.trim()
    if (query.isEmpty() || _uiState.value is ConsultantUiState.Loading) return

    viewModelScope.launch {
      _uiState.value = ConsultantUiState.Loading
      val reply =
        repository.queryConsultant(
          rawQuery = query,
          selectedCategory = _selectedCategory.value,
          focusedApartment = null,
        )
      appendExchange(query, reply)
      _uiState.value = ConsultantUiState.Ready(_kbCount.value)
    }
  }

  private fun appendExchange(userText: String, reply: com.example.data.ConsultantReply) {
    val ts = System.currentTimeMillis()
    val userMsg =
      ConsultantMessage(
        id = "u_$ts",
        isFromUser = true,
        text = userText,
      )
    val consultantMsg =
      ConsultantMessage(
        id = "c_${ts + 1}",
        isFromUser = false,
        text = reply.answerText,
        sourceLabel = reply.sourceLabel,
        matchedArticles = reply.matchedArticles,
        matchedApartments = reply.matchedApartments,
        followUpSuggestions = reply.followUpSuggestions,
      )
    _messages.update { it + userMsg + consultantMsg }
  }
}
