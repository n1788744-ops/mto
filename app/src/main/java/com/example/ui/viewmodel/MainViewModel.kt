package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Apartment
import com.example.data.ApartmentCatalog
import com.example.data.DealType
import com.example.data.FavoriteApartment
import com.example.data.PaymentBooking
import com.example.data.RegionFilter
import com.example.data.UserDataRepository
import com.example.data.UserProfile
import com.example.data.distanceKm
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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

data class CatalogFilterState(
  val selectedRegion: RegionFilter = RegionFilter.ALL,
  val selectedDealType: DealType? = null,
  val selectedRooms: Int? = null, // null = any, 0 = studio, 1, 2, 3
  val maxRentRub: Long = 50000L,
  val maxSaleRub: Long = 7000000L,
  val searchQuery: String = "",
  val mapRadiusKm: Float = 80f,
  val selectedMapApartmentId: String? = null,
)

class MainViewModel(
  private val repository: UserDataRepository,
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
          state.data.count { it.status == "pending_payment" }
        } else {
          0
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = 0,
      )

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _isSavingProfile = MutableStateFlow(false)
  val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

  private val _isSubmittingBooking = MutableStateFlow(false)
  val isSubmittingBooking: StateFlow<Boolean> = _isSubmittingBooking.asStateFlow()

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

  fun updateMaxRent(maxRent: Long) {
    _filterState.update { it.copy(maxRentRub = maxRent) }
  }

  fun updateMaxSale(maxSale: Long) {
    _filterState.update { it.copy(maxSaleRub = maxSale) }
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

  fun createBooking(
    apartment: Apartment,
    amountRub: Long,
    comment: String?,
    onCreated: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isSubmittingBooking.value = true
      val result = repository.createBooking(currentUserId, apartment, amountRub, comment)
      _isSubmittingBooking.value = false
      result.fold(
        onSuccess = {
          _actionMessage.value = "Заявка создана. Перейдите в ЮMoney для оплаты на счёт 4100119599343199."
          onCreated()
        },
        onFailure = { error ->
          _actionMessage.value = error.localizedMessage ?: "Ошибка при создании заявки"
        },
      )
    }
  }

  fun confirmBookingPayment(bookingId: String) {
    viewModelScope.launch {
      val result = repository.updateBookingStatus(currentUserId, bookingId, "confirmed")
      result.fold(
        onSuccess = { _actionMessage.value = "Оплата отмечена как выполненная" },
        onFailure = { error ->
          _actionMessage.value = error.localizedMessage ?: "Не удалось обновить статус"
        },
      )
    }
  }

  fun cancelBooking(bookingId: String) {
    viewModelScope.launch {
      val result = repository.updateBookingStatus(currentUserId, bookingId, "cancelled")
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
