package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Apartment
import com.example.data.ApartmentCatalog
import com.example.data.BookingStatus
import com.example.data.DealType
import com.example.data.FavoriteApartment
import com.example.data.PaymentBooking
import com.example.data.RegionFilter
import com.example.data.UserDataRepository
import com.example.data.UserProfile
import com.example.data.YOO_MONEY_WALLET
import com.example.data.YooMoneyCheckOutcome
import com.example.data.YooMoneyMerchantStatus
import com.example.data.YooMoneyPaymentManager
import com.example.data.distanceKm
import com.example.data.formatRub
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ProfileLoadState {
  data object Loading : ProfileLoadState
  data object NeedsRegistration : ProfileLoadState
  data class Registered(val profile: UserProfile) : ProfileLoadState
  data class Error(val message: String) : ProfileLoadState
}

sealed interface DataUiState<out T> {
  data object Loading : DataUiState<Nothing>
  data class Success<T>(val data: T) : DataUiState<T>
  data class Error(val message: String) : DataUiState<Nothing>
}

data class AntiFraudAlert(
  val title: String,
  val message: String,
  val isSecurityWarning: Boolean = true,
)

data class CatalogFilterState(
  val selectedRegion: RegionFilter = RegionFilter.ALL,
  val selectedDealType: DealType? = null,
  val selectedRooms: Int? = null,
  val maxRentRub: Long = 50000L,
  val maxSaleRub: Long = 7000000L,
  val searchQuery: String = "",
  val mapRadiusKm: Float = 80f,
  val selectedMapApartmentId: String? = null,
)

class MainViewModel(
  private val repository: UserDataRepository,
  private val yooMoneyManager: YooMoneyPaymentManager,
  private val currentUserId: String,
) : ViewModel() {

  val profileState: StateFlow<ProfileLoadState> =
    repository
      .observeProfile(currentUserId)
      .map { profile ->
        if (profile == null || profile.phoneNumber.isBlank()) {
          ProfileLoadState.NeedsRegistration
        } else {
          ProfileLoadState.Registered(profile)
        }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing profile", rootError)
        emit(ProfileLoadState.Error(rootError.localizedMessage ?: "Ошибка загрузки профиля"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ProfileLoadState.Loading,
      )

  val favoritesState: StateFlow<DataUiState<List<FavoriteApartment>>> =
    repository
      .observeFavorites(currentUserId)
      .map<List<FavoriteApartment>, DataUiState<List<FavoriteApartment>>> { DataUiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing favorites", rootError)
        emit(DataUiState.Error(rootError.localizedMessage ?: "Не удалось загрузить избранное"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DataUiState.Loading,
      )

  val bookingsState: StateFlow<DataUiState<List<PaymentBooking>>> =
    repository
      .observeBookings(currentUserId)
      .map<List<PaymentBooking>, DataUiState<List<PaymentBooking>>> { DataUiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing bookings", rootError)
        emit(DataUiState.Error(rootError.localizedMessage ?: "Не удалось загрузить заявки"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DataUiState.Loading,
      )

  private val _filterState = MutableStateFlow(CatalogFilterState())
  val filterState: StateFlow<CatalogFilterState> = _filterState.asStateFlow()

  val filteredApartments: StateFlow<List<Apartment>> =
    _filterState
      .map { filters -> applyFilters(ApartmentCatalog.listings, filters) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ApartmentCatalog.listings,
      )

  val favoriteIds: StateFlow<Set<String>> =
    favoritesState
      .map { state ->
        if (state is DataUiState.Success) {
          state.data.map { it.apartmentId }.toSet()
        } else {
          emptySet()
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptySet(),
      )

  val activeBookingCount: StateFlow<Int> =
    bookingsState
      .map { state ->
        if (state is DataUiState.Success) {
          state.data.count {
            it.status == BookingStatus.AWAITING_PAYMENT ||
              it.status == BookingStatus.PENDING_VERIFICATION ||
              it.status == BookingStatus.VERIFICATION_FAILED
          }
        } else {
          0
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = 0,
      )

  private val _yooMoneyMerchantStatus = MutableStateFlow(yooMoneyManager.getMerchantStatus())
  val yooMoneyMerchantStatus: StateFlow<YooMoneyMerchantStatus> =
    _yooMoneyMerchantStatus.asStateFlow()

  private val _isConnectingMerchant = MutableStateFlow(false)
  val isConnectingMerchant: StateFlow<Boolean> = _isConnectingMerchant.asStateFlow()

  private val _verifyingBookingId = MutableStateFlow<String?>(null)
  val verifyingBookingId: StateFlow<String?> = _verifyingBookingId.asStateFlow()

  private val _antiFraudAlert = MutableStateFlow<AntiFraudAlert?>(null)
  val antiFraudAlert: StateFlow<AntiFraudAlert?> = _antiFraudAlert.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _isSavingProfile = MutableStateFlow(false)
  val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

  private val _isSubmittingBooking = MutableStateFlow(false)
  val isSubmittingBooking: StateFlow<Boolean> = _isSubmittingBooking.asStateFlow()

  private var pendingCheckoutBookingId: String? = null

  fun selectRegion(region: RegionFilter) {
    _filterState.update { state ->
      val currentSelected = ApartmentCatalog.listings.firstOrNull { it.id == state.selectedMapApartmentId }
      val keepPin = currentSelected != null && (region == RegionFilter.ALL || currentSelected.region == region)
      state.copy(
        selectedRegion = region,
        selectedMapApartmentId = if (keepPin) state.selectedMapApartmentId else null,
      )
    }
  }

  fun selectDealType(dealType: DealType?) {
    _filterState.update { it.copy(selectedDealType = dealType) }
  }

  fun selectRooms(rooms: Int?) {
    _filterState.update { it.copy(selectedRooms = rooms) }
  }

  fun updateSearchQuery(query: String) {
    _filterState.update { it.copy(searchQuery = query) }
  }

  fun updateMapRadius(radiusKm: Float) {
    _filterState.update { it.copy(mapRadiusKm = radiusKm) }
  }

  fun selectMapPin(apartmentId: String?) {
    _filterState.update { it.copy(selectedMapApartmentId = apartmentId) }
  }

  fun resetFilters() {
    _filterState.value = CatalogFilterState()
  }

  fun clearActionMessage() {
    _actionMessage.value = null
  }

  fun dismissAntiFraudAlert() {
    _antiFraudAlert.value = null
  }

  fun saveProfile(fullName: String, phoneNumber: String, preferredRegion: RegionFilter) {
    val cleanPhone = phoneNumber.trim()
    if (cleanPhone.length < 5) {
      _actionMessage.value = "Введите корректный номер телефона"
      return
    }
    viewModelScope.launch {
      _isSavingProfile.value = true
      val result =
        repository.saveOrUpdateProfile(
          userId = currentUserId,
          fullName = fullName,
          phoneNumber = cleanPhone,
          preferredRegion = preferredRegion.code,
        )
      _isSavingProfile.value = false
      result.fold(
        onSuccess = {
          if (preferredRegion != RegionFilter.ALL) {
            selectRegion(preferredRegion)
          }
          _actionMessage.value = "Профиль сохранён"
        },
        onFailure = { error ->
          _actionMessage.value = error.localizedMessage ?: "Не удалось сохранить профиль"
        },
      )
    }
  }

  fun toggleFavorite(apartment: Apartment) {
    val isCurrentlyFav = favoriteIds.value.contains(apartment.id)
    viewModelScope.launch {
      val result = repository.toggleFavorite(currentUserId, apartment, isCurrentlyFav)
      result.onFailure { error ->
        _actionMessage.value = error.localizedMessage ?: "Не удалось обновить избранное"
      }
    }
  }

  fun createBookingAndBuildCheckout(
    apartment: Apartment,
    amountRub: Long,
    paymentMethod: String,
    comment: String?,
    onCheckoutReady: (Uri) -> Unit,
  ) {
    viewModelScope.launch {
      _isSubmittingBooking.value = true
      val result =
        repository.createBooking(
          userId = currentUserId,
          apartment = apartment,
          amountRub = amountRub,
          paymentMethod = paymentMethod,
          comment = comment,
        )
      _isSubmittingBooking.value = false
      result.fold(
        onSuccess = { booking ->
          pendingCheckoutBookingId = booking.id
          val checkoutUri = yooMoneyManager.buildQuickpayUri(booking)
          _actionMessage.value =
            "Создана заявка ${booking.paymentLabel}. Завершите перевод в ЮMoney."
          onCheckoutReady(checkoutUri)
        },
        onFailure = { error ->
          _actionMessage.value = error.localizedMessage ?: "Ошибка при создании заявки"
        },
      )
    }
  }

  fun reopenBookingCheckout(booking: PaymentBooking, onCheckoutReady: (Uri) -> Unit) {
    pendingCheckoutBookingId = booking.id
    viewModelScope.launch {
      repository.markCheckoutOpened(currentUserId, booking.id)
    }
    onCheckoutReady(yooMoneyManager.buildQuickpayUri(booking))
  }

  fun handleIncomingDeepLink(uri: Uri) {
    if (uri.scheme != "yugdom" || uri.host != "yoomoney") return
    when (uri.path) {
      "/oauth" -> {
        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
          _actionMessage.value = "Авторизация ЮMoney отменена: $error"
          return
        }
        val code = uri.getQueryParameter("code")
        if (!code.isNullOrBlank()) {
          connectYooMoneyOAuth(code)
        }
      }
      "/return" -> {
        val label = uri.getQueryParameter("label")
        onReturnedFromBrowserCheckout(label)
      }
    }
  }

  fun onAppResumedAfterCheckout() {
    val activeId = pendingCheckoutBookingId ?: return
    pendingCheckoutBookingId = null
    val list = (bookingsState.value as? DataUiState.Success)?.data.orEmpty()
    val booking = list.firstOrNull { it.id == activeId }
    if (booking != null && booking.status != BookingStatus.VERIFIED_PAID && booking.status != BookingStatus.CANCELLED) {
      onReturnedFromBrowserCheckout(booking.paymentLabel)
    }
  }

  private fun onReturnedFromBrowserCheckout(label: String?) {
    pendingCheckoutBookingId = null
    val list = (bookingsState.value as? DataUiState.Success)?.data.orEmpty()
    val booking =
      if (!label.isNullOrBlank()) list.firstOrNull { it.paymentLabel == label }
      else list.firstOrNull { it.status == BookingStatus.AWAITING_PAYMENT }

    if (booking != null && yooMoneyManager.getMerchantStatus().isConnected) {
      verifyBookingViaYooMoneyApi(booking, manualOperationId = null)
    } else {
      val labelNote = booking?.paymentLabel ?: label.orEmpty()
      _antiFraudAlert.value =
        AntiFraudAlert(
          title = "Защита от ложного подтверждения",
          message =
            "Возврат с сайта ЮMoney не означает, что деньги поступили на кошелёк $YOO_MONEY_WALLET.\n\n" +
              (if (labelNote.isNotEmpty()) "Метка заявки: $labelNote.\n" else "") +
              "Статус заявки остаётся «Ожидает оплаты». Чтобы подтвердить бронь, проверьте транзакцию через OAuth2 API ЮMoney или отправьте номер операции из чека.",
          isSecurityWarning = true,
        )
    }
  }

  fun verifyBookingViaYooMoneyApi(
    booking: PaymentBooking,
    manualOperationId: String?,
  ) {
    if (_verifyingBookingId.value != null) return
    viewModelScope.launch {
      _verifyingBookingId.value = booking.id
      val outcome =
        yooMoneyManager.verifyPayment(
          booking = booking,
          manualOperationId = manualOperationId,
          isOperationAlreadyUsed = { opId -> repository.isOperationAlreadyUsed(opId) },
        )
      when (outcome) {
        is YooMoneyCheckOutcome.Verified -> {
          val note =
            "Подтверждено API ЮMoney (${outcome.operationId}) · ${formatRub(outcome.verifiedAmountRub)} ₽"
          val saveResult =
            repository.confirmVerifiedPaymentAtomic(
              userId = currentUserId,
              booking = booking,
              operationId = outcome.operationId,
              verifiedAmountRub = outcome.verifiedAmountRub,
              note = note,
            )
          saveResult.fold(
            onSuccess = {
              _antiFraudAlert.value =
                AntiFraudAlert(
                  title = "Оплата подтверждена ЮMoney",
                  message =
                    "Зачисление ${formatRub(outcome.verifiedAmountRub)} ₽ на кошелёк $YOO_MONEY_WALLET подтверждено.\nID операции: ${outcome.operationId}",
                  isSecurityWarning = false,
                )
            },
            onFailure = { err ->
              _antiFraudAlert.value =
                AntiFraudAlert(
                  title = "Блокировка подтверждения",
                  message = err.localizedMessage ?: "Не удалось зафиксировать оплату.",
                  isSecurityWarning = true,
                )
            },
          )
        }
        is YooMoneyCheckOutcome.Rejected -> {
          repository.recordVerificationFailure(
            userId = currentUserId,
            bookingId = booking.id,
            currentAttempts = booking.verificationAttempts,
            reason = outcome.reason,
          )
          _antiFraudAlert.value =
            AntiFraudAlert(
              title = "Проверка отклонена системой защиты",
              message = outcome.reason,
              isSecurityWarning = true,
            )
        }
        is YooMoneyCheckOutcome.NotFoundYet -> {
          _antiFraudAlert.value =
            AntiFraudAlert(
              title = "Платёж не найден в ЮMoney",
              message = outcome.message,
              isSecurityWarning = true,
            )
        }
        is YooMoneyCheckOutcome.MerchantAuthRequired -> {
          _antiFraudAlert.value =
            AntiFraudAlert(
              title = "Требуется OAuth2-проверка кошелька",
              message = outcome.message,
              isSecurityWarning = true,
            )
        }
      }
      _verifyingBookingId.value = null
    }
  }

  fun submitManualOperationId(booking: PaymentBooking, operationId: String) {
    val cleanOp = operationId.trim()
    viewModelScope.launch {
      _verifyingBookingId.value = booking.id
      val res = repository.submitReceiptForVerification(currentUserId, booking.id, cleanOp)
      res.fold(
        onSuccess = {
          if (yooMoneyManager.getMerchantStatus().isConnected) {
            _verifyingBookingId.value = null
            verifyBookingViaYooMoneyApi(
              booking = booking.copy(submittedOperationId = cleanOp),
              manualOperationId = cleanOp,
            )
          } else {
            _verifyingBookingId.value = null
            _actionMessage.value =
              "Чек $cleanOp отправлен на проверку. Оплата НЕ подтверждена до сверки с ЮMoney."
          }
        },
        onFailure = { err ->
          _verifyingBookingId.value = null
          _antiFraudAlert.value =
            AntiFraudAlert(
              title = "Отказ в приёме чека",
              message = err.localizedMessage ?: "Некорректный или уже использованный номер операции.",
              isSecurityWarning = true,
            )
        },
      )
    }
  }

  fun buildYooMoneyOAuthUri(): Result<Uri> = yooMoneyManager.buildOAuthAuthorizeUri()

  fun connectYooMoneyOAuth(codeOrUrl: String) {
    viewModelScope.launch {
      _isConnectingMerchant.value = true
      val res = yooMoneyManager.connectWithOAuthCodeOrUrl(codeOrUrl)
      _isConnectingMerchant.value = false
      res.fold(
        onSuccess = { status ->
          _yooMoneyMerchantStatus.value = status
          _actionMessage.value =
            "OAuth2 ЮMoney подключён для кошелька ${status.connectedWallet}"
        },
        onFailure = { err ->
          _antiFraudAlert.value =
            AntiFraudAlert(
              title = "Ошибка подключения OAuth2 ЮMoney",
              message = err.localizedMessage ?: "Не удалось получить токен ЮMoney.",
              isSecurityWarning = true,
            )
        },
      )
    }
  }

  fun disconnectYooMoneyOAuth() {
    yooMoneyManager.disconnectMerchant()
    _yooMoneyMerchantStatus.value = yooMoneyManager.getMerchantStatus()
    _actionMessage.value = "OAuth2-подключение ЮMoney отключено"
  }

  fun cancelBooking(bookingId: String) {
    viewModelScope.launch {
      val result = repository.cancelBooking(currentUserId, bookingId)
      result.fold(
        onSuccess = { _actionMessage.value = "Заявка отменена" },
        onFailure = { error ->
          _actionMessage.value = error.localizedMessage ?: "Не удалось отменить заявку"
        },
      )
    }
  }

  private fun applyFilters(
    all: List<Apartment>,
    filters: CatalogFilterState,
  ): List<Apartment> {
    return all.filter { apt ->
      val matchesRegion =
        filters.selectedRegion == RegionFilter.ALL || apt.region == filters.selectedRegion
      val matchesDeal =
        filters.selectedDealType == null || apt.dealType == filters.selectedDealType
      val matchesRooms =
        filters.selectedRooms == null ||
          (if (filters.selectedRooms >= 3) apt.rooms >= 3 else apt.rooms == filters.selectedRooms)
      val matchesPrice =
        when (apt.dealType) {
          DealType.RENT -> apt.priceRub <= filters.maxRentRub
          DealType.SALE -> apt.priceRub <= filters.maxSaleRub
        }
      val query = filters.searchQuery.trim().lowercase()
      val matchesQuery =
        query.isEmpty() ||
          apt.title.lowercase().contains(query) ||
          apt.city.lowercase().contains(query) ||
          apt.district.lowercase().contains(query) ||
          apt.address.lowercase().contains(query)
      val matchesRadius =
        if (filters.selectedRegion == RegionFilter.ALL || filters.mapRadiusKm >= 150f) {
          true
        } else {
          distanceKm(
            filters.selectedRegion.hubLat,
            filters.selectedRegion.hubLon,
            apt.latitude,
            apt.longitude,
          ) <= filters.mapRadiusKm
        }
      matchesRegion && matchesDeal && matchesRooms && matchesPrice && matchesQuery && matchesRadius
    }
  }

  private companion object {
    const val TAG = "MainViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
