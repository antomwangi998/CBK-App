package com.helasacco.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helasacco.app.data.repository.MemberRepository
import com.helasacco.app.data.repository.NotificationRepository
import com.helasacco.app.di.SessionManager
import com.helasacco.app.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── KYC Approval ──────────────────────────────────────────────────────────────

data class KycUiState(
    val members: List<Member> = emptyList(),
    val filter: KycStatus = KycStatus.PENDING,
    val isLoading: Boolean = true,
    val successMessage: String? = null,
)

@HiltViewModel
class KycViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KycUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun setFilter(status: KycStatus) { _uiState.update { it.copy(filter = status) }; load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            memberRepository.getAllActive()
                .map { members -> members.filter { it.kycStatus == _uiState.value.filter } }
                .collect { filtered -> _uiState.update { it.copy(members = filtered, isLoading = false) } }
        }
    }

    fun updateKyc(memberId: String, status: KycStatus) {
        viewModelScope.launch {
            val session = sessionManager.session.first()
            memberRepository.updateKycStatus(memberId, status, session?.userId ?: "system")
            _uiState.update { it.copy(successMessage = "KYC status updated to ${status.value}") }
            load()
        }
    }

    fun dismissSuccess() { _uiState.update { it.copy(successMessage = null) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycApprovalScreen(
    onBack: () -> Unit,
    viewModel: KycViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    uiState.successMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2000)
            viewModel.dismissSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KYC Approval") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary),
            )
        },
        snackbarHost = {
            uiState.successMessage?.let { SnackbarHost(hostState = remember { SnackbarHostState() }.also { host -> LaunchedEffect(it) { host.showSnackbar(it) } }) }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = KycStatus.entries.indexOf(uiState.filter), edgePadding = 16.dp) {
                listOf(KycStatus.PENDING, KycStatus.SUBMITTED, KycStatus.UNDER_REVIEW, KycStatus.APPROVED, KycStatus.REJECTED).forEach { status ->
                    Tab(
                        selected = uiState.filter == status,
                        onClick = { viewModel.setFilter(status) },
                        text = { Text(status.value.replace("_", " ").replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            if (uiState.isLoading) {
                com.helasacco.app.ui.common.LoadingScreen()
            } else if (uiState.members.isEmpty()) {
                com.helasacco.app.ui.common.EmptyState("No members with ${uiState.filter.value} KYC status")
            } else {
                LazyColumn {
                    items(uiState.members, key = { it.id }) { member ->
                        KycMemberRow(member = member, onApprove = { viewModel.updateKyc(member.id, KycStatus.APPROVED) }, onReject = { viewModel.updateKyc(member.id, KycStatus.REJECTED) }, onReview = { viewModel.updateKyc(member.id, KycStatus.UNDER_REVIEW) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun KycMemberRow(member: Member, onApprove: () -> Unit, onReject: () -> Unit, onReview: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(member.fullName, fontWeight = FontWeight.Medium) },
        supportingContent = { Text("${member.memberNo} • ${member.phone ?: ""} • Since ${member.membershipDate}", style = MaterialTheme.typography.bodySmall) },
        leadingContent = { com.helasacco.app.ui.common.MemberAvatar(initials = member.initials) },
        trailingContent = {
            Box {
                IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.MoreVert, null) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Approve") }, leadingIcon = { Icon(Icons.Filled.CheckCircle, null, tint = androidx.compose.ui.graphics.Color(0xFF2E7D32)) }, onClick = { expanded = false; onApprove() })
                    DropdownMenuItem(text = { Text("Mark In Review") }, leadingIcon = { Icon(Icons.Filled.HourglassTop, null) }, onClick = { expanded = false; onReview() })
                    DropdownMenuItem(text = { Text("Reject") }, leadingIcon = { Icon(Icons.Filled.Cancel, null, tint = MaterialTheme.colorScheme.error) }, onClick = { expanded = false; onReject() })
                }
            }
        },
    )
}

// ── Notifications ─────────────────────────────────────────────────────────────

interface NotificationRepository {
    fun getForUser(userId: String, memberId: String?): kotlinx.coroutines.flow.Flow<List<Notification>>
    fun getUnreadCount(userId: String): kotlinx.coroutines.flow.Flow<Int>
    suspend fun markRead(id: String)
    suspend fun markAllRead(userId: String)
}

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionManager.session.first() ?: return@launch
            notificationRepository.getForUser(session.userId, session.memberId)
                .collect { notifs -> _uiState.update { it.copy(notifications = notifs, isLoading = false) } }
        }
    }

    fun markRead(id: String) { viewModelScope.launch { notificationRepository.markRead(id) } }
    fun markAllRead() {
        viewModelScope.launch {
            val session = sessionManager.session.first() ?: return@launch
            notificationRepository.markAllRead(session.userId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = { TextButton(onClick = viewModel::markAllRead) { Text("Mark All Read") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary),
            )
        },
    ) { padding ->
        if (uiState.isLoading) com.helasacco.app.ui.common.LoadingScreen()
        else if (uiState.notifications.isEmpty()) com.helasacco.app.ui.common.EmptyState("No notifications", Icons.Filled.Notifications)
        else LazyColumn(modifier = Modifier.padding(padding)) {
            items(uiState.notifications, key = { it.id }) { notif ->
                NotificationRow(notif = notif, onRead = { viewModel.markRead(notif.id) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun NotificationRow(notif: Notification, onRead: () -> Unit) {
    val bgColor = if (!notif.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
    ListItem(
        headlineContent = { Text(notif.title, fontWeight = if (!notif.isRead) FontWeight.Bold else FontWeight.Normal) },
        supportingContent = { Text(notif.message, style = MaterialTheme.typography.bodySmall, maxLines = 2) },
        trailingContent = { Text(notif.createdAt.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Icon(
                if (notif.isRead) Icons.Outlined.Notifications else Icons.Filled.Notifications,
                null,
                tint = if (!notif.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = bgColor),
        modifier = Modifier.clickable(onClick = onRead),
    )
}

// ── Settings ──────────────────────────────────────────────────────────────────

data class SettingsUiState(
    val userName: String = "",
    val userRole: String = "",
    val theme: String = "system",
    val biometricEnabled: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(sessionManager.session, sessionManager.theme, sessionManager.biometricEnabled) { session, theme, bio ->
                SettingsUiState(userName = session?.fullName ?: "", userRole = session?.role?.value ?: "", theme = theme, biometricEnabled = bio)
            }.collect { _uiState.value = it }
        }
    }

    fun setTheme(theme: String) { viewModelScope.launch { sessionManager.setTheme(theme) } }
    fun setBiometric(enabled: Boolean) { viewModelScope.launch { sessionManager.setBiometric(enabled) } }
    fun logout(onDone: () -> Unit) { viewModelScope.launch { sessionManager.clearSession(); onDone() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        com.helasacco.app.ui.common.ConfirmationDialog(
            title = "Sign Out",
            message = "Are you sure you want to sign out?",
            confirmLabel = "Sign Out",
            onConfirm = { viewModel.logout(onLogout) },
            onDismiss = { showLogoutDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary),
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Profile section
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        com.helasacco.app.ui.common.MemberAvatar(initials = uiState.userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""), size = 56.dp)
                        Column {
                            Text(uiState.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(uiState.userRole.replace("_", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                    }
                }
            }
            // Theme
            item {
                SettingsSection(title = "Appearance") {
                    Text("Theme", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system", "light", "dark").forEach { t ->
                            FilterChip(selected = uiState.theme == t, onClick = { viewModel.setTheme(t) }, label = { Text(t.replaceFirstChar { it.uppercase() }) })
                        }
                    }
                }
            }
            // Security
            item {
                SettingsSection(title = "Security") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Biometric Login", style = MaterialTheme.typography.bodyMedium)
                            Text("Use fingerprint or face ID", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = uiState.biometricEnabled, onCheckedChange = viewModel::setBiometric)
                    }
                }
            }
            // About
            item {
                SettingsSection(title = "About") {
                    com.helasacco.app.ui.common.InfoRow("App Version", "3.0.0")
                    com.helasacco.app.ui.common.InfoRow("Build", "Hela Smart SACCO")
                }
            }
            // Logout
            item {
                OutlinedButton(onClick = { showLogoutDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), border = ButtonDefaults.outlinedButtonBorder.copy()) {
                    Icon(Icons.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
