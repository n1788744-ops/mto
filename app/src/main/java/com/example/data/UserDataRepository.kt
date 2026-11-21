package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserDataRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("Пользователь должен войти через Google перед записью данных.")

  fun observeProfile(userId: String): Flow<UserProfile?> =
    db.collection("users")
      .document(userId)
      .snapshots()
      .map { snapshot ->
        if (!snapshot.exists()) {
          null
        } else {
          snapshot.toObject(
            UserProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
      }

  suspend fun saveOrUpdateProfile(
    userId: String,
    fullName: String,
    phoneNumber: String,
    preferredRegion: String,
  ): Result<Unit> {
    val path = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Идентификатор пользователя не совпадает." }
      val trimmedName = fullName.trim().ifBlank { "Пользователь" }.take(80)
      val trimmedPhone = phoneNumber.trim().take(25)
      require(trimmedPhone.length >= 5) { "Введите корректный номер телефона (минимум 5 символов)." }
      val regionCode =
        if (preferredRegion in listOf("all", "melitopol", "kherson", "zaporizhzhia")) {
          preferredRegion
        } else {
          "all"
        }

      val docRef = db.collection("users").document(uid)
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef
          .update(
            mapOf(
              "fullName" to trimmedName,
              "phoneNumber" to trimmedPhone,
              "preferredRegion" to regionCode,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "fullName" to trimmedName,
              "phoneNumber" to trimmedPhone,
              "preferredRegion" to regionCode,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeFavorites(userId: String): Flow<List<FavoriteApartment>> =
    db.collection("users")
      .document(userId)
      .collection("favorites")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          FavoriteApartment::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      }

  suspend fun toggleFavorite(
    userId: String,
    apartment: Apartment,
    isCurrentlyFavorite: Boolean,
  ): Result<Unit> {
    val path = "users/$userId/favorites/${apartment.id}"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Отказ в доступе к чужому избранному." }
      val docRef =
        db.collection("users").document(uid).collection("favorites").document(apartment.id)
      if (isCurrentlyFavorite) {
        docRef.delete().await()
      } else {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "apartmentId" to apartment.id,
              "apartmentTitle" to apartment.title.take(120),
              "region" to apartment.region.code,
              "dealType" to apartment.dealType.code,
              "priceRub" to apartment.priceRub,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (isCurrentlyFavorite) OperationType.DELETE else OperationType.CREATE,
        path,
      )
      Result.failure(e)
    }
  }

  fun observeBookings(userId: String): Flow<List<PaymentBooking>> =
    db.collection("users")
      .document(userId)
      .collection("bookings")
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            PaymentBooking::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }

  suspend fun createBooking(
    userId: String,
    apartment: Apartment,
    amountRub: Long,
    paymentMethod: String,
    comment: String?,
  ): Result<PaymentBooking> {
    var docPath = "users/$userId/bookings"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Отказ в создании брони для другого пользователя." }
      require(amountRub >= apartment.depositOrBookingRub) {
        "Минимальная сумма бронирования: ${formatRub(apartment.depositOrBookingRub)} ₽."
      }
      val resolvedMethod = if (paymentMethod == "PC") "PC" else "AC"

      val docRef = db.collection("users").document(uid).collection("bookings").document()
      docPath = docRef.path
      val cleanComment = comment?.trim()?.take(300)?.takeIf { it.isNotEmpty() }
      val paymentLabel = generatePaymentLabel(uid)

      val payload =
        mapOf(
          "userId" to uid,
          "apartmentId" to apartment.id,
          "apartmentTitle" to apartment.title.take(120),
          "dealType" to apartment.dealType.code,
          "amountRub" to amountRub,
          "yoomoneyWallet" to YOO_MONEY_WALLET,
          "paymentLabel" to paymentLabel,
          "paymentMethod" to resolvedMethod,
          "status" to BookingStatus.AWAITING_PAYMENT,
          "comment" to cleanComment,
          "verificationAttempts" to 0L,
          "checkoutOpenedAt" to FieldValue.serverTimestamp(),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }

      docRef.set(payload).await()

      val createdBooking =
        PaymentBooking(
          id = docRef.id,
          userId = uid,
          apartmentId = apartment.id,
          apartmentTitle = apartment.title.take(120),
          dealType = apartment.dealType.code,
          amountRub = amountRub,
          yoomoneyWallet = YOO_MONEY_WALLET,
          paymentLabel = paymentLabel,
          paymentMethod = resolvedMethod,
          status = BookingStatus.AWAITING_PAYMENT,
          comment = cleanComment,
          verificationAttempts = 0L,
        )
      Result.success(createdBooking)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun markCheckoutOpened(userId: String, bookingId: String): Result<Unit> {
    val path = "users/$userId/bookings/$bookingId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Отказ в доступе к чужой заявке." }
      db.collection("users")
        .document(uid)
        .collection("bookings")
        .document(bookingId)
        .update(
          mapOf(
            "checkoutOpenedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun isOperationAlreadyUsed(operationId: String): Boolean {
    val cleanId = operationId.trim()
    if (cleanId.isEmpty()) return false
    return try {
      db.collection("used_operations").document(cleanId).get().await().exists()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "used_operations/$cleanId")
      false
    }
  }

  suspend fun submitReceiptForVerification(
    userId: String,
    bookingId: String,
    operationId: String,
  ): Result<Unit> {
    val path = "users/$userId/bookings/$bookingId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Отказ в изменении чужой заявки." }
      val cleanOp = operationId.trim().take(64)
      require(cleanOp.length >= 6 && cleanOp.matches(Regex("^[a-zA-Z0-9_.\\-]+$"))) {
        "Введите корректный номер операции ЮMoney (минимум 6 символов: буквы, цифры, - или _)."
      }
      if (isOperationAlreadyUsed(cleanOp)) {
        return Result.failure(
          SecurityException(
            "Этот номер операции ($cleanOp) уже использовался ранее для другой брони. Повторное использование запрещено."
          )
        )
      }

      db.collection("users")
        .document(uid)
        .collection("bookings")
        .document(bookingId)
        .update(
          mapOf(
            "status" to BookingStatus.PENDING_VERIFICATION,
            "submittedOperationId" to cleanOp,
            "verificationNote" to
              "Чек $cleanOp отправлен на проверку. Бронь подтвердится только после сверки с ЮMoney.",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun recordVerificationFailure(
    userId: String,
    bookingId: String,
    currentAttempts: Long,
    reason: String,
  ): Result<Unit> {
    val path = "users/$userId/bookings/$bookingId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Отказ в изменении чужой заявки." }
      db.collection("users")
        .document(uid)
        .collection("bookings")
        .document(bookingId)
        .update(
          mapOf(
            "status" to BookingStatus.VERIFICATION_FAILED,
            "verificationAttempts" to (currentAttempts + 1L),
            "verificationNote" to reason.take(300),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  /**
   * Atomically locks the verified YooMoney operation_id in `/used_operations/{operationId}`
   * and transitions the booking to `verified_paid`. Prevents replay fraud across bookings/users.
   */
  suspend fun confirmVerifiedPaymentAtomic(
    userId: String,
    booking: PaymentBooking,
    operationId: String,
    verifiedAmountRub: Long,
    note: String,
  ): Result<Unit> {
    val path = "users/$userId/bookings/${booking.id}"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Отказ в подтверждении чужой заявки." }
      val cleanOp = operationId.trim()
      require(cleanOp.length >= 6 && cleanOp.matches(Regex("^[a-zA-Z0-9_.\\-]+$"))) {
        "Некорректный ID операции ЮMoney."
      }
      require(verifiedAmountRub >= booking.amountRub) {
        "Сумма транзакции ($verifiedAmountRub ₽) меньше суммы заявки (${booking.amountRub} ₽)."
      }
      if (isOperationAlreadyUsed(cleanOp)) {
        return Result.failure(
          SecurityException("Операция $cleanOp уже была использована ранее (блокировка повтора).")
        )
      }

      val bookingRef = db.collection("users").document(uid).collection("bookings").document(booking.id)
      val usedOpRef = db.collection("used_operations").document(cleanOp)

      db.batch()
        .set(
          usedOpRef,
          mapOf(
            "operationId" to cleanOp,
            "bookingId" to booking.id,
            "claimedByUid" to uid,
            "amountRub" to verifiedAmountRub,
            "wallet" to YOO_MONEY_WALLET,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        .update(
          bookingRef,
          mapOf(
            "status" to BookingStatus.VERIFIED_PAID,
            "yoomoneyOperationId" to cleanOp,
            "verifiedAmountRub" to verifiedAmountRub,
            "verificationNote" to note.take(300),
            "verifiedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        .commit()
        .await()

      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun cancelBooking(userId: String, bookingId: String): Result<Unit> {
    val path = "users/$userId/bookings/$bookingId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Отказ в изменении чужой заявки." }
      db.collection("users")
        .document(uid)
        .collection("bookings")
        .document(bookingId)
        .update(
          mapOf(
            "status" to BookingStatus.CANCELLED,
            "verificationNote" to "Заявка отменена пользователем",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  private fun generatePaymentLabel(uid: String): String {
    val shortUid = uid.filter { it.isLetterOrDigit() }.take(6).ifEmpty { "USER" }
    val nonce = UUID.randomUUID().toString().replace("-", "").take(10).uppercase()
    return "YD_${shortUid}_$nonce"
  }
}
