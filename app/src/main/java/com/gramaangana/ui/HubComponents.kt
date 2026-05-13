package com.gramaangana.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gramaangana.data.model.Booking
import com.gramaangana.data.model.MaintenanceItem
import com.gramaangana.ui.theme.LocalVillageColors
import java.util.*

@Composable
fun HubDashboard(viewModel: MainViewModel) {
    val isLoggedIn by viewModel.isLoggedIn
    val isAuthenticating by viewModel.isAuthenticating
    val isAdminMode by viewModel.isAdminMode
    val showBookingSheet by viewModel.showBookingSheet
    val showPledgeSheet by viewModel.showPledgeSheet
    val showPaymentDemo by viewModel.showPaymentDemo
    val uiMessage by viewModel.uiMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { if (isLoggedIn && !showPaymentDemo) HubTopBar(viewModel) },
        floatingActionButton = { if (isLoggedIn && !showPaymentDemo) HubFAB(viewModel) },
        bottomBar = { if (isLoggedIn && !showPaymentDemo) HubNavigationBar(viewModel) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (!isLoggedIn) {
                AuthScreen(viewModel)
            } else if (showPaymentDemo) {
                PaymentDemoScreen(viewModel)
            } else {
                val selectedTab by viewModel.selectedTab
                if (isAdminMode && selectedTab == 0) {
                    AdminManagementScreen(viewModel)
                } else {
                    when (selectedTab) {
                        0 -> DashboardContent(viewModel)
                        1 -> CalendarScreen(viewModel)
                        2 -> PledgesScreen(viewModel)
                    }
                }
            }

            if (isAuthenticating) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Processing...", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showBookingSheet) {
        BookingFlowSheet(onDismiss = { viewModel.showBookingSheet.value = false }, onSubmit = { p, s, e -> viewModel.requestBooking(p, s, e); viewModel.showBookingSheet.value = false })
    }

    if (showPledgeSheet) {
        PledgeFormSheet(onDismiss = { viewModel.showPledgeSheet.value = false }, onSubmit = { n, t, d -> viewModel.addMaintenanceItem(n, t, d); viewModel.showPledgeSheet.value = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubTopBar(viewModel: MainViewModel) {
    val isAdminMode by viewModel.isAdminMode; val isPanchayatUser by viewModel.isPanchayatUser; val profile by viewModel.currentUserProfile
    CenterAlignedTopAppBar(
        title = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(text = if (isAdminMode) "PANCHAYAT PORTAL" else "GRAMA-ANGANA", fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = if (isAdminMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary); Text(text = "Welcome, ${profile?.name ?: "User"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        actions = {
            if (isPanchayatUser) { IconButton(onClick = { viewModel.isAdminMode.value = !isAdminMode }) { Icon(if (isAdminMode) Icons.Default.Settings else Icons.Default.Lock, null, tint = if (isAdminMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) } }
            IconButton(onClick = { viewModel.logout() }) { Icon(Icons.Default.ExitToApp, null) }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun HubFAB(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab; val isAdminMode by viewModel.isAdminMode
    AnimatedVisibility(visible = selectedTab != 1) {
        ExtendedFloatingActionButton(
            onClick = { if (selectedTab == 0 || (selectedTab == 2 && !isAdminMode)) viewModel.showBookingSheet.value = true else if (isAdminMode) viewModel.showPledgeSheet.value = true },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text(if (selectedTab == 2 && isAdminMode) "NEW PLEDGE" else "NEW BOOKING", fontWeight = FontWeight.Bold) },
            containerColor = if (selectedTab == 2 && isAdminMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(bottom = 8.dp).shadow(12.dp, RoundedCornerShape(16.dp))
        )
    }
}

@Composable
fun HubNavigationBar(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        val tabs = listOf("Home" to Icons.Default.Home, "Calendar" to Icons.Default.DateRange, "Pledges" to Icons.Default.Favorite)
        tabs.forEachIndexed { i, (l, icon) -> NavigationBarItem(icon = { Icon(icon, null) }, label = { Text(l, fontWeight = FontWeight.Bold) }, selected = selectedTab == i, onClick = { viewModel.selectedTab.value = i }) }
    }
}

@Composable
fun AuthScreen(viewModel: MainViewModel) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }; var isReg by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), modifier = Modifier.size(120.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp)) } }
        Spacer(modifier = Modifier.height(24.dp)); Text("Grama-Angana", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary); Text(if (isReg) "Join our village community" else "Welcome back to the hub", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(48.dp))
        if (isReg) { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), leadingIcon = { Icon(Icons.Default.Face, null) }); Spacer(modifier = Modifier.height(16.dp)) }
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Village Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), leadingIcon = { Icon(Icons.Default.Email, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)); Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Secure Password") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)); Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { if (isReg) viewModel.register(email, password, name) else viewModel.login(email, password) }, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp)) { Text(if (isReg) "CREATE ACCOUNT" else "ACCESS HUB", fontWeight = FontWeight.Black) }
        Spacer(modifier = Modifier.height(16.dp)); TextButton(onClick = { isReg = !isReg }) { Text(if (isReg) "ALREADY A MEMBER? LOG IN" else "NEW TO THE VILLAGE? REGISTER HERE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun DashboardContent(viewModel: MainViewModel) {
    val items by viewModel.maintenanceItems.collectAsState(); val bookings by viewModel.bookingsForDate.collectAsState()
    val status = if (bookings.isEmpty()) "Free" else if (bookings.size < 3) "Partially Booked" else "Fully Booked"
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Column(modifier = Modifier.padding(24.dp)) { Text("Village Hub", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black); Text("Real-time updates from your Panchayat.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        EventBoardCard(date = "Today's Schedule", eventCount = bookings.size, status = status)
        Spacer(modifier = Modifier.height(32.dp)); Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("Community Pledges", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); TextButton(onClick = { viewModel.selectedTab.value = 2 }) { Text("VIEW ALL", fontWeight = FontWeight.Bold) } }
        if (items.isEmpty()) MaintenanceJarEmptyState() else items.filter { it.status == "NEEDED" }.take(2).forEach { item -> MaintenanceJarCard(item = item, onContribute = { a -> viewModel.initiateContribution(item.itemName, a) }, isAdmin = viewModel.isAdminMode.value, onClose = { viewModel.closePledge(item.itemName) }) }
    }
}

@Composable
fun MaintenanceJarEmptyState() {
    Card(modifier = Modifier.padding(24.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(64.dp)); Spacer(modifier = Modifier.height(16.dp)); Text("No active needs.", fontWeight = FontWeight.Bold); Text("Your community hall is perfectly maintained!", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun CalendarScreen(viewModel: MainViewModel) {
    val selectedDate by viewModel.selectedDate.collectAsState(); val bookings by viewModel.bookingsForDate.collectAsState(); val availability by viewModel.monthAvailability.collectAsState()
    val month by viewModel.currentMonth; val year by viewModel.currentYear; val calendarInstance = remember(month, year) { Calendar.getInstance().apply { set(Calendar.YEAR, year); set(Calendar.MONTH, month - 1); set(Calendar.DAY_OF_MONTH, 1) } }
    val monthName = calendarInstance.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())?.uppercase() ?: ""; val firstDay = calendarInstance.get(Calendar.DAY_OF_WEEK); val daysInMonth = calendarInstance.getActualMaximum(Calendar.DAY_OF_MONTH)
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Text("Hall Availability", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(24.dp))
        Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = CardDefaults.outlinedCardBorder(), modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("$monthName $year", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Row { IconButton(onClick = { viewModel.changeMonth(-1) }) { Icon(Icons.Default.KeyboardArrowLeft, null) }; IconButton(onClick = { viewModel.changeMonth(1) }) { Icon(Icons.Default.KeyboardArrowRight, null) } } }
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) { listOf("S", "M", "T", "W", "T", "F", "S").forEach { Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.width(40.dp), textAlign = TextAlign.Center) } }
                val totalSlots = firstDay - 1 + daysInMonth; val rows = (totalSlots + 6) / 7
                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (col in 1..7) {
                            val day = row * 7 + col - (firstDay - 1)
                            if (day in 1..daysInMonth) {
                                val dStr = "$year-${"%02d".format(month)}-${"%02d".format(day)}"; val isSel = selectedDate == dStr
                                val stat = availability[dStr] ?: "FREE"; val sCol = when (stat) { "FULL" -> LocalVillageColors.current.full; "PARTIAL" -> LocalVillageColors.current.partial; else -> LocalVillageColors.current.free }
                                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { viewModel.setDate(dStr) }, contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(day.toString(), style = MaterialTheme.typography.bodyLarge, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold); Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isSel) Color.White else sCol)) } }
                            } else Spacer(modifier = Modifier.size(42.dp))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp)); Row(modifier = Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) { LegendItem("FREE", LocalVillageColors.current.free); LegendItem("BUSY", LocalVillageColors.current.partial); LegendItem("FULL", LocalVillageColors.current.full) }
        Spacer(modifier = Modifier.height(32.dp)); Text("Schedule for ${selectedDate.substringAfterLast("-")} $monthName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 24.dp))
        if (bookings.isEmpty()) Card(modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { viewModel.showBookingSheet.value = true }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)), shape = RoundedCornerShape(16.dp)) { Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.width(16.dp)); Text("Hall is free. Tap here to book!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) } } else bookings.forEach { BookingSummaryCard(it, viewModel) }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color)); Spacer(modifier = Modifier.width(8.dp)); Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) }
}

@Composable
fun BookingSummaryCard(booking: Booking, viewModel: MainViewModel) {
    val profile by viewModel.currentUserProfile; val isAdminMode by viewModel.isAdminMode
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp), border = CardDefaults.outlinedCardBorder()) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(booking.purpose, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black); Text("Requested by: ${booking.userId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.width(6.dp)); Text("${booking.startTime/60}:${"%02d".format(booking.startTime%60)} - ${booking.endTime/60}:${"%02d".format(booking.endTime%60)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                if ((booking.userId == profile?.name || isAdminMode) && booking.status != "CANCELLED" && booking.status != "REJECTED") { Spacer(modifier = Modifier.height(12.dp)); TextButton(onClick = { viewModel.cancelBooking(booking.id, booking.date) }, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("CANCEL BOOKING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black) } }
            }
            val sCol = when(booking.status) { "APPROVED" -> LocalVillageColors.current.free; "REJECTED", "CANCELLED" -> MaterialTheme.colorScheme.error; else -> LocalVillageColors.current.partial }
            Surface(shape = RoundedCornerShape(10.dp), color = sCol.copy(alpha = 0.1f)) { Text(booking.status, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = sCol) }
        }
    }
}

@Composable
fun PledgesScreen(viewModel: MainViewModel) {
    val items by viewModel.maintenanceItems.collectAsState(); val isAdmin by viewModel.isAdminMode
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
        Column(modifier = Modifier.padding(24.dp)) { Text("Community Pledges", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary); Text("Contribute to local village development projects.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items.forEach { MaintenanceJarCard(it, { a -> viewModel.initiateContribution(it.itemName, a) }, isAdmin, { viewModel.closePledge(it.itemName) }) }; Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun MaintenanceJarCard(item: MaintenanceItem, onContribute: (Double) -> Unit, isAdmin: Boolean, onClose: () -> Unit) {
    val progress = (item.collectedAmount / item.targetAmount).toFloat().coerceIn(0f, 1f); var customAmount by remember { mutableStateOf("") }; val isClosed = item.status == "CLOSED"
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = if (isClosed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface), border = CardDefaults.outlinedCardBorder()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = if (isClosed) Color.Gray else MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(if (isClosed) Icons.Default.CheckCircle else Icons.Default.Build, null, tint = if (isClosed) Color.White else MaterialTheme.colorScheme.onSecondaryContainer) } }
                Spacer(modifier = Modifier.width(16.dp)); Column { Text(item.itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black); Text("Target: ₹${"%,.0f".format(item.targetAmount)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (isAdmin && !isClosed) { Spacer(modifier = Modifier.weight(1f)); IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) } }
            }
            Spacer(modifier = Modifier.height(24.dp)); Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) { Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(brush = Brush.horizontalGradient(colors = listOf(LocalVillageColors.current.progressPledged, LocalVillageColors.current.progressActual)))) }
            Spacer(modifier = Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("Collected: ₹${"%,.0f".format(item.collectedAmount)}", fontWeight = FontWeight.Bold, color = if (isClosed) Color.Gray else MaterialTheme.colorScheme.primary); Text(if (isClosed) "COMPLETED" else "${(progress * 100).toInt()}% FUNDED", fontWeight = FontWeight.Black, color = if (isClosed) Color.Gray else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall) }
            if (!isClosed) { Spacer(modifier = Modifier.height(24.dp)); Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = customAmount, onValueChange = { if (it.all { c -> c.isDigit() }) customAmount = it }, label = { Text("Amount ₹") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), maxLines = 1); Button(onClick = { onContribute(customAmount.toDoubleOrNull() ?: 100.0) }, modifier = Modifier.height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("PLEDGE") } } }
        }
    }
}

@Composable
fun AdminManagementScreen(viewModel: MainViewModel) {
    val requests by viewModel.pendingRequests.collectAsState()
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
        Text("Panchayat Oversight", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
        Text("PENDING BOOKING REQUESTS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp)); if (requests.isEmpty()) Text("No pending requests. Hall is idle.", modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) else requests.forEach { BookingRequestCard(it.userId, it.date, it.purpose, { viewModel.updateBookingStatus(it.id, it.date, "APPROVED") }, { viewModel.updateBookingStatus(it.id, it.date, "REJECTED") }) }
    }
}

@Composable
fun BookingRequestCard(name: String, date: String, purpose: String, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = CardDefaults.outlinedCardBorder()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Request by $name", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text("HALL BOOKING: $date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.height(12.dp)); Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) { Text(purpose, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge) }
            Spacer(modifier = Modifier.height(24.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Button(onClick = onApprove, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = LocalVillageColors.current.free), shape = RoundedCornerShape(12.dp)) { Text("APPROVE", fontWeight = FontWeight.Black) }; OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp)) { Text("REJECT", fontWeight = FontWeight.Black) } }
        }
    }
}

@Composable
fun EventBoardCard(date: String, eventCount: Int, status: String) {
    val villageColors = LocalVillageColors.current; val statusColor = when (status) { "Free" -> villageColors.free; "Partially Booked" -> villageColors.partial; else -> villageColors.full }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)), shape = RoundedCornerShape(32.dp)) {
        Column(modifier = Modifier.padding(28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("HALL STATUS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary); Surface(shape = RoundedCornerShape(12.dp), color = statusColor, modifier = Modifier.size(width = 140.dp, height = 36.dp)) { Box(contentAlignment = Alignment.Center) { Text(status.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Black) } } }
            Spacer(modifier = Modifier.height(16.dp)); Text(date, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer); Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(12.dp)); Text(if (eventCount == 0) "Free all day" else "$eventCount Events Scheduled", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) @Composable
fun BookingFlowSheet(onDismiss: () -> Unit, onSubmit: (String, Int, Int) -> Unit) {
    var p by remember { mutableStateOf("") }; var s by remember { mutableStateOf("10") }; var e by remember { mutableStateOf("14") }
    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { BottomSheetDefaults.DragHandle() }, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 64.dp).fillMaxWidth()) {
            Text("Hall Booking Request", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.height(24.dp)); OutlinedTextField(value = p, onValueChange = { p = it }, label = { Text("Purpose (e.g. Marriage)", fontWeight = FontWeight.Bold) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
            Spacer(modifier = Modifier.height(24.dp)); Text("Timing (24hr clock)", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) { OutlinedTextField(value = s, onValueChange = { if(it.length<=2) s=it }, label = { Text("Start Hour") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); OutlinedTextField(value = e, onValueChange = { if(it.length<=2) e=it }, label = { Text("End Hour") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
            Spacer(modifier = Modifier.height(48.dp)); Button(onClick = { onSubmit(p, (s.toIntOrNull() ?: 10) * 60, (e.toIntOrNull() ?: 14) * 60) }, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(18.dp)) { Text("SUBMIT REQUEST TO PANCHAYAT", fontWeight = FontWeight.Black) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) @Composable
fun PledgeFormSheet(onDismiss: () -> Unit, onSubmit: (String, Double, String) -> Unit) {
    var n by remember { mutableStateOf("") }; var t by remember { mutableStateOf("") }; var d by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { BottomSheetDefaults.DragHandle() }, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 64.dp).fillMaxWidth()) {
            Text("Add Village Need", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary); Spacer(modifier = Modifier.height(32.dp)); OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
            Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = t, onValueChange = { if(it.all{c->c.isDigit()}) t=it }, label = { Text("Target Amount (₹)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = d, onValueChange = { d = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
            Spacer(modifier = Modifier.height(48.dp)); Button(onClick = { onSubmit(n, t.toDoubleOrNull() ?: 0.0, d) }, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("ADD PLEDGE JAR", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
fun PaymentDemoScreen(viewModel: MainViewModel) {
    val item by viewModel.pendingContributionItem; val amt by viewModel.pendingContributionAmount
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = CircleShape, color = Color(0xFF1E1E1E), modifier = Modifier.size(100.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp)) } }
        Spacer(modifier = Modifier.height(24.dp)); Text("SECURE PAYMENT GATEWAY", color = Color.White.copy(alpha = 0.5f), letterSpacing = 2.sp, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(48.dp)); Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) { Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Villager Contribution", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge); Spacer(modifier = Modifier.height(8.dp)); Text("For: $item", color = Color.Gray, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(32.dp)); Text("₹$amt", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = Color.White) } }
        Spacer(modifier = Modifier.height(48.dp)); Button(onClick = { viewModel.confirmContribution() }, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(16.dp)) { Text("CONFIRM \u0026 PAY NOW", fontWeight = FontWeight.Black, fontSize = 18.sp) }
        Spacer(modifier = Modifier.height(24.dp)); TextButton(onClick = { viewModel.showPaymentDemo.value = false }) { Text("CANCEL TRANSACTION", color = Color.White.copy(alpha = 0.7f)) }
        Spacer(modifier = Modifier.height(32.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("256-BIT ENCRYPTION SECURED", style = MaterialTheme.typography.labelSmall, color = Color.Gray) }
    }
}
