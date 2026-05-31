package com.helasacco.app.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helasacco.app.data.repository.MemberRepository
import com.helasacco.app.domain.model.*
import com.helasacco.app.ui.theme.HelaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

// ── State ─────────────────────────────────────────────────────────────────────

data class SelfRegState(
    // Step 0 - Phone entry
    val phone: String = "",
    val otp: String = "",
    val generatedOtp: String = "",
    val otpSent: Boolean = false,
    val otpVerified: Boolean = false,
    val otpCountdown: Int = 0,
    // Step 1 - Personal info
    val firstName: String = "",
    val lastName: String = "",
    val idNumber: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    // Step 2 - Contact
    val email: String = "",
    val mpesaNumber: String = "",
    val county: String = "",
    // Meta
    val currentStep: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val memberNo: String = "",
    val pin: String = "",
    val pinConfirm: String = "",
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class SelfRegViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val authRepository: com.helasacco.app.data.repository.AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SelfRegState())
    val state = _state.asStateFlow()

    fun update(block: SelfRegState.() -> SelfRegState) = _state.update(block)

    fun sendOtp() {
        val phone = _state.value.phone.trim()
        if (phone.length < 10) {
            _state.update { it.copy(error = "Enter a valid phone number") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val otp = Random.nextInt(100000, 999999).toString()
            // In production: send via Africa's Talking SMS API
            // For now we simulate — show OTP in snackbar for testing
            delay(1000)
            _state.update {
                it.copy(
                    isLoading = false,
                    generatedOtp = otp,
                    otpSent = true,
                    otpCountdown = 60,
                    error = "OTP sent to $phone (test: $otp)", // Remove in production
                )
            }
            startCountdown()
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            repeat(60) {
                delay(1000)
                _state.update { it.copy(otpCountdown = (it.otpCountdown - 1).coerceAtLeast(0)) }
            }
        }
    }

    fun verifyOtp() {
        val state = _state.value
        if (state.otp.trim() == state.generatedOtp) {
            _state.update { it.copy(otpVerified = true, currentStep = 1, error = null) }
        } else {
            _state.update { it.copy(error = "Incorrect OTP. Please try again.") }
        }
    }

    fun nextStep() {
        val s = _state.value
        when (s.currentStep) {
            3 -> {
                if (s.pin.length < 4) { _state.update { it.copy(error = "PIN must be at least 4 digits") }; return }
                if (s.pin != s.pinConfirm) { _state.update { it.copy(error = "PINs do not match") }; return }
            }
            1 -> {
                if (s.firstName.isBlank() || s.lastName.isBlank()) {
                    _state.update { it.copy(error = "First and last name are required") }
                    return
                }
                if (s.idNumber.isBlank()) {
                    _state.update { it.copy(error = "ID number is required") }
                    return
                }
            }
        }
        _state.update { it.copy(currentStep = it.currentStep + 1, error = null) }
    }

    fun prevStep() = _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0), error = null) }

    fun submit() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val memberNo = "MBR${LocalDate.now().year.toString().takeLast(2)}${(10000..99999).random()}"
            val member = Member(
                id = UUID.randomUUID().toString(),
                memberNo = memberNo,
                branchId = null,
                firstName = s.firstName.trim(),
                lastName = s.lastName.trim(),
                otherNames = null,
                idNumber = s.idNumber.trim(),
                dateOfBirth = s.dateOfBirth.trim().ifBlank { null },
                gender = s.gender.ifBlank { null },
                phone = s.phone.trim(),
                email = s.email.trim().ifBlank { null },
                address = null,
                city = null,
                county = s.county.trim().ifBlank { null },
                occupation = null,
                employer = null,
                mpesaNumber = s.mpesaNumber.trim().ifBlank { s.phone.trim() },
                kycStatus = KycStatus.PENDING,
                isActive = true,
                membershipDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE),
                profilePhotoPath = null,
                createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            )
            when (val result = memberRepository.save(member)) {
                is Result.Success -> {
                    // Create login account using phone as username and PIN as password
                    authRepository.createUser(
                        username = s.phone.trim(),
                        password = s.pin,
                        role = com.helasacco.app.domain.model.UserRole.MEMBER,
                        fullName = "${s.firstName.trim()} ${s.lastName.trim()}",
                        branchId = null,
                    )
                    _state.update { it.copy(isLoading = false, success = true, memberNo = memberNo) }
                }
                is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRegisterScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: SelfRegViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.success) {
        SuccessScreen(memberNo = state.memberNo, onDone = onSuccess)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join Hela Smart SACCO") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { (state.currentStep + 1) / 5f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
            )

            // Step labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("Verify", "Personal", "Contact", "PIN", "Review").forEachIndexed { i, label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (i == state.currentStep) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (i == state.currentStep) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }

            // Error
            state.error?.let {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (it.startsWith("OTP sent"))
                            HelaColors.Success.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        it, modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.startsWith("OTP sent")) HelaColors.Success
                                else MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Step content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                when (state.currentStep) {
                    0 -> PhoneVerifyStep(state, viewModel)
                    1 -> PersonalInfoStep(state, viewModel::update)
                    2 -> ContactStep(state, viewModel::update)
                    3 -> PinStep(state, viewModel::update)
                    4 -> ReviewStep(state)
                }
            }

            // Navigation buttons
            if (state.currentStep > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = viewModel::prevStep, modifier = Modifier.weight(1f)) {
                        Text("Back")
                    }
                    Button(
                        onClick = { if (state.currentStep == 4) viewModel.submit() else viewModel.nextStep() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(if (state.currentStep == 4) "Submit Application" else "Next")
                        }
                    }
                }
            }
        }
    }
}

// ── Step 0: Phone + OTP ───────────────────────────────────────────────────────

@Composable
private fun PhoneVerifyStep(state: SelfRegState, viewModel: SelfRegViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Verify your phone number",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "We'll send a one-time code to confirm your number.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.phone,
            onValueChange = { viewModel.update { copy(phone = it, error = null) } },
            label = { Text("Phone Number") },
            placeholder = { Text("07XXXXXXXX") },
            leadingIcon = { Icon(Icons.Outlined.Phone, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp),
            enabled = !state.otpSent,
        )

        if (!state.otpSent) {
            Button(
                onClick = viewModel::sendOtp,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !state.isLoading && state.phone.length >= 10,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("Send OTP")
            }
        } else {
            OutlinedTextField(
                value = state.otp,
                onValueChange = { viewModel.update { copy(otp = it, error = null) } },
                label = { Text("Enter OTP") },
                placeholder = { Text("6-digit code") },
                leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = viewModel::verifyOtp,
                    modifier = Modifier.weight(1f).height(50.dp),
                    enabled = state.otp.length == 6,
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Verify OTP") }

                OutlinedButton(
                    onClick = viewModel::sendOtp,
                    modifier = Modifier.weight(1f).height(50.dp),
                    enabled = state.otpCountdown == 0,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (state.otpCountdown > 0) "Resend (${state.otpCountdown}s)" else "Resend OTP")
                }
            }
        }
    }
}

// ── Step 1: Personal Info ─────────────────────────────────────────────────────

@Composable
private fun PersonalInfoStep(state: SelfRegState, update: (SelfRegState.() -> SelfRegState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Personal Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = state.firstName,
            onValueChange = { update { copy(firstName = it) } },
            label = { Text("First Name *") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = RoundedCornerShape(12.dp),
        )
        OutlinedTextField(
            value = state.lastName,
            onValueChange = { update { copy(lastName = it) } },
            label = { Text("Last Name *") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = RoundedCornerShape(12.dp),
        )
        OutlinedTextField(
            value = state.idNumber,
            onValueChange = { update { copy(idNumber = it) } },
            label = { Text("ID / Passport Number *") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
        )
        OutlinedTextField(
            value = state.dateOfBirth,
            onValueChange = { update { copy(dateOfBirth = it) } },
            label = { Text("Date of Birth (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
        Column {
            Text("Gender", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("male", "female", "other").forEach { g ->
                    FilterChip(
                        selected = state.gender == g,
                        onClick = { update { copy(gender = g) } },
                        label = { Text(g.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
        }
    }
}

// ── Step 2: Contact ───────────────────────────────────────────────────────────

@Composable
private fun ContactStep(state: SelfRegState, update: (SelfRegState.() -> SelfRegState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Contact Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = state.mpesaNumber,
            onValueChange = { update { copy(mpesaNumber = it) } },
            label = { Text("M-Pesa Number") },
            placeholder = { Text("07XXXXXXXX (defaults to phone if blank)") },
            leadingIcon = { Icon(Icons.Outlined.Phone, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp),
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = { update { copy(email = it) } },
            label = { Text("Email Address (optional)") },
            leadingIcon = { Icon(Icons.Outlined.Email, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
        )
        OutlinedTextField(
            value = state.county,
            onValueChange = { update { copy(county = it) } },
            label = { Text("County (optional)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = RoundedCornerShape(12.dp),
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Text(
                    "Your application will be reviewed by our team. You'll receive an SMS once approved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

// ── Step 3: Review ────────────────────────────────────────────────────────────

@Composable
private fun ReviewStep(state: SelfRegState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Review Your Application", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Personal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                com.helasacco.app.ui.common.InfoRow("Full Name", "${state.firstName} ${state.lastName}")
                com.helasacco.app.ui.common.InfoRow("ID Number", state.idNumber)
                com.helasacco.app.ui.common.InfoRow("Date of Birth", state.dateOfBirth)
                com.helasacco.app.ui.common.InfoRow("Gender", state.gender.replaceFirstChar { it.uppercase() })
                Spacer(Modifier.height(4.dp))
                Text("Contact", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                com.helasacco.app.ui.common.InfoRow("Phone", state.phone)
                com.helasacco.app.ui.common.InfoRow("M-Pesa", state.mpesaNumber.ifBlank { state.phone })
                com.helasacco.app.ui.common.InfoRow("Email", state.email)
                com.helasacco.app.ui.common.InfoRow("County", state.county)
            }
        }

        Text(
            "By submitting, you confirm the above information is accurate and consent to Hela Smart SACCO processing your data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}


@Composable
private fun PinStep(state: SelfRegState, update: (SelfRegState.() -> SelfRegState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Set Your Login PIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Use your phone number + this PIN to log in anytime.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.pin,
            onValueChange = { if (it.length <= 6) update { copy(pin = it) } },
            label = { Text("PIN (4-6 digits)") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(12.dp),
        )
        OutlinedTextField(
            value = state.pinConfirm,
            onValueChange = { if (it.length <= 6) update { copy(pinConfirm = it) } },
            label = { Text("Confirm PIN") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(12.dp),
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(10.dp),
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    "Remember this PIN — you will use your phone number and PIN to log in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// ── Success screen ────────────────────────────────────────────────────────────

@Composable
private fun SuccessScreen(memberNo: String, onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HelaColors.PrimaryDark, HelaColors.Primary))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(80.dp))
            Text("Application Submitted!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Your member number is", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f))
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))) {
                Text(memberNo, modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
            }
            Text(
                "Our team will review your application and contact you within 2 business days.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HelaColors.Primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Back to Login", fontWeight = FontWeight.Bold) }
        }
    }
}
