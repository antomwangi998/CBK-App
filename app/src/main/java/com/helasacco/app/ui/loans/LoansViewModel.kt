package com.helasacco.app.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helasacco.app.data.local.entities.LoanEntity
import com.helasacco.app.data.repository.*
import com.helasacco.app.di.SessionManager
import com.helasacco.app.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlin.math.pow

// ── Loan List ──────────────────────────────────────────────────────────────────

data class LoanListUiState(
    val activeLoans: List<Loan> = emptyList(),
    val pendingLoans: List<Loan> = emptyList(),
    val selectedTab: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class LoanListViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                loanRepository.getActiveLoans(),
                loanRepository.getPendingLoans(),
            ) { active, pending -> active to pending }
                .collect { (active, pending) ->
                    _uiState.update { it.copy(activeLoans = active, pendingLoans = pending, isLoading = false) }
                }
        }
    }

    fun selectTab(tab: Int) { _uiState.update { it.copy(selectedTab = tab) } }
}

// ── Loan Detail ────────────────────────────────────────────────────────────────

data class LoanDetailUiState(
    val loan: Loan? = null,
    val member: Member? = null,
    val schedule: List<ScheduleItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class ScheduleItem(
    val installmentNo: Int,
    val dueDate: String,
    val principal: Long,
    val interest: Long,
    val total: Long,
    val status: String,
)

@HiltViewModel
class LoanDetailViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val memberRepository: MemberRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun load(loanId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val loan = loanRepository.getById(loanId)
            val member = loan?.let { memberRepository.getById(it.memberId) }
            val schedule = loan?.let { generateSchedule(it) } ?: emptyList()
            _uiState.update { it.copy(loan = loan, member = member, schedule = schedule, isLoading = false) }
        }
    }

    private fun generateSchedule(loan: Loan): List<ScheduleItem> {
        val monthlyRate = loan.interestRate / 100.0 / 12.0
        val n = loan.termMonths
        val principal = loan.principalMinor / 100.0
        val emi = if (monthlyRate == 0.0) principal / n
        else principal * monthlyRate * (1 + monthlyRate).pow(n) / ((1 + monthlyRate).pow(n) - 1)

        var balance = principal
        val startDate = loan.disbursementDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        return (1..n).map { i ->
            val interest = balance * monthlyRate
            val principalPart = emi - interest
            balance -= principalPart
            ScheduleItem(
                installmentNo = i,
                dueDate = startDate.plusMonths(i.toLong()).format(DateTimeFormatter.ISO_DATE),
                principal = (principalPart * 100).toLong(),
                interest = (interest * 100).toLong(),
                total = (emi * 100).toLong(),
                status = if (LocalDate.now().isAfter(startDate.plusMonths(i.toLong()))) "overdue" else "pending",
            )
        }
    }
}

// ── Loan Application ───────────────────────────────────────────────────────────

data class LoanApplicationState(
    val memberId: String = "",
    val memberName: String = "",
    val principalText: String = "",
    val termMonths: Int = 12,
    val interestRate: Double = 12.0,
    val purpose: String = "",
    val currentStep: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val savedLoanId: String? = null,
    // Calculator preview
    val monthlyPayment: Long = 0L,
    val totalInterest: Long = 0L,
    val totalRepayable: Long = 0L,
)

@HiltViewModel
class LoanApplicationViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val memberRepository: MemberRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LoanApplicationState())
    val state = _state.asStateFlow()

    fun setMember(id: String) {
        viewModelScope.launch {
            val member = memberRepository.getById(id)
            _state.update { it.copy(memberId = id, memberName = member?.fullName ?: id) }
        }
    }

    fun update(block: LoanApplicationState.() -> LoanApplicationState) {
        _state.update(block)
        recalculate()
    }

    private fun recalculate() {
        val s = _state.value
        val principal = s.principalText.toDoubleOrNull() ?: return
        val r = s.interestRate / 100.0 / 12.0
        val n = s.termMonths
        val emi = if (r == 0.0) principal / n
        else principal * r * (1 + r).pow(n) / ((1 + r).pow(n) - 1)
        val total = emi * n
        val interest = total - principal
        _state.update {
            it.copy(
                monthlyPayment = (emi * 100).toLong(),
                totalInterest = (interest * 100).toLong(),
                totalRepayable = (total * 100).toLong(),
            )
        }
    }

    fun nextStep() { _state.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(2), error = null) } }
    fun prevStep() { _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) } }

    fun submit() {
        val s = _state.value
        val principalMinor = s.principalText.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        if (principalMinor <= 0) { _state.update { it.copy(error = "Enter a valid loan amount") }; return }
        if (s.memberId.isBlank()) { _state.update { it.copy(error = "Member is required") }; return }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val session = sessionManager.session.first()
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val loanId = UUID.randomUUID().toString()
            val entity = LoanEntity(
                id = loanId,
                loanNo = "LN${System.currentTimeMillis()}",
                memberId = s.memberId,
                branchId = session?.branchId,
                principalMinor = principalMinor,
                outstandingMinor = principalMinor,
                interestRate = s.interestRate,
                termMonths = s.termMonths,
                status = LoanStatus.PENDING.value,
                purpose = s.purpose.ifBlank { null },
                createdBy = session?.userId,
                createdAt = now,
            )
            when (val r = loanRepository.save(entity)) {
                is Result.Success -> _state.update { it.copy(isLoading = false, success = true, savedLoanId = loanId) }
                is Result.Error -> _state.update { it.copy(isLoading = false, error = r.message) }
                else -> {}
            }
        }
    }
}
