package com.helasacco.app.ui.members

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helasacco.app.domain.model.KycStatus
import com.helasacco.app.domain.model.Member
import com.helasacco.app.ui.common.*

// ── Member List ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberListScreen(
    onMemberClick: (String) -> Unit,
    onNewMember: () -> Unit,
    onBack: () -> Unit,
    viewModel: MemberListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Members") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = onNewMember) { Icon(Icons.Filled.PersonAdd, contentDescription = "Add member") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearch,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Search by name, phone, ID…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) IconButton(onClick = { viewModel.onSearch("") }) { Icon(Icons.Filled.Clear, null) }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            if (uiState.isLoading) {
                LoadingScreen()
            } else if (uiState.members.isEmpty()) {
                EmptyState("No members found", Icons.Filled.People, "Register Member", onNewMember)
            } else {
                LazyColumn {
                    items(uiState.members, key = { it.id }) { member ->
                        MemberListItem(member = member, onClick = { onMemberClick(member.id) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberListItem(member: Member, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(member.fullName, fontWeight = FontWeight.Medium) },
        supportingContent = { Text("${member.memberNo} • ${member.phone ?: "No phone"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { KycStatusChip(member.kycStatus) },
        leadingContent = { MemberAvatar(initials = member.initials) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

// ── Member Detail ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    memberId: String,
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MemberDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(memberId) { viewModel.loadMember(memberId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.member?.fullName ?: "Member Profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = {
                    uiState.member?.let { IconButton(onClick = { onEdit(it.id) }) { Icon(Icons.Filled.Edit, null) } }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary),
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> LoadingScreen()
            uiState.member == null -> ErrorScreen("Member not found")
            else -> {
                val member = uiState.member!!
                LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
                    item {
                        // Profile header
                        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                MemberAvatar(initials = member.initials, size = 72.dp)
                                Spacer(Modifier.height(12.dp))
                                Text(member.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(member.memberNo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                Spacer(Modifier.height(8.dp))
                                KycStatusChip(member.kycStatus)
                            }
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Personal Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                InfoRow("ID Number", member.idNumber)
                                InfoRow("Date of Birth", member.dateOfBirth)
                                InfoRow("Gender", member.gender?.replaceFirstChar { it.uppercase() })
                                InfoRow("Member Since", member.membershipDate)
                            }
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Contact Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                InfoRow("Phone", member.phone)
                                InfoRow("M-Pesa", member.mpesaNumber)
                                InfoRow("Email", member.email)
                                InfoRow("Address", member.address)
                                InfoRow("City", member.city)
                                InfoRow("County", member.county)
                            }
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Employment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                InfoRow("Occupation", member.occupation)
                                InfoRow("Employer", member.employer)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Member Registration (multi-step) ──────────────────────────────────────────

private val STEPS = listOf("Personal", "Contact", "Employment", "Review")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberRegistrationScreen(
    onSuccess: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MemberRegistrationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.success) {
        if (state.success && state.savedMemberId != null) onSuccess(state.savedMemberId!!)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register Member") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Step indicator
            LinearProgressIndicator(
                progress = { (state.currentStep + 1) / STEPS.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                STEPS.forEachIndexed { i, label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (i == state.currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (i == state.currentStep) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
            // Error
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
            }
            // Step content
            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                when (state.currentStep) {
                    0 -> PersonalStep(state, viewModel::update)
                    1 -> ContactStep(state, viewModel::update)
                    2 -> EmploymentStep(state, viewModel::update)
                    3 -> ReviewStep(state)
                }
            }
            // Navigation buttons
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.currentStep > 0) {
                    OutlinedButton(onClick = viewModel::prevStep, modifier = Modifier.weight(1f)) { Text("Back") }
                }
                Button(
                    onClick = { if (state.currentStep == STEPS.size - 1) viewModel.submit() else viewModel.nextStep() },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading,
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(if (state.currentStep == STEPS.size - 1) "Submit" else "Next")
                }
            }
        }
    }
}

@Composable
private fun PersonalStep(state: MemberFormState, update: (MemberFormState.() -> MemberFormState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Personal Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FormField("First Name *", state.firstName, { v -> update { copy(firstName = v) } }, KeyboardCapitalization.Words)
        FormField("Last Name *", state.lastName, { v -> update { copy(lastName = v) } }, KeyboardCapitalization.Words)
        FormField("Other Names", state.otherNames, { v -> update { copy(otherNames = v) } }, KeyboardCapitalization.Words)
        FormField("ID Number", state.idNumber, { v -> update { copy(idNumber = v) } })
        FormField("Date of Birth (YYYY-MM-DD)", state.dateOfBirth, { v -> update { copy(dateOfBirth = v) } })
        GenderSelector(selected = state.gender, onSelect = { v -> update { copy(gender = v) } })
    }
}

@Composable
private fun ContactStep(state: MemberFormState, update: (MemberFormState.() -> MemberFormState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Contact Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FormField("Phone Number *", state.phone, { v -> update { copy(phone = v) } }, keyboardType = KeyboardType.Phone)
        FormField("M-Pesa Number", state.mpesaNumber, { v -> update { copy(mpesaNumber = v) } }, keyboardType = KeyboardType.Phone)
        FormField("Email Address", state.email, { v -> update { copy(email = v) } }, keyboardType = KeyboardType.Email)
        FormField("Physical Address", state.address, { v -> update { copy(address = v) } }, KeyboardCapitalization.Words)
        FormField("City", state.city, { v -> update { copy(city = v) } }, KeyboardCapitalization.Words)
        FormField("County", state.county, { v -> update { copy(county = v) } }, KeyboardCapitalization.Words)
    }
}

@Composable
private fun EmploymentStep(state: MemberFormState, update: (MemberFormState.() -> MemberFormState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Employment Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FormField("Occupation", state.occupation, { v -> update { copy(occupation = v) } }, KeyboardCapitalization.Words)
        FormField("Employer", state.employer, { v -> update { copy(employer = v) } }, KeyboardCapitalization.Words)
        FormField("Monthly Income (KES)", state.monthlyIncome, { v -> update { copy(monthlyIncome = v) } }, keyboardType = KeyboardType.Number)
    }
}

@Composable
private fun ReviewStep(state: MemberFormState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Review & Confirm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow("Full Name", "${state.firstName} ${state.lastName} ${state.otherNames}".trim())
                InfoRow("ID Number", state.idNumber)
                InfoRow("Phone", state.phone)
                InfoRow("M-Pesa", state.mpesaNumber)
                InfoRow("Email", state.email)
                InfoRow("Address", "${state.address}, ${state.city}".trim(',', ' '))
                InfoRow("County", state.county)
                InfoRow("Occupation", state.occupation)
                InfoRow("Employer", state.employer)
            }
        }
    }
}

// ── Shared form helpers ───────────────────────────────────────────────────────

@Composable
private fun FormField(
    label: String, value: String, onValueChange: (String) -> Unit,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
    )
}

@Composable
private fun GenderSelector(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text("Gender", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("male", "female", "other").forEach { g ->
                FilterChip(
                    selected = selected == g,
                    onClick = { onSelect(g) },
                    label = { Text(g.replaceFirstChar { it.uppercase() }) },
                )
            }
        }
    }
}
