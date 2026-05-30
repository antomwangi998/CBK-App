package com.helasacco.app.ui.loans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helasacco.app.domain.model.Loan
import com.helasacco.app.domain.model.LoanStatus
import com.helasacco.app.ui.common.*

// ── Loan List ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanListScreen(
    onLoanClick: (String) -> Unit,
    onNewLoan: () -> Unit,
    onBack: () -> Unit,
    viewModel: LoanListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loans") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = onNewLoan) { Icon(Icons.Filled.Add, "New Loan") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = uiState.selectedTab) {
                listOf("Active (${uiState.activeLoans.size})", "Pending (${uiState.pendingLoans.size})").forEachIndexed { i, title ->
                    Tab(selected = uiState.selectedTab == i, onClick = { viewModel.selectTab(i) }, text = { Text(title) })
                }
            }
            val loans = if (uiState.selectedTab == 0) uiState.activeLoans else uiState.pendingLoans
            if (uiState.isLoading) LoadingScreen()
            else if (loans.isEmpty()) EmptyState("No loans found", Icons.Filled.AccountBalance, "Apply for Loan", onNewLoan)
            else LazyColumn {
                items(loans, key = { it.id }) { loan ->
                    LoanListItem(loan = loan, onClick = { onLoanClick(loan.id) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun LoanListItem(loan: Loan, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(loan.loanNo, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(
                "Outstanding: ${loan.outstandingMinor.minorToKes()} • ${loan.termMonths}mo @ ${loan.interestRate}%",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = { LoanStatusChip(loan.status) },
        leadingContent = {
            Icon(Icons.Filled.AccountBalance, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

// ── Loan Detail ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: String,
    onRepay: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: LoanDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSchedule by remember { mutableStateOf(false) }

    LaunchedEffect(loanId) { viewModel.load(loanId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary),
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> LoadingScreen()
            uiState.loan == null -> ErrorScreen("Loan not found")
            else -> {
                val loan = uiState.loan!!
                LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(loan.loanNo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                LoanStatusChip(loan.status)
                                Spacer(Modifier.height(12.dp))
                                Text("Outstanding", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                AmountText(amountMinor = loan.outstandingMinor, style = MaterialTheme.typography.displaySmall)
                            }
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Loan Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                InfoRow("Principal", loan.principalMinor.minorToKes())
                                InfoRow("Interest Rate", "${loan.interestRate}% p.a.")
                                InfoRow("Term", "${loan.termMonths} months")
                                InfoRow("Disbursed", loan.disbursementDate ?: "—")
                                InfoRow("Maturity", loan.maturityDate ?: "—")
                                InfoRow("Next Payment", loan.nextPaymentDate ?: "—")
                                InfoRow("Monthly Payment", loan.nextPaymentMinor.minorToKes())
                                uiState.member?.let { m -> InfoRow("Member", m.fullName) }
                            }
                        }
                    }
                    // Repayment button
                    if (loan.status in listOf(LoanStatus.ACTIVE, LoanStatus.DISBURSED)) {
                        item {
                            Button(onClick = { onRepay(loan.id) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Filled.Payments, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Make Repayment")
                            }
                        }
                    }
                    // Schedule toggle
                    item {
                        TextButton(onClick = { showSchedule = !showSchedule }, modifier = Modifier.fillMaxWidth()) {
                            Icon(if (showSchedule) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (showSchedule) "Hide Schedule" else "View Repayment Schedule")
                        }
                    }
                    if (showSchedule) {
                        item {
                            Card(shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        listOf("#", "Due Date", "Principal", "Interest", "Total").forEach { h ->
                                            Text(h, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                        }
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    uiState.schedule.forEach { item ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${item.installmentNo}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                            Text(item.dueDate.takeLast(5), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                            Text(item.principal.minorToKes().replace("KES", "").trim(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                            Text(item.interest.minorToKes().replace("KES", "").trim(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                            Text(item.total.minorToKes().replace("KES", "").trim(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Loan Application (multi-step) ─────────────────────────────────────────────

private val LOAN_STEPS = listOf("Product", "Details", "Review")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanApplicationScreen(
    memberId: String?,
    onSuccess: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: LoanApplicationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(memberId) { memberId?.let { viewModel.setMember(it) } }
    LaunchedEffect(state.success) { if (state.success && state.savedLoanId != null) onSuccess(state.savedLoanId!!) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Application") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(progress = { (state.currentStep + 1) / LOAN_STEPS.size.toFloat() }, modifier = Modifier.fillMaxWidth().height(4.dp))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                when (state.currentStep) {
                    0 -> LoanProductStep(state, viewModel::update)
                    1 -> LoanDetailsStep(state, viewModel::update)
                    2 -> LoanReviewStep(state)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.currentStep > 0) OutlinedButton(onClick = viewModel::prevStep, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = { if (state.currentStep == LOAN_STEPS.size - 1) viewModel.submit() else viewModel.nextStep() },
                    modifier = Modifier.weight(1f), enabled = !state.isLoading, shape = RoundedCornerShape(12.dp),
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(if (state.currentStep == LOAN_STEPS.size - 1) "Submit Application" else "Next")
                }
            }
        }
    }
}

@Composable
private fun LoanProductStep(state: LoanApplicationState, update: (LoanApplicationState.() -> LoanApplicationState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Loan Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = state.principalText,
            onValueChange = { v -> update { copy(principalText = v) } },
            label = { Text("Loan Amount (KES)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
        )
        Text("Term: ${state.termMonths} months", style = MaterialTheme.typography.bodyMedium)
        Slider(value = state.termMonths.toFloat(), onValueChange = { v -> update { copy(termMonths = v.toInt()) } }, valueRange = 1f..60f, steps = 58)
        Text("Interest Rate: ${state.interestRate}% p.a.", style = MaterialTheme.typography.bodyMedium)
        Slider(value = state.interestRate.toFloat(), onValueChange = { v -> update { copy(interestRate = v.toDouble()) } }, valueRange = 1f..36f, steps = 34)
        // Preview card
        if (state.monthlyPayment > 0) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Repayment Preview", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    InfoRow("Monthly Payment", state.monthlyPayment.minorToKes())
                    InfoRow("Total Interest", state.totalInterest.minorToKes())
                    InfoRow("Total Repayable", state.totalRepayable.minorToKes())
                }
            }
        }
    }
}

@Composable
private fun LoanDetailsStep(state: LoanApplicationState, update: (LoanApplicationState.() -> LoanApplicationState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Loan Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (state.memberName.isNotBlank()) {
            Text("Member: ${state.memberName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        OutlinedTextField(
            value = state.purpose,
            onValueChange = { v -> update { copy(purpose = v) } },
            label = { Text("Loan Purpose") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3, maxLines = 5,
            shape = RoundedCornerShape(10.dp),
        )
    }
}

@Composable
private fun LoanReviewStep(state: LoanApplicationState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Review Application", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow("Member", state.memberName)
                InfoRow("Loan Amount", "KES ${state.principalText}")
                InfoRow("Interest Rate", "${state.interestRate}% p.a.")
                InfoRow("Term", "${state.termMonths} months")
                InfoRow("Monthly Payment", state.monthlyPayment.minorToKes())
                InfoRow("Total Repayable", state.totalRepayable.minorToKes())
                InfoRow("Purpose", state.purpose)
            }
        }
        Text("By submitting, you confirm this application is accurate.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
