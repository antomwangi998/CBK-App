package com.helasacco.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helasacco.app.data.repository.*
import com.helasacco.app.di.SessionData
import com.helasacco.app.di.SessionManager
import com.helasacco.app.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DashboardStats(
    val membersCount: Int = 0,
    val activeLoansCount: Int = 0,
    val totalSavingsMinor: Long = 0L,
    val pendingKycCount: Int = 0,
    val totalOutstandingMinor: Long = 0L,
    val pendingLoansCount: Int = 0,
    // Member-specific
    val myBalanceMinor: Long = 0L,
    val myActiveLoans: Int = 0,
)

data class DashboardUiState(
    val session: SessionData? = null,
    val stats: DashboardStats = DashboardStats(),
    val recentTransactions: List<Transaction> = emptyList(),
    val pendingLoans: List<Loan> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val greeting: String = "Good Morning",
    val todayDate: String = "",
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val memberRepository: MemberRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val loanRepository: LoanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeSession()
        _uiState.update {
            it.copy(
                greeting = greeting(),
                todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")),
            )
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.session.collect { session ->
                _uiState.update { it.copy(session = session) }
                if (session != null) loadDashboard(session)
            }
        }
    }

    fun refresh() {
        val session = _uiState.value.session ?: return
        loadDashboard(session)
    }

    private fun loadDashboard(session: SessionData) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

                if (session.role == UserRole.MEMBER) {
                    loadMemberDashboard(session, today)
                } else {
                    loadStaffDashboard(today)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load dashboard") }
            }
        }
    }

    private suspend fun loadMemberDashboard(session: SessionData, today: String) {
        val memberId = session.memberId
        if (memberId == null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        // Collect account balances
        val accounts = accountRepository.getByMember(memberId).first()
        val savingsBalance = accounts
            .filter { it.accountType == AccountType.SAVINGS && it.status == "active" }
            .sumOf { it.balanceMinor }

        // Active loans
        val loans = loanRepository.getByMember(memberId).first()
        val activeLoans = loans.count { it.status in listOf(LoanStatus.ACTIVE, LoanStatus.DISBURSED) }

        // Recent transactions (use first savings account)
        val savingsAccount = accounts.firstOrNull { it.accountType == AccountType.SAVINGS }
        val recent = if (savingsAccount != null)
            transactionRepository.getByAccount(savingsAccount.id, 8, 0)
        else emptyList()

        _uiState.update {
            it.copy(
                isLoading = false,
                stats = it.stats.copy(
                    myBalanceMinor = savingsBalance,
                    myActiveLoans = activeLoans,
                ),
                recentTransactions = recent,
            )
        }
    }

    private suspend fun loadStaffDashboard(today: String) {
        // Collect all stats as flows
        combine(
            memberRepository.getActiveCount(),
            memberRepository.getPendingKycCount(),
            loanRepository.getPendingCount(),
            accountRepository.getTotalDeposits(),
            loanRepository.getTotalOutstanding(),
        ) { memberCount, pendingKyc, pendingLoans, totalDeposits, totalOutstanding ->
            DashboardStats(
                membersCount = memberCount,
                pendingKycCount = pendingKyc,
                pendingLoansCount = pendingLoans,
                totalSavingsMinor = totalDeposits ?: 0L,
                totalOutstandingMinor = totalOutstanding ?: 0L,
                activeLoansCount = 0, // loaded separately
            )
        }.first().let { stats ->
            val activeLoans = loanRepository.getActiveLoans().first().size
            val pending = loanRepository.getPendingLoans().first().take(5)

            // Monthly totals for recent activity context
            val monthStart = today.substring(0, 7) + "-01"
            val todayDeposits = transactionRepository.getTotalDepositsFrom(monthStart)
            val todayWithdrawals = transactionRepository.getTotalWithdrawalsFrom(monthStart)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    stats = stats.copy(activeLoansCount = activeLoans),
                    pendingLoans = pending,
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
        }
    }

    private fun greeting(): String {
        val hour = java.time.LocalTime.now().hour
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}
