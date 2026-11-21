package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.Apartment
import com.example.data.ConsultantRepository
import com.example.data.RegionFilter
import com.example.data.UserDataRepository
import com.example.data.UserProfile
import com.example.data.YooMoneyPaymentManager
import com.example.data.attemptAutoSignIn
import com.example.data.signOut
import com.example.ui.components.YooMoneyPaymentSheet
import com.example.ui.components.openUriInBrowser
import com.example.ui.screens.ApartmentDetailScreen
import com.example.ui.screens.AuthRegistrationScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.ConsultantScreen
import com.example.ui.screens.MapSearchScreen
import com.example.ui.screens.ProfileBookingsScreen
import com.example.ui.theme.YugDomTheme
import com.example.ui.viewmodel.ConsultantViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ProfileLoadState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private enum class MainTab(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  CATALOG("catalog", "Квартиры", Icons.Filled.Apartment, Icons.Outlined.Apartment),
  MAP("map", "Карта", Icons.Filled.Map, Icons.Outlined.Map),
  CONSULTANT("consultant", "Консультант", Icons.Filled.SupportAgent, Icons.Outlined.SupportAgent),
  PROFILE("profile", "Кабинет", Icons.Filled.Person, Icons.Outlined.Person),
}

class MainActivity : ComponentActivity() {
  private val _incomingDeepLink = MutableStateFlow<Uri?>(null)
  private val incomingDeepLink: StateFlow<Uri?> = _incomingDeepLink.asStateFlow()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    _incomingDeepLink.value = intent?.data

    enableEdgeToEdge()
    setContent {
      YugDomTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          AppContent(
            incomingDeepLinkFlow = incomingDeepLink,
            onConsumeDeepLink = { _incomingDeepLink.value = null },
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    intent.data?.let { _incomingDeepLink.value = it }
  }
}

@Composable
fun AppContent(
  incomingDeepLinkFlow: StateFlow<Uri?>,
  onConsumeDeepLink: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
  var pendingPhone by rememberSaveable { mutableStateOf("") }
  var pendingName by rememberSaveable { mutableStateOf("") }
  var pendingRegionCode by rememberSaveable { mutableStateOf(RegionFilter.MELITOPOL.code) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
    Firebase.auth.addAuthStateListener(listener)
    onDispose { Firebase.auth.removeAuthStateListener(listener) }
  }

  LaunchedEffect(Unit) {
    if (Firebase.auth.currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { currentUser = Firebase.auth.currentUser },
        onUnauthenticated = {},
        scope = scope,
      )
    }
  }

  val user = currentUser
  if (user == null) {
    AuthRegistrationScreen(
      isGoogleSignedIn = false,
      initialName = pendingName,
      initialPhone = pendingPhone,
      isSavingProfile = false,
      externalError = null,
      onGoogleAuthSuccess = { phone, name, region ->
        pendingPhone = phone
        pendingName = name
        pendingRegionCode = region.code
        currentUser = Firebase.auth.currentUser
      },
      onSavePhoneRegistration = { _, _, _ -> },
      onSignOut = {},
    )
  } else {
    val uid = user.uid
    key(uid) {
      AuthenticatedAppShell(
        currentUserId = uid,
        defaultDisplayName = user.displayName.orEmpty(),
        pendingPhone = pendingPhone,
        pendingName = pendingName,
        pendingRegion = RegionFilter.fromCode(pendingRegionCode),
        incomingDeepLinkFlow = incomingDeepLinkFlow,
        onConsumeDeepLink = onConsumeDeepLink,
        onClearPendingRegistration = {
          pendingPhone = ""
          pendingName = ""
        },
        onSignOut = {
          pendingPhone = ""
          pendingName = ""
          signOut(credentialManager, scope) {
            currentUser = null
          }
        },
      )
    }
  }
}

@Composable
private fun AuthenticatedAppShell(
  currentUserId: String,
  defaultDisplayName: String,
  pendingPhone: String,
  pendingName: String,
  pendingRegion: RegionFilter,
  incomingDeepLinkFlow: StateFlow<Uri?>,
  onConsumeDeepLink: () -> Unit,
  onClearPendingRegistration: () -> Unit,
  onSignOut: () -> Unit,
) {
  val mainViewModel: MainViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            MainViewModel(
              repository = UserDataRepository(db),
              yooMoneyManager = YooMoneyPaymentManager(app),
              currentUserId = currentUserId,
            )
          }
        },
    )

  val consultantViewModel: ConsultantViewModel =
    viewModel(
      key = "consultant_$currentUserId",
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            ConsultantViewModel(ConsultantRepository(db))
          }
        },
    )

  val profileState by mainViewModel.profileState.collectAsStateWithLifecycle()
  val isSavingProfile by mainViewModel.isSavingProfile.collectAsStateWithLifecycle()
  val actionMessage by mainViewModel.actionMessage.collectAsStateWithLifecycle()
  val pendingDeepLink by incomingDeepLinkFlow.collectAsStateWithLifecycle()

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer =
      LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
          mainViewModel.onAppResumedAfterCheckout()
        }
      }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(pendingDeepLink) {
    val uri = pendingDeepLink
    if (uri != null) {
      mainViewModel.handleIncomingDeepLink(uri)
      onConsumeDeepLink()
    }
  }

  // Auto-complete registration if phone number was entered right before Google Sign-In
  LaunchedEffect(profileState, pendingPhone) {
    if (profileState is ProfileLoadState.NeedsRegistration && pendingPhone.count { it.isDigit() } >= 5) {
      val resolvedName = pendingName.ifBlank { defaultDisplayName.ifBlank { "Клиент ЮгДом" } }
      mainViewModel.saveProfile(resolvedName, pendingPhone, pendingRegion)
      onClearPendingRegistration()
    }
  }

  when (val state = profileState) {
    is ProfileLoadState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          CircularProgressIndicator()
          Text("Загрузка профиля...", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
    is ProfileLoadState.Error -> {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = state.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
          )
          OutlinedButton(onClick = onSignOut) {
            Text("Выйти и попробовать снова")
          }
        }
      }
    }
    is ProfileLoadState.NeedsRegistration -> {
      AuthRegistrationScreen(
        isGoogleSignedIn = true,
        initialName = pendingName.ifBlank { defaultDisplayName },
        initialPhone = pendingPhone,
        isSavingProfile = isSavingProfile,
        externalError = actionMessage,
        onGoogleAuthSuccess = { _, _, _ -> },
        onSavePhoneRegistration = { fullName, phone, region ->
          mainViewModel.saveProfile(fullName.ifBlank { defaultDisplayName }, phone, region)
        },
        onSignOut = onSignOut,
      )
    }
    is ProfileLoadState.Registered -> {
      RegisteredWorkspace(
        profile = state.profile,
        mainViewModel = mainViewModel,
        consultantViewModel = consultantViewModel,
        onSignOut = onSignOut,
      )
    }
  }
}

@Composable
private fun RegisteredWorkspace(
  profile: UserProfile,
  mainViewModel: MainViewModel,
  consultantViewModel: ConsultantViewModel,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  var selectedTab by rememberSaveable { mutableStateOf(MainTab.CATALOG) }
  var detailApartment by remember { mutableStateOf<Apartment?>(null) }
  var paymentSheetApartment by remember { mutableStateOf<Apartment?>(null) }

  val apartments by mainViewModel.filteredApartments.collectAsStateWithLifecycle()
  val filters by mainViewModel.filterState.collectAsStateWithLifecycle()
  val favoriteIds by mainViewModel.favoriteIds.collectAsStateWithLifecycle()
  val favoritesState by mainViewModel.favoritesState.collectAsStateWithLifecycle()
  val bookingsState by mainViewModel.bookingsState.collectAsStateWithLifecycle()
  val activeBookingCount by mainViewModel.activeBookingCount.collectAsStateWithLifecycle()
  val yooMoneyStatus by mainViewModel.yooMoneyMerchantStatus.collectAsStateWithLifecycle()
  val isConnectingMerchant by mainViewModel.isConnectingMerchant.collectAsStateWithLifecycle()
  val verifyingBookingId by mainViewModel.verifyingBookingId.collectAsStateWithLifecycle()
  val antiFraudAlert by mainViewModel.antiFraudAlert.collectAsStateWithLifecycle()
  val isSavingProfile by mainViewModel.isSavingProfile.collectAsStateWithLifecycle()
  val isSubmittingBooking by mainViewModel.isSubmittingBooking.collectAsStateWithLifecycle()
  val actionMessage by mainViewModel.actionMessage.collectAsStateWithLifecycle()

  val consultantMessages by consultantViewModel.messages.collectAsStateWithLifecycle()
  val consultantUiState by consultantViewModel.uiState.collectAsStateWithLifecycle()
  val selectedCategory by consultantViewModel.selectedCategory.collectAsStateWithLifecycle()
  val categoryArticles by consultantViewModel.categoryArticles.collectAsStateWithLifecycle()
  val kbCount by consultantViewModel.kbCount.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(actionMessage) {
    val msg = actionMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      mainViewModel.clearActionMessage()
    }
  }

  val openDetail = detailApartment
  if (openDetail != null) {
    ApartmentDetailScreen(
      apartment = openDetail,
      isFavorite = favoriteIds.contains(openDetail.id),
      onBack = { detailApartment = null },
      onToggleFavorite = { mainViewModel.toggleFavorite(openDetail) },
      onAskAiConditions = {
        detailApartment = null
        selectedTab = MainTab.CONSULTANT
        consultantViewModel.askAboutApartment(openDetail)
      },
      onPayYooMoney = { paymentSheetApartment = openDetail },
    )
  } else {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavigationRail = maxWidth >= 600.dp

      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (!useNavigationRail) {
            NavigationBar {
              MainTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                NavigationBarItem(
                  selected = selected,
                  onClick = { selectedTab = tab },
                  icon = {
                    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
                    if (tab == MainTab.PROFILE && activeBookingCount > 0) {
                      BadgedBox(badge = { Badge { Text("$activeBookingCount") } }) {
                        Icon(icon, contentDescription = tab.label)
                      }
                    } else {
                      Icon(icon, contentDescription = tab.label)
                    }
                  },
                  label = { Text(tab.label) },
                  modifier = Modifier.testTag("nav_tab_${tab.route}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (useNavigationRail) {
            NavigationRail(modifier = Modifier.fillMaxHeight().statusBarsPadding()) {
              Spacer(modifier = Modifier.height(12.dp))
              MainTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                NavigationRailItem(
                  selected = selected,
                  onClick = { selectedTab = tab },
                  icon = {
                    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
                    if (tab == MainTab.PROFILE && activeBookingCount > 0) {
                      BadgedBox(badge = { Badge { Text("$activeBookingCount") } }) {
                        Icon(icon, contentDescription = tab.label)
                      }
                    } else {
                      Icon(icon, contentDescription = tab.label)
                    }
                  },
                  label = { Text(tab.label) },
                  modifier = Modifier.testTag("rail_tab_${tab.route}"),
                )
              }
            }
          }

          Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (selectedTab) {
              MainTab.CATALOG -> {
                CatalogScreen(
                  apartments = apartments,
                  filters = filters,
                  favoriteIds = favoriteIds,
                  onSearchChange = mainViewModel::updateSearchQuery,
                  onSelectRegion = mainViewModel::selectRegion,
                  onSelectDealType = mainViewModel::selectDealType,
                  onSelectRooms = mainViewModel::selectRooms,
                  onResetFilters = mainViewModel::resetFilters,
                  onOpenMap = { selectedTab = MainTab.MAP },
                  onOpenAi = { selectedTab = MainTab.CONSULTANT },
                  onToggleFavorite = mainViewModel::toggleFavorite,
                  onOpenApartment = { detailApartment = it },
                  onPayApartment = { paymentSheetApartment = it },
                )
              }
              MainTab.MAP -> {
                MapSearchScreen(
                  apartments = apartments,
                  filters = filters,
                  onSelectRegion = mainViewModel::selectRegion,
                  onSelectDealType = mainViewModel::selectDealType,
                  onRadiusChange = mainViewModel::updateMapRadius,
                  onSelectPin = mainViewModel::selectMapPin,
                  onOpenApartment = { detailApartment = it },
                  onPayApartment = { paymentSheetApartment = it },
                )
              }
              MainTab.CONSULTANT -> {
                ConsultantScreen(
                  messages = consultantMessages,
                  uiState = consultantUiState,
                  selectedCategory = selectedCategory,
                  categoryArticles = categoryArticles,
                  kbCount = kbCount,
                  onSelectCategory = consultantViewModel::selectCategory,
                  onOpenArticle = consultantViewModel::openArticleFromDatabase,
                  onRefreshKb = consultantViewModel::refreshDatabase,
                  onSendMessage = consultantViewModel::sendMessage,
                  onOpenApartment = { detailApartment = it },
                  onPayApartment = { paymentSheetApartment = it },
                )
              }
              MainTab.PROFILE -> {
                ProfileBookingsScreen(
                  profile = profile,
                  favoritesState = favoritesState,
                  bookingsState = bookingsState,
                  yooMoneyStatus = yooMoneyStatus,
                  isConnectingMerchant = isConnectingMerchant,
                  verifyingBookingId = verifyingBookingId,
                  isSavingProfile = isSavingProfile,
                  onSaveProfile = mainViewModel::saveProfile,
                  onOpenApartment = { detailApartment = it },
                  onPayBookingCheckout = { booking ->
                    mainViewModel.reopenBookingCheckout(booking) { uri ->
                      openUriInBrowser(context, uri)
                    }
                  },
                  onVerifyBookingApi = { booking ->
                    mainViewModel.verifyBookingViaYooMoneyApi(
                      booking = booking,
                      manualOperationId = booking.submittedOperationId,
                    )
                  },
                  onSubmitManualOperationId = mainViewModel::submitManualOperationId,
                  onCancelBooking = mainViewModel::cancelBooking,
                  onStartYooMoneyOAuth = mainViewModel::buildYooMoneyOAuthUri,
                  onConnectYooMoneyCode = mainViewModel::connectYooMoneyOAuth,
                  onDisconnectYooMoneyOAuth = mainViewModel::disconnectYooMoneyOAuth,
                  onSignOut = onSignOut,
                )
              }
            }
          }
        }
      }
    }
  }

  val payApt = paymentSheetApartment
  if (payApt != null) {
    YooMoneyPaymentSheet(
      apartment = payApt,
      isSubmitting = isSubmittingBooking,
      onDismiss = { paymentSheetApartment = null },
      onSubmitAndPay = { amountRub, paymentMethod, comment ->
        mainViewModel.createBookingAndBuildCheckout(
          apartment = payApt,
          amountRub = amountRub,
          paymentMethod = paymentMethod,
          comment = comment,
          onCheckoutReady = { checkoutUri ->
            paymentSheetApartment = null
            selectedTab = MainTab.PROFILE
            openUriInBrowser(context, checkoutUri)
          },
        )
      },
    )
  }

  val activeAlert = antiFraudAlert
  if (activeAlert != null) {
    AlertDialog(
      onDismissRequest = mainViewModel::dismissAntiFraudAlert,
      icon = {
        Icon(
          imageVector =
            if (activeAlert.isSecurityWarning) Icons.Filled.Security
            else Icons.Filled.VerifiedUser,
          contentDescription = null,
          tint =
            if (activeAlert.isSecurityWarning) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.tertiary,
        )
      },
      title = { Text(activeAlert.title) },
      text = { Text(activeAlert.message) },
      confirmButton = {
        Button(onClick = mainViewModel::dismissAntiFraudAlert) {
          Text("Понятно")
        }
      },
    )
  }
}
