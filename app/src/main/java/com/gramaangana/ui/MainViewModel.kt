package com.gramaangana.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gramaangana.data.model.Booking
import com.gramaangana.data.model.MaintenanceItem
import com.gramaangana.data.model.User
import com.gramaangana.data.repository.BookingRepository
import com.gramaangana.data.repository.MaintenanceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class MainViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val bookingRepository = BookingRepository(db)
    private val maintenanceRepository = MaintenanceRepository(db)

    // Auth State
    val isLoggedIn = mutableStateOf(auth.currentUser != null)
    val isAuthenticating = mutableStateOf(false)
    val currentUserProfile = mutableStateOf<User?>(null)
    val isPanchayatUser = mutableStateOf(false)

    // UI State
    val selectedTab = mutableStateOf(0)
    val isAdminMode = mutableStateOf(false)
    val showBookingSheet = mutableStateOf(false)
    val showPledgeSheet = mutableStateOf(false)
    val showPaymentDemo = mutableStateOf(false)
    
    val pendingContributionItem = mutableStateOf<String?>(null)
    val pendingContributionAmount = mutableStateOf(0.0)

    private val calendar = Calendar.getInstance()
    val currentMonth = mutableStateOf(calendar.get(Calendar.MONTH) + 1)
    val currentYear = mutableStateOf(calendar.get(Calendar.YEAR))

    private val _selectedDate = MutableStateFlow("${calendar.get(Calendar.YEAR)}-${"%02d".format(calendar.get(Calendar.MONTH) + 1)}-${"%02d".format(calendar.get(Calendar.DAY_OF_MONTH))}")
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val bookingsForDate: StateFlow<List<Booking>> = _selectedDate
        .flatMapLatest { date ->
            bookingRepository.getBookingsForDate(date)
        }
        .catch { e -> _uiMessage.value = "Booking Error: ${e.message}" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRequests: StateFlow<List<Booking>> = bookingRepository.getAllPendingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceItems: StateFlow<List<MaintenanceItem>> = maintenanceRepository.getMaintenanceItems()
        .catch { e -> _uiMessage.value = "Maintenance Error: ${e.message}" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthAvailability: StateFlow<Map<String, String>> = _selectedDate
        .map { it.substringBeforeLast("-") }
        .distinctUntilChanged()
        .flatMapLatest { yearMonth ->
            bookingRepository.getMonthAvailability(yearMonth)
        }
        .catch { e -> _uiMessage.value = "Calendar Error: ${e.message}" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    init {
        auth.currentUser?.let { fetchUserProfile(it.uid) }
    }

    // Auth Logic
    fun register(email: String, pass: String, name: String) {
        if (email.isBlank() || pass.isBlank() || name.isBlank()) {
            _uiMessage.value = "Please fill all fields."
            return
        }
        isAuthenticating.value = true
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val user = User(uid = result.user!!.uid, name = name, email = email, role = "VILLAGER")
                db.collection("users").document(user.uid).set(user).await()
                fetchUserProfile(user.uid)
                isLoggedIn.value = true
                _uiMessage.value = "Registration successful!"
            } catch (e: Exception) {
                if (e.message?.contains("CONFIGURATION_NOT_FOUND") == true) {
                    _uiMessage.value = "Auth Error: Please enable Email/Password in Firebase Console."
                } else {
                    _uiMessage.value = "Error: ${e.message}"
                }
            } finally {
                isAuthenticating.value = false
            }
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiMessage.value = "Email and password required."
            return
        }
        isAuthenticating.value = true
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                fetchUserProfile(result.user!!.uid)
                isLoggedIn.value = true
                _uiMessage.value = "Welcome back!"
            } catch (e: Exception) {
                if (e.message?.contains("CONFIGURATION_NOT_FOUND") == true) {
                    _uiMessage.value = "Auth Error: Please enable Email/Password in Firebase Console."
                } else {
                    _uiMessage.value = "Login failed: ${e.message}"
                }
            } finally {
                isAuthenticating.value = false
            }
        }
    }

    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                val profile = doc.toObject(User::class.java)
                currentUserProfile.value = profile
                // Admin Account Check (Industry standard + User requested official admin)
                val userEmail = auth.currentUser?.email
                isPanchayatUser.value = profile?.role == "PANCHAYAT" || userEmail == "admin@gramaangana.com"
            } catch (e: Exception) {
                _uiMessage.value = "Profile error: ${e.message}"
            }
        }
    }

    fun logout() {
        auth.signOut()
        isLoggedIn.value = false
        currentUserProfile.value = null
        isPanchayatUser.value = false
        isAdminMode.value = false
        _uiMessage.value = "Logged out safely."
    }

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun changeMonth(delta: Int) {
        var newMonth = currentMonth.value + delta
        var newYear = currentYear.value
        if (newMonth > 12) { newMonth = 1; newYear++ }
        if (newMonth < 1) { newMonth = 12; newYear-- }
        currentMonth.value = newMonth
        currentYear.value = newYear
        setDate("$newYear-${"%02d".format(newMonth)}-01")
    }

    fun requestBooking(purpose: String, startTime: Int, endTime: Int) {
        if (purpose.isBlank()) {
            _uiMessage.value = "Please state the purpose."
            return
        }
        viewModelScope.launch {
            val bookingId = db.collection("bookings").document().id
            val booking = Booking(
                id = bookingId,
                userId = currentUserProfile.value?.name ?: "Unknown",
                purpose = purpose,
                date = _selectedDate.value,
                startTime = startTime,
                endTime = endTime,
                status = "PENDING"
            )
            val result = bookingRepository.requestBookingTransaction(booking)
            _uiMessage.value = result.getOrNull() ?: result.exceptionOrNull()?.message
        }
    }

    fun cancelBooking(bookingId: String, date: String) {
        viewModelScope.launch {
            updateBookingStatus(bookingId, date, "CANCELLED")
        }
    }

    fun initiateContribution(itemName: String, amount: Double) {
        if (amount <= 0) {
            _uiMessage.value = "Invalid contribution amount."
            return
        }
        pendingContributionItem.value = itemName
        pendingContributionAmount.value = amount
        showPaymentDemo.value = true
    }

    fun confirmContribution() {
        val itemName = pendingContributionItem.value ?: return
        val amount = pendingContributionAmount.value
        viewModelScope.launch {
            val result = maintenanceRepository.contributeToItem(itemName, amount)
            if (result.isSuccess) {
                _uiMessage.value = "Transaction complete! ₹$amount added."
                showPaymentDemo.value = false
            } else {
                _uiMessage.value = "Payment failed. Please retry."
            }
        }
    }

    fun addMaintenanceItem(name: String, target: Double, desc: String) {
        if (name.isBlank() || target <= 0) {
            _uiMessage.value = "Valid name and target required."
            return
        }
        viewModelScope.launch {
            val item = MaintenanceItem(
                id = (0..1000000).random(),
                itemName = name,
                description = desc,
                targetAmount = target,
                collectedAmount = 0.0,
                status = "NEEDED"
            )
            val result = maintenanceRepository.addMaintenanceItem(item)
            _uiMessage.value = if (result.isSuccess) "New pledge jar created!" else "Failed to create jar."
        }
    }

    fun closePledge(itemName: String) {
        viewModelScope.launch {
            val result = maintenanceRepository.closeMaintenanceItem(itemName)
            _uiMessage.value = if (result.isSuccess) "Project marked as closed." else "Error closing project."
        }
    }

    fun updateBookingStatus(bookingId: String, date: String, status: String) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("daily_schedules").document(date)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val schedule = snapshot.toObject(com.gramaangana.data.model.DailySchedule::class.java)
                    val updatedBookings = schedule?.bookings?.map { 
                        if (it.id == bookingId) it.copy(status = status) else it
                    } ?: emptyList()
                    transaction.update(docRef, "bookings", updatedBookings)
                }.await()
                _uiMessage.value = "Successfully updated to $status."
            } catch (e: Exception) {
                _uiMessage.value = "Update failed: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _uiMessage.value = null
    }
}
