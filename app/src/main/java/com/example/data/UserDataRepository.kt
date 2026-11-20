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
    comment: String?,
  ): Result<String> {
    var docPath = "users/$userId/bookings"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Отказ в создании брони для другого пользователя." }
      require(amountRub > 0L) { "Сумма оплаты должна быть больше 0 ₽." }

      val docRef = db.collection("users").document(uid).collection("bookings").document()
      docPath = docRef.path
      val cleanComment = comment?.trim()?.take(300)?.takeIf { it.isNotEmpty() }

      val payload =
        mapOf(
          "userId" to uid,
          "apartmentId" to apartment.id,
          "apartmentTitle" to apartment.title.take(120),
          "dealType" to apartment.dealType.code,
          "amountRub" to amountRub,
          "yoomoneyWallet" to YOO_MONEY_WALLET,
          "status" to "pending_payment",
          "comment" to cleanComment,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }

      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateBookingStatus(
    userId: String,
    bookingId: String,
    newStatus: String,
  ): Result<Unit> {
    val path = "users/$userId/bookings/$bookingId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Отказ в изменении чужой заявки." }
      require(newStatus in listOf("confirmed", "cancelled")) { "Недопустимый статус заявки." }
      db.collection("users")
        .document(uid)
        .collection("bookings")
        .document(bookingId)
        .update(
          mapOf(
            "status" to newStatus,
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
}
