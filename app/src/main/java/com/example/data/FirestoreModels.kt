package com.example.data

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val fullName: String = "",
  val phoneNumber: String = "",
  val preferredRegion: String = "all",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FavoriteApartment(
  @DocumentId val id: String = "",
  val userId: String = "",
  val apartmentId: String = "",
  val apartmentTitle: String = "",
  val region: String = "",
  val dealType: String = "rent",
  val priceRub: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PaymentBooking(
  @DocumentId val id: String = "",
  val userId: String = "",
  val apartmentId: String = "",
  val apartmentTitle: String = "",
  val dealType: String = "rent",
  val amountRub: Long = 0L,
  val yoomoneyWallet: String = YOO_MONEY_WALLET,
  val paymentLabel: String = "",
  val paymentMethod: String = "AC",
  val status: String = BookingStatus.AWAITING_PAYMENT,
  val comment: String? = null,
  val submittedOperationId: String? = null,
  val yoomoneyOperationId: String? = null,
  val verifiedAmountRub: Long? = null,
  val verificationNote: String? = null,
  val verificationAttempts: Long = 0L,
  @ServerTimestamp val checkoutOpenedAt: Timestamp? = null,
  @ServerTimestamp val verifiedAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class UsedYooMoneyOperation(
  @DocumentId val id: String = "",
  val operationId: String = "",
  val bookingId: String = "",
  val claimedByUid: String = "",
  val amountRub: Long = 0L,
  val wallet: String = YOO_MONEY_WALLET,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class KnowledgeArticle(
  @DocumentId val id: String = "",
  val articleId: String = "",
  val category: String = "general",
  val title: String = "",
  val summary: String = "",
  val keywords: List<String> = emptyList(),
  val priority: Int = 0,
)

object BookingStatus {
  const val AWAITING_PAYMENT = "awaiting_payment"
  const val PENDING_VERIFICATION = "pending_verification"
  const val VERIFIED_PAID = "verified_paid"
  const val VERIFICATION_FAILED = "verification_failed"
  const val CANCELLED = "cancelled"
}

const val YOO_MONEY_WALLET = "4100119599343199"
const val YOO_MONEY_QUICKPAY_URL = "https://yoomoney.ru/quickpay/confirm"
const val YOO_MONEY_PAY_URL = "https://yoomoney.ru/to/4100119599343199"
const val YOO_MONEY_HOME_URL = "https://yoomoney.ru"
const val YOO_MONEY_OAUTH_REDIRECT_URI = "yugdom://yoomoney/oauth"
const val YOO_MONEY_RETURN_URI = "yugdom://yoomoney/return"

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write"),
}

fun handleFirestoreError(
  exception: Throwable,
  operationType: OperationType,
  path: String?,
): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError =
    causes.firstOrNull { it is FirebaseFirestoreException }
      ?: causes.firstOrNull { it !is CancellationException }
      ?: exception

  val auth = FirebaseAuth.getInstance()
  val currentUser = auth.currentUser

  val providerInfoList =
    currentUser?.providerData?.map { provider ->
      JSONObject().apply {
        put("providerId", provider.providerId)
        put("email", provider.email)
      }
    } ?: emptyList()

  val authInfoJson =
    JSONObject().apply {
      put("userId", currentUser?.uid)
      put("email", currentUser?.email)
      put("emailVerified", currentUser?.isEmailVerified)
      put("tenantId", currentUser?.tenantId)
      put("providerInfo", JSONArray(providerInfoList))
    }

  val errorInfoJson =
    JSONObject().apply {
      put("error", rootError.message ?: rootError.toString())
      put("operationType", operationType.value)
      put("path", path)
      put("authInfo", authInfoJson)
    }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}
