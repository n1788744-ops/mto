package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

@JsonClass(generateAdapter = true)
data class YooMoneyTokenResponse(
  @Json(name = "access_token") val accessToken: String? = null,
  @Json(name = "error") val error: String? = null,
)

@JsonClass(generateAdapter = true)
data class YooMoneyAccountInfoResponse(
  @Json(name = "account") val account: String? = null,
  @Json(name = "balance") val balance: Double? = null,
  @Json(name = "currency") val currency: String? = null,
  @Json(name = "account_status") val accountStatus: String? = null,
  @Json(name = "account_type") val accountType: String? = null,
)

@JsonClass(generateAdapter = true)
data class YooMoneyOperationItem(
  @Json(name = "operation_id") val operationId: String = "",
  @Json(name = "status") val status: String? = null,
  @Json(name = "datetime") val datetime: String? = null,
  @Json(name = "title") val title: String? = null,
  @Json(name = "direction") val direction: String? = null,
  @Json(name = "amount") val amount: Double? = null,
  @Json(name = "label") val label: String? = null,
  @Json(name = "type") val type: String? = null,
)

@JsonClass(generateAdapter = true)
data class YooMoneyOperationHistoryResponse(
  @Json(name = "error") val error: String? = null,
  @Json(name = "next_record") val nextRecord: String? = null,
  @Json(name = "operations") val operations: List<YooMoneyOperationItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class YooMoneyOperationDetailsResponse(
  @Json(name = "error") val error: String? = null,
  @Json(name = "operation_id") val operationId: String? = null,
  @Json(name = "status") val status: String? = null,
  @Json(name = "datetime") val datetime: String? = null,
  @Json(name = "direction") val direction: String? = null,
  @Json(name = "amount") val amount: Double? = null,
  @Json(name = "label") val label: String? = null,
  @Json(name = "message") val message: String? = null,
  @Json(name = "sender") val sender: String? = null,
)

interface YooMoneyRestApi {
  @FormUrlEncoded
  @POST("oauth/token")
  suspend fun exchangeToken(
    @Field("code") code: String,
    @Field("client_id") clientId: String,
    @Field("grant_type") grantType: String = "authorization_code",
    @Field("redirect_uri") redirectUri: String = YOO_MONEY_OAUTH_REDIRECT_URI,
    @Field("client_secret") clientSecret: String,
  ): YooMoneyTokenResponse

  @POST("api/account-info")
  suspend fun getAccountInfo(
    @Header("Authorization") bearerToken: String,
  ): YooMoneyAccountInfoResponse

  @FormUrlEncoded
  @POST("api/operation-history")
  suspend fun getOperationHistory(
    @Header("Authorization") bearerToken: String,
    @Field("type") type: String = "deposition",
    @Field("label") label: String? = null,
    @Field("records") records: Int = 30,
    @Field("details") details: Boolean = true,
  ): YooMoneyOperationHistoryResponse

  @FormUrlEncoded
  @POST("api/operation-details")
  suspend fun getOperationDetails(
    @Header("Authorization") bearerToken: String,
    @Field("operation_id") operationId: String,
  ): YooMoneyOperationDetailsResponse
}

data class YooMoneyMerchantStatus(
  val credentialsConfigured: Boolean = false,
  val isConnected: Boolean = false,
  val connectedWallet: String? = null,
  val balanceRub: Double? = null,
)

sealed interface YooMoneyCheckOutcome {
  data class Verified(
    val operationId: String,
    val verifiedAmountRub: Long,
    val datetimeIso: String,
  ) : YooMoneyCheckOutcome

  data class Rejected(
    val reason: String,
    val isFraudAlert: Boolean,
  ) : YooMoneyCheckOutcome

  data class NotFoundYet(val message: String) : YooMoneyCheckOutcome

  data class MerchantAuthRequired(val message: String) : YooMoneyCheckOutcome
}

class YooMoneyPaymentManager(context: Context) {
  private val appContext = context.applicationContext
  private val prefs =
    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  private val okHttpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(25, TimeUnit.SECONDS)
      .apply {
        if (BuildConfig.DEBUG) {
          addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
          )
        }
      }
      .build()

  private val api: YooMoneyRestApi =
    Retrofit.Builder()
      .baseUrl("https://yoomoney.ru/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(YooMoneyRestApi::class.java)

  fun hasConfiguredOAuthSecrets(): Boolean {
    val cid = BuildConfig.YOOMONEY_CLIENT_ID.trim()
    val secret = BuildConfig.YOOMONEY_CLIENT_SECRET.trim()
    return cid.isNotEmpty() &&
      !cid.startsWith("YOUR_") &&
      secret.isNotEmpty() &&
      !secret.startsWith("YOUR_")
  }

  fun getMerchantStatus(): YooMoneyMerchantStatus {
    val token = prefs.getString(KEY_ACCESS_TOKEN, null)
    val wallet = prefs.getString(KEY_CONNECTED_WALLET, null)
    val balanceStr = prefs.getString(KEY_LAST_BALANCE, null)
    return YooMoneyMerchantStatus(
      credentialsConfigured = hasConfiguredOAuthSecrets(),
      isConnected = !token.isNullOrBlank() && wallet == YOO_MONEY_WALLET,
      connectedWallet = wallet,
      balanceRub = balanceStr?.toDoubleOrNull(),
    )
  }

  fun buildQuickpayUri(booking: PaymentBooking): Uri {
    val targetText =
      "Оплата ЮгДом (${booking.paymentLabel}): ${booking.apartmentTitle}".take(150)
    return Uri.parse(YOO_MONEY_QUICKPAY_URL)
      .buildUpon()
      .appendQueryParameter("receiver", YOO_MONEY_WALLET)
      .appendQueryParameter("quickpay-form", "button")
      .appendQueryParameter("targets", targetText)
      .appendQueryParameter("paymentType", booking.paymentMethod)
      .appendQueryParameter("sum", booking.amountRub.toString())
      .appendQueryParameter("label", booking.paymentLabel)
      .appendQueryParameter(
        "successURL",
        Uri.parse(YOO_MONEY_RETURN_URI)
          .buildUpon()
          .appendQueryParameter("label", booking.paymentLabel)
          .build()
          .toString(),
      )
      .build()
  }

  fun buildOAuthAuthorizeUri(): Result<Uri> {
    if (!hasConfiguredOAuthSecrets()) {
      return Result.failure(
        IllegalStateException(
          "Добавьте YOOMONEY_CLIENT_ID и YOOMONEY_CLIENT_SECRET в панель Secrets в AI Studio и пересоберите приложение."
        )
      )
    }
    val uri =
      Uri.parse("https://yoomoney.ru/oauth/authorize")
        .buildUpon()
        .appendQueryParameter("client_id", BuildConfig.YOOMONEY_CLIENT_ID.trim())
        .appendQueryParameter("response_type", "code")
        .appendQueryParameter("redirect_uri", YOO_MONEY_OAUTH_REDIRECT_URI)
        .appendQueryParameter("scope", "account-info operation-history operation-details")
        .build()
    return Result.success(uri)
  }

  suspend fun connectWithOAuthCodeOrUrl(rawCodeOrUrl: String): Result<YooMoneyMerchantStatus> =
    withContext(Dispatchers.IO) {
      if (!hasConfiguredOAuthSecrets()) {
        return@withContext Result.failure(
          IllegalStateException(
            "Секреты YOOMONEY_CLIENT_ID и YOOMONEY_CLIENT_SECRET не настроены в BuildConfig."
          )
        )
      }
      val trimmed = rawCodeOrUrl.trim()
      val code =
        if (trimmed.contains("code=")) {
          Uri.parse(trimmed).getQueryParameter("code").orEmpty().trim()
        } else {
          trimmed
        }
      if (code.length < 8) {
        return@withContext Result.failure(
          IllegalArgumentException("Некорректный код авторизации ЮMoney OAuth2.")
        )
      }

      try {
        val tokenResp =
          api.exchangeToken(
            code = code,
            clientId = BuildConfig.YOOMONEY_CLIENT_ID.trim(),
            clientSecret = BuildConfig.YOOMONEY_CLIENT_SECRET.trim(),
          )
        val token = tokenResp.accessToken
        if (token.isNullOrBlank()) {
          val err = tokenResp.error ?: "empty_token"
          return@withContext Result.failure(
            IllegalStateException("ЮMoney отклонил выдачу токена: $err")
          )
        }

        val accountInfo = api.getAccountInfo("Bearer $token")
        val actualAccount = accountInfo.account.orEmpty().trim()
        if (actualAccount != YOO_MONEY_WALLET) {
          return@withContext Result.failure(
            SecurityException(
              "Ошибка безопасности: подключён кошелёк $actualAccount вместо целевого $YOO_MONEY_WALLET."
            )
          )
        }

        prefs
          .edit()
          .putString(KEY_ACCESS_TOKEN, token)
          .putString(KEY_CONNECTED_WALLET, actualAccount)
          .putString(KEY_LAST_BALANCE, accountInfo.balance?.toString())
          .apply()

        Result.success(getMerchantStatus())
      } catch (e: HttpException) {
        Log.e(TAG, "YooMoney OAuth HTTP error ${e.code()}", e)
        Result.failure(IllegalStateException("Ошибка API ЮMoney (HTTP ${e.code()})."))
      } catch (e: Exception) {
        Log.e(TAG, "YooMoney OAuth exchange failed", e)
        Result.failure(e)
      }
    }

  fun disconnectMerchant() {
    prefs.edit().clear().apply()
  }

  /**
   * Executes strict anti-fraud verification against YooMoney API:
   * 1. Merchant OAuth2 token must belong to wallet 4100119599343199.
   * 2. Operation must exist in incoming deposition history (`direction == "in"`, `status == "success"`).
   * 3. Operation label must match the unique `booking.paymentLabel` (if present).
   * 4. Amount must be >= `booking.amountRub` (blocks partial/1-ruble tampering).
   * 5. Timestamp must be >= `booking.createdAt` - 2 min (blocks reusing old transactions).
   * 6. `operation_id` must not have been claimed by any other booking (anti-replay).
   */
  suspend fun verifyPayment(
    booking: PaymentBooking,
    manualOperationId: String?,
    isOperationAlreadyUsed: suspend (String) -> Boolean,
  ): YooMoneyCheckOutcome =
    withContext(Dispatchers.IO) {
      val token = prefs.getString(KEY_ACCESS_TOKEN, null)
      val connectedWallet = prefs.getString(KEY_CONNECTED_WALLET, null)
      if (token.isNullOrBlank() || connectedWallet != YOO_MONEY_WALLET) {
        return@withContext YooMoneyCheckOutcome.MerchantAuthRequired(
          "OAuth2-проверка кошелька $YOO_MONEY_WALLET не подключена на этом устройстве. " +
            "Подключите OAuth2 в разделе «Кабинет» или отправьте ID операции из чека на ручную проверку."
        )
      }

      val bearer = "Bearer $token"
      try {
        // Confirm wallet identity & refresh balance
        val accountInfo = api.getAccountInfo(bearer)
        if (accountInfo.account != YOO_MONEY_WALLET) {
          disconnectMerchant()
          return@withContext YooMoneyCheckOutcome.Rejected(
            reason = "Токен принадлежит другому кошельку (${accountInfo.account}). Проверка заблокирована.",
            isFraudAlert = true,
          )
        }
        prefs.edit().putString(KEY_LAST_BALANCE, accountInfo.balance?.toString()).apply()

        val opIdInput = manualOperationId?.trim()?.takeIf { it.isNotEmpty() }
        if (opIdInput != null) {
          if (isOperationAlreadyUsed(opIdInput)) {
            return@withContext YooMoneyCheckOutcome.Rejected(
              reason = "Повторное использование чека: операция $opIdInput уже привязана к другой заявке.",
              isFraudAlert = true,
            )
          }
          val details = api.getOperationDetails(bearer, opIdInput)
          if (details.error != null || details.operationId.isNullOrBlank()) {
            return@withContext YooMoneyCheckOutcome.Rejected(
              reason = "Операция $opIdInput не найдена на кошельке $YOO_MONEY_WALLET (${details.error ?: "illegal_param"}).",
              isFraudAlert = true,
            )
          }
          return@withContext validateMatchedOperation(
            operationId = details.operationId,
            status = details.status,
            direction = details.direction,
            amount = details.amount,
            label = details.label,
            datetimeIso = details.datetime,
            booking = booking,
            isOperationAlreadyUsed = isOperationAlreadyUsed,
          )
        }

        // Primary check: query operation-history filtered strictly by unique booking paymentLabel
        val history =
          api.getOperationHistory(
            bearerToken = bearer,
            type = "deposition",
            label = booking.paymentLabel,
            records = 30,
            details = true,
          )
        if (history.error != null) {
          return@withContext YooMoneyCheckOutcome.NotFoundYet(
            "ЮMoney API вернул ошибку: ${history.error}"
          )
        }

        val matchingOps =
          history.operations.filter { op ->
            op.label == booking.paymentLabel || op.operationId == booking.submittedOperationId
          }
        if (matchingOps.isEmpty()) {
          return@withContext YooMoneyCheckOutcome.NotFoundYet(
            "Платёж с меткой ${booking.paymentLabel} пока не поступил на кошелёк $YOO_MONEY_WALLET. " +
              "Переход на страницу ЮMoney не является оплатой."
          )
        }

        for (op in matchingOps) {
          val outcome =
            validateMatchedOperation(
              operationId = op.operationId,
              status = op.status,
              direction = op.direction,
              amount = op.amount,
              label = op.label,
              datetimeIso = op.datetime,
              booking = booking,
              isOperationAlreadyUsed = isOperationAlreadyUsed,
            )
          if (outcome is YooMoneyCheckOutcome.Verified || outcome is YooMoneyCheckOutcome.Rejected) {
            return@withContext outcome
          }
        }

        YooMoneyCheckOutcome.NotFoundYet(
          "Операция по метке ${booking.paymentLabel} ещё обрабатывается или не подтверждена."
        )
      } catch (e: HttpException) {
        if (e.code() == 401 || e.code() == 403) {
          disconnectMerchant()
          YooMoneyCheckOutcome.MerchantAuthRequired(
            "Срок действия токена ЮMoney истёк. Подключите OAuth2 заново в разделе «Кабинет»."
          )
        } else {
          YooMoneyCheckOutcome.NotFoundYet("Ошибка сети ЮMoney API (HTTP ${e.code()}).")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error verifying YooMoney payment", e)
        YooMoneyCheckOutcome.NotFoundYet(
          e.localizedMessage ?: "Не удалось связаться с сервером ЮMoney."
        )
      }
    }

  private suspend fun validateMatchedOperation(
    operationId: String,
    status: String?,
    direction: String?,
    amount: Double?,
    label: String?,
    datetimeIso: String?,
    booking: PaymentBooking,
    isOperationAlreadyUsed: suspend (String) -> Boolean,
  ): YooMoneyCheckOutcome {
    if (operationId.isBlank()) {
      return YooMoneyCheckOutcome.Rejected("Пустой идентификатор операции ЮMoney.", isFraudAlert = true)
    }
    if (isOperationAlreadyUsed(operationId)) {
      return YooMoneyCheckOutcome.Rejected(
        reason = "Обнаружена попытка повторного использования: операция $operationId уже использовалась.",
        isFraudAlert = true,
      )
    }
    if (direction != "in") {
      return YooMoneyCheckOutcome.Rejected(
        reason = "Неверное направление транзакции ($direction): ожидалось входящее зачисление.",
        isFraudAlert = true,
      )
    }
    if (status != "success") {
      if (status == "in_progress") {
        return YooMoneyCheckOutcome.NotFoundYet("Платёж $operationId ещё в обработке ЮMoney.")
      }
      return YooMoneyCheckOutcome.Rejected(
        reason = "Платёж $operationId отклонён или не завершён (статус: ${status ?: "unknown"}).",
        isFraudAlert = true,
      )
    }
    if (!label.isNullOrBlank() && label != booking.paymentLabel) {
      return YooMoneyCheckOutcome.Rejected(
        reason = "Метка платежа ($label) не совпадает с заявкой (${booking.paymentLabel}).",
        isFraudAlert = true,
      )
    }

    val receivedRub = (amount ?: 0.0).roundToLong()
    if (receivedRub < booking.amountRub) {
      return YooMoneyCheckOutcome.Rejected(
        reason =
          "Недостаточная сумма оплаты: получено ${formatRub(receivedRub)} ₽ вместо ${formatRub(booking.amountRub)} ₽.",
        isFraudAlert = true,
      )
    }

    val bookingCreatedMs = booking.createdAt?.toDate()?.time ?: 0L
    val opTimeMs = parseIso8601Millis(datetimeIso)
    if (bookingCreatedMs > 0L && opTimeMs > 0L && opTimeMs < bookingCreatedMs - 120_000L) {
      return YooMoneyCheckOutcome.Rejected(
        reason = "Операция $operationId совершена раньше создания заявки и не может быть принята.",
        isFraudAlert = true,
      )
    }

    return YooMoneyCheckOutcome.Verified(
      operationId = operationId,
      verifiedAmountRub = receivedRub,
      datetimeIso = datetimeIso.orEmpty(),
    )
  }

  private fun parseIso8601Millis(iso: String?): Long {
    if (iso.isNullOrBlank()) return 0L
    val patterns =
      listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
      )
    for (pattern in patterns) {
      try {
        val sdf = SimpleDateFormat(pattern, Locale.US).apply {
          timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = sdf.parse(iso)
        if (date != null) return date.time
      } catch (_: Exception) {
      }
    }
    return 0L
  }

  private companion object {
    const val TAG = "YooMoneyManager"
    const val PREFS_NAME = "yoomoney_merchant_prefs"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_CONNECTED_WALLET = "connected_wallet"
    const val KEY_LAST_BALANCE = "last_balance"
  }
}
