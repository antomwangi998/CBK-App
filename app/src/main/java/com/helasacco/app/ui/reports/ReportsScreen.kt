package com.helasacco.app.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.helasacco.app.data.repository.*
import com.helasacco.app.ui.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ReportsUiState(
    val totalMembers: Int = 0,
    val activeLoans: Int = 0,
    val totalSavingsMinor: Long = 0,
    val totalOutstandingMinor: Long = 0,
    val pendingKyc: Int = 0,
    val monthlyDepositsMinor: Long = 0,
    val monthlyWithdrawalsMinor: Long = 0,
    val isLoading: Boolean = true,
    val selectedTab: Int = 0,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val loanRepository: LoanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val monthStart = LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ISO_DATE)
            combine(
                memberRepository.getActiveCount(),
                memberRepository.getPendingKycCount(),
                accountRepository.getTotalDeposits(),
                loanRepository.getTotalOutstanding(),
                loanRepository.getPendingCount(),
            ) { members, kyc, deposits, outstanding, pending ->
                ReportsUiState(
                    totalMembers = members,
                    pendingKyc = kyc,
                    totalSavingsMinor = deposits ?: 0,
                    totalOutstandingMinor = outstanding ?: 0,
                    activeLoans = pending,
                    isLoading = false,
                )
            }.collect { state ->
                val deposits = transactionRepository.getTotalDepositsFrom(monthStart)
                val withdrawals = transactionRepository.getTotalWithdrawalsFrom(monthStart)
                _uiState.value = state.copy(monthlyDepositsMinor = deposits, monthlyWithdrawalsMinor = withdrawals)
            }
        }
    }

    fun selectTab(i: Int) { _uiState.update { it.copy(selectedTab = i) } }
    fun refresh() { _uiState.update { it.copy(isLoading = true) }; load() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf("Overview", "Members", "Loans", "Savings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = viewModel::refresh) { Icon(Icons.Filled.Refresh, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = uiState.selectedTab) {
                tabs.forEachIndexed { i, t -> Tab(selected = uiState.selectedTab == i, onClick = { viewModel.selectTab(i) }, text = { Text(t) }) }
            }
            if (uiState.isLoading) LoadingScreen()
            else when (uiState.selectedTab) {
                0 -> OverviewTab(uiState)
                1 -> MembersTab(uiState)
                2 -> LoansTab(uiState)
                3 -> SavingsTab(uiState)
            }
        }
    }
}

@Composable
private fun OverviewTab(s: ReportsUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Summary — ${LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total Members", "%,d".format(s.totalMembers), Icons.Filled.People, modifier = Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                StatCard("Active Loans", "%,d".format(s.activeLoans), Icons.Filled.AccountBalance, modifier = Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total Savings", s.totalSavingsMinor.minorToKes(), Icons.Filled.Savings, modifier = Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                StatCard("Loan Book", s.totalOutstandingMinor.minorToKes(), Icons.Filled.TrendingUp, modifier = Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        item { SectionHeader("This Month") }
        item {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow("Deposits", s.monthlyDepositsMinor.minorToKes())
                    InfoRow("Withdrawals", s.monthlyWithdrawalsMinor.minorToKes())
                    InfoRow("Net", (s.monthlyDepositsMinor - s.monthlyWithdrawalsMinor).minorToKes())
                }
            }
        }
    }
}

@Composable
private fun MembersTab(s: ReportsUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Member Statistics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Total Active Members", "%,d".format(s.totalMembers))
                    InfoRow("Pending KYC", "%,d".format(s.pendingKyc))
                }
            }
        }
    }
}

@Composable
private fun LoansTab(s: ReportsUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Loan Portfolio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Active Loans", "%,d".format(s.activeLoans))
                    InfoRow("Total Outstanding", s.totalOutstandingMinor.minorToKes())
                }
            }
        }
    }
}

@Composable
private fun SavingsTab(s: ReportsUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Savings Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Total Deposits", s.totalSavingsMinor.minorToKes())
                    InfoRow("Monthly Deposits", s.monthlyDepositsMinor.minorToKes())
                    InfoRow("Monthly Withdrawals", s.monthlyWithdrawalsMinor.minorToKes())
                }
            }
        }
    }
}
