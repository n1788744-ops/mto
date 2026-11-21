package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ConsultantAskRequest(
  @Json(name = "query") val query: String,
  @Json(name = "category") val category: String? = null,
  @Json(name = "apartment_id") val apartmentId: String? = null,
)

@JsonClass(generateAdapter = true)
data class ConsultantAskResponse(
  @Json(name = "answer") val answer: String,
  @Json(name = "source") val source: String = "api",
  @Json(name = "matched_article_ids") val matchedArticleIds: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RemoteKbArticleDto(
  @Json(name = "id") val id: String,
  @Json(name = "category") val category: String,
  @Json(name = "title") val title: String,
  @Json(name = "summary") val summary: String,
  @Json(name = "keywords") val keywords: List<String> = emptyList(),
  @Json(name = "priority") val priority: Int = 0,
)

interface ConsultantRestApi {
  @GET("v1/kb/articles")
  suspend fun fetchArticles(): List<RemoteKbArticleDto>

  @POST("v1/consultant/ask")
  suspend fun ask(@Body request: ConsultantAskRequest): ConsultantAskResponse
}

enum class ConsultantCategory(val code: String, val label: String) {
  ALL("all", "Все темы"),
  RENT("rent", "Аренда"),
  SALE("sale", "Покупка"),
  PAYMENT("payment", "Оплата и защита"),
  REGIONS("regions", "Локации"),
}

data class ConsultantReply(
  val answerText: String,
  val sourceLabel: String,
  val matchedArticles: List<KnowledgeArticle> = emptyList(),
  val matchedApartments: List<Apartment> = emptyList(),
  val followUpSuggestions: List<String> = emptyList(),
)

object CompanyKnowledgeDatabase {
  val defaultArticles: List<KnowledgeArticle> =
    listOf(
      KnowledgeArticle(
        id = "kb_rent_terms",
        articleId = "kb_rent_terms",
        category = "rent",
        title = "Условия аренды квартир ЮгДом",
        summary =
          "• Срок договора: от 1–3 месяцев с официальным оформлением.\n" +
            "• Страховой залог: 100% от месячной стоимости (возвращается при выезде или засчитывается за последний месяц; для студий возможна разбивка на 2 платежа).\n" +
            "• Коммунальные услуги: оплачиваются по счётчикам (на Арабатской стрелке — фиксированный тариф).\n" +
            "• Заселение: в день подтверждённой оплаты или в согласованную дату.",
        keywords = listOf("аренда", "снять", "договор", "залог", "срок", "коммунальн", "счетчик", "счётчик", "заселен", "студи"),
        priority = 10,
      ),
      KnowledgeArticle(
        id = "kb_sale_terms",
        articleId = "kb_sale_terms",
        category = "sale",
        title = "Условия покупки и рассрочки",
        summary =
          "• Прямая продажа от компании «ЮгДом» без скрытых комиссий.\n" +
            "• Юридическая проверка чистоты истории объекта и подготовка полного пакета для регистрации права собственности.\n" +
            "• Фиксация цены: возвратный задаток (30 000 – 50 000 ₽) снимает квартиру с рекламы на 14 дней.\n" +
            "• Рассрочка: для ряда объектов в Мелитополе и Приморске доступна поэтапная рассрочка до 6 месяцев.",
        keywords = listOf("покуп", "купить", "продаж", "задаток", "рассроч", "ипотек", "комисс", "цена", "брон"),
        priority = 10,
      ),
      KnowledgeArticle(
        id = "kb_sale_docs",
        articleId = "kb_sale_docs",
        category = "sale",
        title = "Документы для аренды и покупки",
        summary =
          "• Для аренды: паспорт гражданина РФ и подтверждённый номер телефона в личном кабинете (по запросу выдаём отчётные документы для командировок).\n" +
            "• Для покупки: паспорт, СНИЛС, ИНН (для супругов — согласие на сделку). Все правоустанавливающие документы по квартире предоставляет компания перед подписанием договора.",
        keywords = listOf("документ", "паспорт", "снилс", "инн", "регистрац", "собственност", "отчетн", "отчётн", "договор"),
        priority = 9,
      ),
      KnowledgeArticle(
        id = "kb_yoomoney_pay",
        articleId = "kb_yoomoney_pay",
        category = "payment",
        title = "Оплата через ЮMoney (кошелёк 4100119599343199)",
        summary =
          "• Официальный счёт компании в ЮMoney: 4100119599343199 (https://yoomoney.ru).\n" +
            "• Доступна оплата банковской картой (AC) или из кошелька ЮMoney (PC).\n" +
            "• Для каждой заявки автоматически создаётся уникальный код заказа (paymentLabel) и фиксируется точная сумма — изменить реквизиты или сумму в обход заявки невозможно.",
        keywords = listOf("юмани", "yoomoney", "оплат", "платить", "кошелек", "кошелёк", "карт", "4100119599343199", "счет", "счёт"),
        priority = 10,
      ),
      KnowledgeArticle(
        id = "kb_antifraud_security",
        articleId = "kb_antifraud_security",
        category = "payment",
        title = "Защита от ложных подтверждений оплаты",
        summary =
          "• Возврат со страницы ЮMoney в приложение НЕ подтверждает оплату — статус остаётся «Ожидает оплаты».\n" +
            "• Система защиты проверяет: 1) поступление средств через OAuth2 API ЮMoney на кошелёк 4100119599343199; 2) точное совпадение уникальной метки заявки (label); 3) сумму перевода (не меньше суммы заявки); 4) время операции (не старше создания заявки).\n" +
            "• Каждый operation_id записывается в защищённый реестр used_operations: повторно использовать старый чек или один перевод на две квартиры невозможно.",
        keywords = listOf("защит", "обман", "проверк", "фейк", "мошен", "вернул", "подтверд", "чек", "операц", "api", "oauth"),
        priority = 10,
      ),
      KnowledgeArticle(
        id = "kb_deposit_refund",
        articleId = "kb_deposit_refund",
        category = "rent",
        title = "Возврат залога и задатка",
        summary =
          "• При аренде: страховой залог возвращается в полном объёме при выезде после приёмки квартиры или засчитывается за последний месяц проживания.\n" +
            "• При покупке: внесённый задаток полностью входит в стоимость квартиры при подписании основного договора купли-продажи.",
        keywords = listOf("возврат", "вернуть", "залог", "задаток", "отмен", "выезд"),
        priority = 8,
      ),
      KnowledgeArticle(
        id = "kb_loc_melitopol",
        articleId = "kb_loc_melitopol",
        category = "regions",
        title = "Мелитополь — районы и предложения",
        summary =
          "В Мелитополе доступны квартиры в Центре (пр. Богдана Хмельницкого), микрорайоне (пр. 50-летия Победы), на Кизияре и в Новом Мелитополе. Аренда от 21 000 ₽/мес, покупка от 3 450 000 ₽.",
        keywords = listOf("мелитопол", "кизияр", "хмельницк", "побед", "южный квартал", "центр"),
        priority = 7,
      ),
      KnowledgeArticle(
        id = "kb_loc_kherson",
        articleId = "kb_loc_kherson",
        category = "regions",
        title = "Херсонская область — Геническ, Скадовск, Новая Каховка",
        summary =
          "Предлагаем квартиры у Азовского и Чёрного морей: Геническ (террасы с видом на море), Счастливцево (Арабатская стрелка), Скадовск и Новая Каховка. Аренда от 24 000 ₽/мес, продажа от 3 150 000 ₽.",
        keywords = listOf("херсон", "геническ", "скадовск", "каховк", "арабат", "счастливц", "море"),
        priority = 7,
      ),
      KnowledgeArticle(
        id = "kb_loc_zaporizhzhia",
        articleId = "kb_loc_zaporizhzhia",
        category = "regions",
        title = "Запорожская область — Бердянск, Приморск, Энергодар",
        summary =
          "В Запорожской области доступны видовые квартиры у Бердянского залива, семейные 3-комн. квартиры в Приморске и 2-комн. в Энергодаре с отчётными документами. Аренда от 23 000 ₽/мес, покупка от 4 200 000 ₽.",
        keywords = listOf("запорож", "бердянск", "приморск", "энергодар", "залив"),
        priority = 7,
      ),
      KnowledgeArticle(
        id = "kb_map_search",
        articleId = "kb_map_search",
        category = "regions",
        title = "Поиск по карте и радиусу",
        summary =
          "Во вкладке «Карта» выберите локацию (Мелитополь, Херсонская или Запорожская область), укажите тип сделки (Аренда / Продажа) и настройте радиус поиска от 5 до 150 км. Нажмите на маркер квартиры для просмотра фото и оплаты брони.",
        keywords = listOf("карт", "радиус", "фильтр", "поиск", "маркер", "локац", "км"),
        priority = 6,
      ),
    )
}

class ConsultantRepository(
  private val db: FirebaseFirestore,
  private val remoteBaseUrl: String? = null,
) {
  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  private val restApi: ConsultantRestApi? =
    remoteBaseUrl
      ?.trim()
      ?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
      ?.let { url ->
        val normalizedUrl = if (url.endsWith("/")) url else "$url/"
        val client =
          OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
          .baseUrl(normalizedUrl)
          .client(client)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build()
          .create(ConsultantRestApi::class.java)
      }

  @Volatile private var cachedKbArticles: List<KnowledgeArticle> = CompanyKnowledgeDatabase.defaultArticles
  @Volatile private var syncedFromCloud: Boolean = false

  fun getKnowledgeBaseSize(): Int = cachedKbArticles.size

  fun getArticlesByCategory(category: ConsultantCategory): List<KnowledgeArticle> {
    val base = cachedKbArticles.sortedByDescending { it.priority }
    return if (category == ConsultantCategory.ALL) {
      base
    } else {
      base.filter { it.category == category.code }
    }
  }

  suspend fun refreshKnowledgeBase(): Result<Int> {
    return try {
      val merged = linkedMapOf<String, KnowledgeArticle>()
      CompanyKnowledgeDatabase.defaultArticles.forEach { merged[it.articleId] = it }

      // 1. Optional custom HTTP REST API sync (if remoteBaseUrl provided)
      if (restApi != null) {
        try {
          val remoteItems = withContext(Dispatchers.IO) { restApi.fetchArticles() }
          remoteItems.forEach { dto ->
            merged[dto.id] =
              KnowledgeArticle(
                id = dto.id,
                articleId = dto.id,
                category = dto.category,
                title = dto.title,
                summary = dto.summary,
                keywords = dto.keywords,
                priority = dto.priority,
              )
          }
          syncedFromCloud = true
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "Optional REST KB endpoint unreachable, using Firestore/local KB", e)
        }
      }

      // 2. Cloud Firestore `/consultant_kb` database sync
      val snapshot = db.collection("consultant_kb").get().await()
      val cloudArticles = snapshot.toObjects(KnowledgeArticle::class.java)
      cloudArticles.forEach { art ->
        val key = art.articleId.ifBlank { art.id }
        if (key.isNotBlank() && art.title.isNotBlank()) {
          merged[key] = art.copy(articleId = key)
        }
      }
      if (cloudArticles.isNotEmpty()) {
        syncedFromCloud = true
      }

      cachedKbArticles = merged.values.toList()
      Result.success(cachedKbArticles.size)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Firestore KB sync fallback to built-in database", e)
      Result.success(cachedKbArticles.size)
    }
  }

  suspend fun queryConsultant(
    rawQuery: String,
    selectedCategory: ConsultantCategory = ConsultantCategory.ALL,
    focusedApartment: Apartment? = null,
  ): ConsultantReply {
    val query = rawQuery.trim()
    val lower = query.lowercase()

    // Try optional remote consultant API if configured
    if (restApi != null && query.isNotBlank()) {
      try {
        val remote =
          withContext(Dispatchers.IO) {
            restApi.ask(
              ConsultantAskRequest(
                query = query,
                category = selectedCategory.code.takeIf { it != "all" },
                apartmentId = focusedApartment?.id,
              )
            )
          }
        if (remote.answer.isNotBlank()) {
          val matched =
            cachedKbArticles.filter { it.articleId in remote.matchedArticleIds }
          return ConsultantReply(
            answerText = remote.answer,
            sourceLabel = "API ЮгДом (${remote.source})",
            matchedArticles = matched,
            matchedApartments = findMatchingApartments(lower, focusedApartment),
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Remote consultant API fallback to local DB engine", e)
      }
    }

    // If user asked specifically about a selected apartment dossier
    if (focusedApartment != null) {
      return buildApartmentDossierReply(focusedApartment)
    }

    // Score KB articles from our merged Firestore + Company database
    val tokens = tokenize(lower)
    val scoredArticles =
      cachedKbArticles
        .asSequence()
        .filter { art ->
          selectedCategory == ConsultantCategory.ALL || art.category == selectedCategory.code
        }
        .map { art -> art to scoreArticle(art, lower, tokens) }
        .filter { (_, score) -> score > 0 }
        .sortedByDescending { (_, score) -> score }
        .map { (art, _) -> art }
        .take(3)
        .toList()

    val matchedApartments = findMatchingApartments(lower, focusedApartment = null)

    val sourceText =
      if (syncedFromCloud) {
        "База знаний API + Firestore (${cachedKbArticles.size} статей)"
      } else {
        "База знаний ЮгДом API (${cachedKbArticles.size} статей · ${ApartmentCatalog.listings.size} объектов)"
      }

    val sb = StringBuilder()
    if (scoredArticles.isNotEmpty()) {
      scoredArticles.forEachIndexed { idx, article ->
        if (idx > 0) sb.append("\n\n")
        sb.append("${article.title}\n${article.summary}")
      }
    }

    if (matchedApartments.isNotEmpty()) {
      if (sb.isNotEmpty()) sb.append("\n\n")
      sb.append("Подходящие квартиры из базы (${matchedApartments.size}):")
      matchedApartments.forEach { apt ->
        val roomsStr = if (apt.rooms == 0) "Студия" else "${apt.rooms}-комн."
        sb.append(
          "\n• ${apt.title} ($roomsStr, ${apt.areaSqM} м²) — ${formatRub(apt.priceRub)} ${apt.dealType.priceSuffix}, " +
            "бронь/залог ${formatRub(apt.depositOrBookingRub)} ₽"
        )
      }
    }

    if (sb.isEmpty()) {
      sb.append(
        "По вашему запросу в базе знаний найдено несколько ключевых правил:\n\n" +
          "1) Аренда и продажа доступны в Мелитополе, Херсонской и Запорожской областях.\n" +
          "2) Оплата задатка или первого месяца проходит через ЮMoney на счёт $YOO_MONEY_WALLET с уникальным кодом заявки.\n" +
          "3) Для защиты от обмана бронь подтверждается только после проверки реального поступления в ЮMoney.\n\n" +
          "Уточните тему (например: «документы для покупки», «залог», «защита оплаты», «квартиры в Бердянске»)."
      )
    }

    val followUps =
      buildList {
        if (matchedApartments.isEmpty()) add("Квартиры в Мелитополе до 30 000")
        add("Как работает защита оплаты ЮMoney?")
        add("Документы для покупки квартиры")
      }.take(3)

    return ConsultantReply(
      answerText = sb.toString(),
      sourceLabel = sourceText,
      matchedArticles = scoredArticles,
      matchedApartments = matchedApartments,
      followUpSuggestions = followUps,
    )
  }

  private fun buildApartmentDossierReply(apartment: Apartment): ConsultantReply {
    val dealLabel = apartment.dealType.displayName
    val roomsLabel = if (apartment.rooms == 0) "Студия" else "${apartment.rooms}-комн. квартира"
    val relevantArticles =
      cachedKbArticles.filter { art ->
        art.articleId in
          listOf(
            if (apartment.dealType == DealType.RENT) "kb_rent_terms" else "kb_sale_terms",
            "kb_yoomoney_pay",
            "kb_antifraud_security",
          )
      }

    val text =
      buildString {
        append("Карточка условий из базы ЮгДом:\n")
        append("Объект: ${apartment.title}\n")
        append("Локация: ${apartment.region.displayName}, ${apartment.address}\n")
        append("Параметры: $roomsLabel, ${apartment.areaSqM} м², этаж ${apartment.floor}/${apartment.totalFloors}\n")
        append("Сделка: $dealLabel · ${formatRub(apartment.priceRub)} ${apartment.dealType.priceSuffix}\n")
        append("Сумма брони / залога: ${formatRub(apartment.depositOrBookingRub)} ₽\n\n")
        append("Условия по объекту:\n${apartment.conditionsSummary}\n\n")
        append(
          "Безопасная оплата:\n" +
            "• Нажмите «Платить» — сформируется заявка с уникальной меткой платежа.\n" +
            "• Перевод направляется на официальный кошелёк ЮMoney $YOO_MONEY_WALLET.\n" +
            "• Зачисление проверяется системой защиты по метке, сумме и ID операции (повтор чека или возврат без оплаты заблокированы)."
        )
      }

    return ConsultantReply(
      answerText = text,
      sourceLabel = "База объектов и правил ЮгДом",
      matchedArticles = relevantArticles,
      matchedApartments = listOf(apartment),
      followUpSuggestions =
        listOf(
          "Как работает защита оплаты ЮMoney?",
          "Документы для покупки квартиры",
          "Условия возврата залога",
        ),
    )
  }

  private fun scoreArticle(
    article: KnowledgeArticle,
    lowerQuery: String,
    tokens: List<String>,
  ): Int {
    var score = 0
    val titleLower = article.title.lowercase()
    val summaryLower = article.summary.lowercase()

    article.keywords.forEach { kw ->
      val kwLower = kw.lowercase()
      if (lowerQuery.contains(kwLower)) {
        score += 12
      } else if (tokens.any { tok -> tok.length >= 3 && (kwLower.contains(tok) || tok.contains(kwLower)) }) {
        score += 7
      }
    }

    tokens.forEach { tok ->
      if (tok.length >= 3) {
        if (titleLower.contains(tok)) score += 6
        if (summaryLower.contains(tok)) score += 3
      }
    }

    if (score > 0) {
      score += article.priority
    }
    return score
  }

  private fun findMatchingApartments(
    lowerQuery: String,
    focusedApartment: Apartment?,
  ): List<Apartment> {
    if (focusedApartment != null) return listOf(focusedApartment)

    val regionFilter =
      when {
        lowerQuery.contains("мелитопол") || lowerQuery.contains("кизияр") -> RegionFilter.MELITOPOL
        lowerQuery.contains("херсон") ||
          lowerQuery.contains("геническ") ||
          lowerQuery.contains("скадовск") ||
          lowerQuery.contains("каховк") ||
          lowerQuery.contains("арабат") ||
          lowerQuery.contains("счастливц") -> RegionFilter.KHERSON
        lowerQuery.contains("запорож") ||
          lowerQuery.contains("бердянск") ||
          lowerQuery.contains("приморск") ||
          lowerQuery.contains("энергодар") -> RegionFilter.ZAPORIZHZHIA
        else -> null
      }

    val dealFilter =
      when {
        lowerQuery.contains("аренд") || lowerQuery.contains("снять") -> DealType.RENT
        lowerQuery.contains("покуп") || lowerQuery.contains("купить") || lowerQuery.contains("продаж") ->
          DealType.SALE
        else -> null
      }

    val roomFilter =
      when {
        lowerQuery.contains("студи") -> 0
        lowerQuery.contains("1-комн") || lowerQuery.contains("1 комн") || lowerQuery.contains("однокомн") -> 1
        lowerQuery.contains("2-комн") || lowerQuery.contains("2 комн") || lowerQuery.contains("двухкомн") -> 2
        lowerQuery.contains("3-комн") || lowerQuery.contains("3 комн") || lowerQuery.contains("трехкомн") || lowerQuery.contains("трёхкомн") -> 3
        else -> null
      }

    val maxPriceRub = extractMaxPriceRub(lowerQuery)
    val mentionsListingSearch =
      regionFilter != null ||
        roomFilter != null ||
        maxPriceRub != null ||
        lowerQuery.contains("квартир") ||
        lowerQuery.contains("вариант") ||
        lowerQuery.contains("подобрать") ||
        lowerQuery.contains("каталог")

    if (!mentionsListingSearch) return emptyList()

    return ApartmentCatalog.listings
      .filter { apt ->
        val okRegion = regionFilter == null || apt.region == regionFilter
        val okDeal = dealFilter == null || apt.dealType == dealFilter
        val okRoom = roomFilter == null || apt.rooms == roomFilter
        val okPrice = maxPriceRub == null || apt.priceRub <= maxPriceRub
        val cityMatch =
          when {
            lowerQuery.contains("геническ") -> apt.city.contains("Геническ", ignoreCase = true)
            lowerQuery.contains("скадовск") -> apt.city.contains("Скадовск", ignoreCase = true)
            lowerQuery.contains("бердянск") -> apt.city.contains("Бердянск", ignoreCase = true)
            lowerQuery.contains("приморск") -> apt.city.contains("Приморск", ignoreCase = true)
            lowerQuery.contains("энергодар") -> apt.city.contains("Энергодар", ignoreCase = true)
            lowerQuery.contains("каховк") -> apt.city.contains("Каховка", ignoreCase = true)
            else -> true
          }
        okRegion && okDeal && okRoom && okPrice && cityMatch
      }
      .take(3)
  }

  private fun extractMaxPriceRub(lowerQuery: String): Long? {
    val millionRegex = Regex("""до\s+(\d+(?:[.,]\d+)?)\s*млн""")
    millionRegex.find(lowerQuery)?.let { match ->
      val num = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return null
      return (num * 1_000_000.0).toLong()
    }
    val thousandRegex = Regex("""до\s+(\d+)\s*тыс""")
    thousandRegex.find(lowerQuery)?.let { match ->
      val num = match.groupValues[1].toLongOrNull() ?: return null
      return num * 1_000L
    }
    val plainRegex = Regex("""до\s+(\d[\d\s]{2,9})""")
    plainRegex.find(lowerQuery)?.let { match ->
      val digits = match.groupValues[1].filter { it.isDigit() }.toLongOrNull() ?: return null
      return digits
    }
    return null
  }

  private fun tokenize(text: String): List<String> =
    text.split(Regex("[^а-яёa-z0-9\\-]+"))
      .map { it.trim() }
      .filter { it.length >= 2 }

  private companion object {
    const val TAG = "ConsultantRepo"
  }
}
