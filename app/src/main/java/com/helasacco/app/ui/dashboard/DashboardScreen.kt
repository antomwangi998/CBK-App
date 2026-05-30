package com.helasacco.app.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helasacco.app.domain.model.*
import com.helasacco.app.ui.common.*
import com.helasacco.app.ui.navigation.Routes
import com.helasacco.app.ui.theme.HelaColors
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        ConfirmationDialog(
            title = "Sign Out",
            message = "Are you sure you want to sign out?",
            confirmLabel = "Sign Out",
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
                onLogout()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }

    Scaffold(
        topBar = {
            DashboardTopBar(
                name = uiState.session?.fullName?.split(" ")?.firstOrNull() ?: "",
                role = uiState.session?.role,
                onNotifications = { onNavigate(Routes.NOTIFICATIONS) },
                onProfile = { onNavigate(Routes.SETTINGS) },
                onLogout = { showLogoutDialog = true },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Routes.TRANSACTION_DEPOSIT) },
                containerColor = MaterialTheme.colorScheme.secondary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New transaction", tint = Color.White)
            }
        },
    ) { padding ->
        if (uiState.isLoading) {
            LoadingScreen()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                // Welcome banner
                item {
                    WelcomeBanner(
                        greeting = uiState.greeting,
                        name = uiState.session?.fullName?.split(" ")?.firstOrNull() ?: "",
                        date = uiState.todayDate,
                    )
                }

                // Stats grid
                item {
                    Spacer(Modifier.height(4.dp))
                    val isMember = uiState.session?.role == UserRole.MEMBER
                    if (isMember) {
                        MemberStatsGrid(stats = uiState.stats)
                    } else {
                        StaffStatsGrid(stats = uiState.stats, onNavigate = onNavigate)
                    }
                }

                // Quick actions
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(title = "QUICK ACTIONS")
                    QuickActionsGrid(
                        role = uiState.session?.role ?: UserRole.MEMBER,
                        onNavigate = onNavigate,
                    )
                }

                // Pending loans (staff only)
                if (uiState.pendingLoans.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionHeader(
                            title = "PENDING LOANS",
                            action = "View All",
                            onAction = { onNavigate(Routes.LOAN_LIST) },
                        )
                    }
                    items(uiState.pendingLoans) { loan ->
                        PendingLoanRow(loan = loan, onClick = { onNavigate(Routes.loanDetail(loan.id)) })
                    }
                }

                // Recent transactions
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(
                        title = "RECENT ACTIVITY",
                        action = "View All",
                        onAction = { onNavigate(Routes.TRANSACTION_LIST) },
                    )
                }

                if (uiState.recentTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No recent transactions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(uiState.recentTransactions) { txn ->
                        TransactionRow(
                            transaction = txn,
                            onClick = { onNavigate(Routes.transactionDetail(txn.id)) },
                        )
                    }
                }
            }
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    name: String,
    role: UserRole?,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    "HELA SMART SACCO",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                if (name.isNotBlank()) {
                    Text(
                        role?.value?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        actions = {
            IconButton(onClick = onNotifications) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = "Profile")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                        onClick = { showMenu = false; onProfile() },
                    )
                    DropdownMenuItem(
                        text = { Text("Sign Out") },
                        leadingIcon = { Icon(Icons.Outlined.Logout, null) },
                        onClick = { showMenu = false; onLogout() },
                    )
                }
            }
        },
    )
}

// ── Welcome Banner ────────────────────────────────────────────────────────────

@Composable
private fun WelcomeBanner(greeting: String, name: String, date: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (name.isNotBlank()) "$greeting, $name!" else "$greeting!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
            Icon(
                Icons.Filled.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ── Stats grids ───────────────────────────────────────────────────────────────

@Composable
private fun StaffStatsGrid(stats: DashboardStats, onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Total Members",
                value = "%,d".format(stats.membersCount),
                icon = Icons.Filled.People,
                modifier = Modifier.weight(1f),
                subtitle = "Active",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { onNavigate(Routes.MEMBER_LIST) },
            )
            StatCard(
                title = "Active Loans",
                value = "%,d".format(stats.activeLoansCount),
                icon = Icons.Filled.AccountBalance,
                modifier = Modifier.weight(1f),
                subtitle = "${stats.pendingLoansCount} pending",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { onNavigate(Routes.LOAN_LIST) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Total Savings",
                value = stats.totalSavingsMinor.minorToKes(),
                icon = Icons.Filled.Savings,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            StatCard(
                title = "Pending KYC",
                value = "%,d".format(stats.pendingKycCount),
                icon = Icons.Filled.VerifiedUser,
                modifier = Modifier.weight(1f),
                subtitle = "Needs review",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                onClick = { onNavigate(Routes.KYC_APPROVAL) },
            )
        }
    }
}

@Composable
private fun MemberStatsGrid(stats: DashboardStats) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "My Balance",
                value = stats.myBalanceMinor.minorToKes(),
                icon = Icons.Filled.AccountBalanceWallet,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            StatCard(
                title = "My Loans",
                value = "%,d".format(stats.myActiveLoans),
                icon = Icons.Filled.AccountBalance,
                modifier = Modifier.weight(1f),
                subtitle = "Active",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

// ── Quick Actions ─────────────────────────────────────────────────────────────

private data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val route: String,
    val containerColor: @Composable () -> Color,
    val iconColor: @Composable () -> Color,
    val allowedRoles: Set<UserRole>? = null,
)

@Composable
private fun QuickActionsGrid(role: UserRole, onNavigate: (String) -> Unit) {
    val allActions = listOf(
        QuickAction(Icons.Filled.ArrowDownward, "Deposit", Routes.TRANSACTION_DEPOSIT,
            { HelaColors.Success.copy(alpha = 0.15f) }, { HelaColors.Success }),
        QuickAction(Icons.Filled.ArrowUpward, "Withdraw", Routes.TRANSACTION_WITHDRAWAL,
            { MaterialTheme.colorScheme.errorContainer }, { MaterialTheme.colorScheme.error }),
        QuickAction(Icons.Filled.SwapHoriz, "Transfer", Routes.TRANSACTION_TRANSFER,
            { MaterialTheme.colorScheme.secondaryContainer }, { MaterialTheme.colorScheme.secondary }),
        QuickAction(Icons.Filled.People, "Members", Routes.MEMBER_LIST,
            { MaterialTheme.colorScheme.tertiaryContainer }, { MaterialTheme.colorScheme.tertiary },
            allowedRoles = setOf(UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.MANAGER,
                UserRole.BRANCH_MANAGER, UserRole.LOANS_OFFICER, UserRole.SENIOR_LOANS_OFFICER,
                UserRole.TELLER, UserRole.SENIOR_TELLER, UserRole.FIELD_OFFICER,
                UserRole.CREDIT_ANALYST, UserRole.AUDITOR)),
        QuickAction(Icons.Filled.AccountBalance, "Loans", Routes.LOAN_LIST,
            { MaterialTheme.colorScheme.primaryContainer }, { MaterialTheme.colorScheme.primary }),
        QuickAction(Icons.Filled.Payments, "Repay", Routes.LOAN_LIST,
            { HelaColors.Success.copy(alpha = 0.15f) }, { HelaColors.Success }),
        QuickAction(Icons.Filled.TrendingUp, "Invest", Routes.INVESTMENTS,
            { MaterialTheme.colorScheme.secondaryContainer }, { MaterialTheme.colorScheme.secondary }),
        QuickAction(Icons.Filled.BarChart, "Reports", Routes.REPORTS,
            { MaterialTheme.colorScheme.tertiaryContainer }, { MaterialTheme.colorScheme.tertiary },
            allowedRoles = setOf(UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.MANAGER,
                UserRole.BRANCH_MANAGER, UserRole.LOANS_OFFICER, UserRole.SENIOR_LOANS_OFFICER,
                UserRole.ACCOUNTANT, UserRole.AUDITOR)),
        //QuickAction(Icons.Filled.SmartToy, "AI Assist", Routes.AI_ASSISTANT,
            { MaterialTheme.colorScheme.primaryContainer }, { MaterialTheme.colorScheme.primary }),
    )

    val visible = allActions
        .filter { it.allowedRoles == null || role in it.allowedRoles }
        .take(8)

    val rows = visible.chunked(4)

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowItems.forEach { action ->
                    ActionButton(
                        action = action,
                        onClick = { onNavigate(action.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Fill empty spots
                repeat(4 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ActionButton(action: QuickAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = action.containerColor()),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                action.icon,
                contentDescription = null,
                tint = action.iconColor(),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                action.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Pending loan row ──────────────────────────────────────────────────────────

@Composable
private fun PendingLoanRow(loan: Loan, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(loan.loanNo, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = { Text("Applied ${loan.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                AmountText(amountMinor = loan.principalMinor, style = MaterialTheme.typography.bodyMedium)
                LoanStatusChip(status = loan.status)
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AccountBalance, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

// ── Transaction row ───────────────────────────────────────────────────────────

@Composable
private fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    val isCredit = transaction.transactionType in listOf(
        TransactionType.DEPOSIT, TransactionType.INTEREST,
        TransactionType.DIVIDEND, TransactionType.LOAN_DISBURSEMENT,
    )
    ListItem(
        headlineContent = {
            Text(
                transaction.transactionType.value.replace("_", " ").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            Text(
                transaction.description ?: transaction.reference ?: transaction.createdAt.take(10),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isCredit) "+" else "-"}${transaction.amountMinor.minorToKes()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCredit) HelaColors.Success else MaterialTheme.colorScheme.error,
                )
                Text(
                    transaction.createdAt.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = { TransactionIcon(type = transaction.transactionType) },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
