package com.helasacco.app.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helasacco.app.data.repository.MemberRepository
import com.helasacco.app.domain.model.*
import com.helasacco.app.di.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

// ── Member List ────────────────────────────────────────────────────────────────

data class MemberListUiState(
    val members: List<Member> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class MemberListViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeMembers()
    }

    private fun observeMembers() {
        viewModelScope.launch {
            memberRepository.getAllActive()
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { members ->
                    _uiState.update { it.copy(members = members, isLoading = false) }
                }
        }
    }

    fun onSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.length >= 2) {
            viewModelScope.launch {
                val results = memberRepository.search(query)
                _uiState.update { it.copy(members = results) }
            }
        } else if (query.isEmpty()) {
            observeMembers()
        }
    }
}

// ── Member Detail ──────────────────────────────────────────────────────────────

data class MemberDetailUiState(
    val member: Member? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDeleteConfirm: Boolean = false,
)

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun loadMember(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val member = memberRepository.getById(id)
            _uiState.update { it.copy(member = member, isLoading = false) }
        }
    }

    fun toggleDeleteConfirm(show: Boolean) {
        _uiState.update { it.copy(showDeleteConfirm = show) }
    }
}

// ── Member Registration ────────────────────────────────────────────────────────

data class MemberFormState(
    // Personal
    val firstName: String = "",
    val lastName: String = "",
    val otherNames: String = "",
    val idNumber: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    // Contact
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val county: String = "",
    val mpesaNumber: String = "",
    // Employment
    val occupation: String = "",
    val employer: String = "",
    val employmentType: String = "",
    val monthlyIncome: String = "",
    // Meta
    val currentStep: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val savedMemberId: String? = null,
)

@HiltViewModel
class MemberRegistrationViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(MemberFormState())
    val state = _state.asStateFlow()

    fun update(block: MemberFormState.() -> MemberFormState) = _state.update(block)

    fun nextStep() { _state.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(3), error = null) } }
    fun prevStep() { _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0), error = null) } }

    fun submit() {
        val s = _state.value
        if (s.firstName.isBlank() || s.lastName.isBlank()) {
            _state.update { it.copy(error = "First and last name are required") }
            return
        }
        if (s.phone.isBlank()) {
            _state.update { it.copy(error = "Phone number is required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val session = sessionManager.session.first()
            val memberId = UUID.randomUUID().toString()
            val memberNo = generateMemberNo()
            val member = Member(
                id = memberId,
                memberNo = memberNo,
                branchId = session?.branchId,
                firstName = s.firstName.trim(),
                lastName = s.lastName.trim(),
                otherNames = s.otherNames.trim().ifBlank { null },
                idNumber = s.idNumber.trim().ifBlank { null },
                dateOfBirth = s.dateOfBirth.trim().ifBlank { null },
                gender = s.gender.ifBlank { null },
                phone = s.phone.trim(),
                email = s.email.trim().ifBlank { null },
                address = s.address.trim().ifBlank { null },
                city = s.city.trim().ifBlank { null },
                county = s.county.trim().ifBlank { null },
                occupation = s.occupation.trim().ifBlank { null },
                employer = s.employer.trim().ifBlank { null },
                mpesaNumber = s.mpesaNumber.trim().ifBlank { null },
                kycStatus = KycStatus.PENDING,
                isActive = true,
                membershipDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE),
                profilePhotoPath = null,
                createdAt = java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            )
            when (val r = memberRepository.save(member, createdBy = session?.userId)) {
                is Result.Success -> _state.update { it.copy(isLoading = false, success = true, savedMemberId = memberId) }
                is Result.Error -> _state.update { it.copy(isLoading = false, error = r.message) }
                else -> {}
            }
        }
    }

    private fun generateMemberNo(): String {
        val year = LocalDate.now().year.toString().takeLast(2)
        val rand = (10000..99999).random()
        return "MBR$year$rand"
    }
}
